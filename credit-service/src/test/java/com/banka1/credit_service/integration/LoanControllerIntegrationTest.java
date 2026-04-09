package com.banka1.credit_service.integration;

import com.banka1.credit_service.domain.Installment;
import com.banka1.credit_service.domain.Loan;
import com.banka1.credit_service.domain.LoanRequest;
import com.banka1.credit_service.domain.enums.CurrencyCode;
import com.banka1.credit_service.domain.enums.EmploymentStatus;
import com.banka1.credit_service.domain.enums.InterestType;
import com.banka1.credit_service.domain.enums.LoanType;
import com.banka1.credit_service.domain.enums.PaymentStatus;
import com.banka1.credit_service.domain.enums.Status;
import com.banka1.credit_service.dto.request.LoanRequestDto;
import com.banka1.credit_service.dto.response.AccountDetailsResponseDto;
import com.banka1.credit_service.rabbitMQ.RabbitClient;
import com.banka1.credit_service.repository.InstallmentRepository;
import com.banka1.credit_service.repository.LoanRepository;
import com.banka1.credit_service.repository.LoanRequestRepository;
import com.banka1.credit_service.rest_client.AccountService;
import com.banka1.credit_service.rest_client.ExchangeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoanControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private LoanRequestRepository loanRequestRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private InstallmentRepository installmentRepository;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private ExchangeService exchangeService;

    @MockitoBean
    private RabbitClient rabbitClient;

    @BeforeEach
    void setUp() {
        installmentRepository.deleteAll();
        loanRepository.deleteAll();
        loanRequestRepository.deleteAll();
        doNothing().when(rabbitClient).sendEmailNotification(any());
    }

    @Test
    void requestEndpointPersistsLoanRequestForAuthenticatedOwner() throws Exception {
        LoanRequestDto request = validRequest();
        when(accountService.getDetails("ACC-001"))
                .thenReturn(new AccountDetailsResponseDto(77L, CurrencyCode.RSD, "pera@test.com", "pera"));

        mockMvc.perform(post("/api/loans/requests")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.claim("id", 77L).claim("roles", "CLIENT_BASIC"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT_BASIC")))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        LoanRequest persisted = loanRequestRepository.findAll().getFirst();
        assertThat(persisted.getClientId()).isEqualTo(77L);
        assertThat(persisted.getStatus()).isEqualTo(Status.PENDING);
        assertThat(persisted.getAccountNumber()).isEqualTo("ACC-001");
    }

    @Test
    void requestEndpointReturnsBadRequestForBusinessRuleViolation() throws Exception {
        LoanRequestDto request = validRequest();
        request.setRepaymentPeriod(11);

        mockMvc.perform(post("/api/loans/requests")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.claim("id", 77L).claim("roles", "CLIENT_BASIC"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT_BASIC")))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR_VALIDATION"))
                .andExpect(jsonPath("$.errorTitle").value("Neispravni argumenti"));
    }

    @Test
    void requestEndpointReturnsValidationErrorsForMalformedPayload() throws Exception {
        LoanRequestDto request = validRequest();
        request.setPurpose("");
        request.setAmount(BigDecimal.ZERO);

        mockMvc.perform(post("/api/loans/requests")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.claim("id", 77L).claim("roles", "CLIENT_BASIC"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT_BASIC")))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR_VALIDATION"))
                .andExpect(jsonPath("$.validationErrors.amount").value("amount mora biti >0"))
                .andExpect(jsonPath("$.validationErrors.purpose").value("purpose ne sme biti prazan"));
    }

    @Test
    void approveEndpointCreatesLoanAndFirstInstallment() throws Exception {
        LoanRequest loanRequest = loanRequestRepository.save(pendingRequest());
        when(accountService.transactionFromBank(any())).thenReturn(null);

        mockMvc.perform(put("/api/loans/requests/{id}/approve", loanRequest.getId())
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.claim("id", 501L).claim("roles", "BASIC"))
                                .authorities(new SimpleGrantedAuthority("ROLE_BASIC")))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("ODOBREN ZAHTEV"));

        LoanRequest updatedRequest = loanRequestRepository.findById(loanRequest.getId()).orElseThrow();
        assertThat(updatedRequest.getStatus()).isEqualTo(Status.APPROVED);

        Loan createdLoan = loanRepository.findAll().getFirst();
        assertThat(createdLoan.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(createdLoan.getClientId()).isEqualTo(loanRequest.getClientId());
        assertThat(createdLoan.getAccountNumber()).isEqualTo(loanRequest.getAccountNumber());

        Installment createdInstallment = installmentRepository.findAll().getFirst();
        assertThat(createdInstallment.getLoan().getId()).isEqualTo(createdLoan.getId());
        assertThat(createdInstallment.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    void infoEndpointReturnsLoanDetailsToOwner() throws Exception {
        Loan loan = loanRepository.save(activeLoan());
        installmentRepository.save(new Installment(
                loan,
                new BigDecimal("22100.00"),
                new BigDecimal("0.0060"),
                CurrencyCode.RSD,
                LocalDate.now().plusMonths(1),
                null,
                PaymentStatus.UNPAID
        ));

        mockMvc.perform(get("/api/loans/{loanNumber}", loan.getId())
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.claim("id", 77L).claim("roles", "CLIENT_BASIC"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT_BASIC"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loan.loanNumber").value(loan.getId()))
                .andExpect(jsonPath("$.loan.status").value("ACTIVE"))
                .andExpect(jsonPath("$.installments.length()").value(1))
                .andExpect(jsonPath("$.installments[0].paymentStatus").value("UNPAID"));
    }

    private LoanRequestDto validRequest() {
        return new LoanRequestDto(
                LoanType.AUTO,
                InterestType.FIXED,
                new BigDecimal("500000.00"),
                CurrencyCode.RSD,
                "Kupovina automobila",
                new BigDecimal("150000.00"),
                EmploymentStatus.PERMANENT,
                48,
                24,
                "+38160111222",
                "ACC-001"
        );
    }

    private LoanRequest pendingRequest() {
        return new LoanRequest(
                LoanType.AUTO,
                InterestType.FIXED,
                new BigDecimal("500000.00"),
                CurrencyCode.RSD,
                "Kupovina automobila",
                new BigDecimal("150000.00"),
                EmploymentStatus.PERMANENT,
                48,
                24,
                "+38160111222",
                "ACC-001",
                77L,
                Status.PENDING,
                "pera@test.com",
                "pera"
        );
    }

    private Loan activeLoan() {
        Loan loan = new Loan();
        loan.setLoanType(LoanType.AUTO);
        loan.setAccountNumber("ACC-001");
        loan.setAmount(new BigDecimal("500000.00"));
        loan.setRepaymentPeriod(24);
        loan.setNominalInterestRate(new BigDecimal("0.0060"));
        loan.setEffectiveInterestRate(new BigDecimal("0.0060"));
        loan.setInterestType(InterestType.FIXED);
        loan.setAgreementDate(LocalDate.now().minusMonths(1));
        loan.setMaturityDate(LocalDate.now().plusMonths(23));
        loan.setInstallmentAmount(new BigDecimal("22100.00"));
        loan.setNextInstallmentDate(LocalDate.now().plusMonths(1));
        loan.setRemainingDebt(new BigDecimal("480000.00"));
        loan.setCurrency(CurrencyCode.RSD);
        loan.setStatus(Status.ACTIVE);
        loan.setUserEmail("pera@test.com");
        loan.setUsername("pera");
        loan.setClientId(77L);
        loan.setInstallmentCount(1);
        return loan;
    }
}
