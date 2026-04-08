package com.banka1.stock_service.service;

import com.banka1.stock_service.config.ListingRefreshProperties;
import com.banka1.stock_service.domain.Listing;
import com.banka1.stock_service.domain.ListingType;
import com.banka1.stock_service.dto.ListingRefreshBatchResponse;
import com.banka1.stock_service.dto.StockExchangeStatusResponse;
import com.banka1.stock_service.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheduled batch refresher for persisted listing snapshots.
 *
 * <p>The scheduler runs on a fixed configurable delay and refreshes only listings whose
 * exchange is currently open according to the existing stock-exchange status logic.
 *
 * <p>Unsupported listing types are skipped instead of failing the entire batch.
 * Individual provider failures are logged and counted, but the batch continues with
 * the remaining listings.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ListingMarketDataScheduler {

    private static final String SOURCE = "scheduled-listing-refresh";

    private final ListingRepository listingRepository;
    private final StockExchangeService stockExchangeService;
    private final ListingMarketDataRefreshService listingMarketDataRefreshService;
    private final ListingRefreshProperties listingRefreshProperties;

    /**
     * Runs one scheduled refresh pass using the configured fixed delay.
     */
    @Scheduled(fixedDelayString = "${stock.listing-refresh.interval-ms:900000}")
    public void runScheduledRefresh() {
        if (!listingRefreshProperties.enabled()) {
            return;
        }

        ListingRefreshBatchResponse response = refreshOpenListings();
        log.info(
                "Scheduled listing refresh completed. processedListings={}, refreshedCount={}, skippedClosedCount={}, skippedUnsupportedCount={}, failedCount={}",
                response.processedListings(),
                response.refreshedCount(),
                response.skippedClosedCount(),
                response.skippedUnsupportedCount(),
                response.failedCount()
        );
    }

    /**
     * Refreshes all persisted listings whose exchange is currently open.
     *
     * <p>The exchange-open check is evaluated once per exchange and reused for every listing quoted
     * on that exchange during the current batch pass.
     *
     * @return batch refresh summary used by logs and tests
     */
    public ListingRefreshBatchResponse refreshOpenListings() {
        List<Listing> listings = listingRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        Map<Long, Boolean> exchangeOpenById = new HashMap<>();

        int refreshedCount = 0;
        int skippedClosedCount = 0;
        int skippedUnsupportedCount = 0;
        int failedCount = 0;

        for (Listing listing : listings) {
            if (!exchangeOpenById.computeIfAbsent(
                    listing.getStockExchange().getId(),
                    this::isExchangeOpen
            )) {
                skippedClosedCount++;
                continue;
            }

            if (!supportsRefresh(listing.getListingType())) {
                skippedUnsupportedCount++;
                continue;
            }

            try {
                listingMarketDataRefreshService.refreshListing(listing.getId());
                refreshedCount++;
            } catch (ResponseStatusException exception) {
                failedCount++;
                log.warn(
                        "Failed to refresh listing id={} ticker={} because {}",
                        listing.getId(),
                        listing.getTicker(),
                        exception.getReason()
                );
            }
        }

        return new ListingRefreshBatchResponse(
                SOURCE,
                listings.size(),
                refreshedCount,
                skippedClosedCount,
                skippedUnsupportedCount,
                failedCount
        );
    }

    /**
     * Determines whether the exchange of one listing should be treated as open.
     *
     * @param stockExchangeId exchange identifier
     * @return {@code true} when listings on that exchange should be refreshed
     */
    private boolean isExchangeOpen(Long stockExchangeId) {
        StockExchangeStatusResponse status = stockExchangeService.getStockExchangeStatus(stockExchangeId);
        return status.open();
    }

    /**
     * Returns whether the current scheduler implementation supports refreshing one listing type.
     *
     * @param listingType listing category
     * @return {@code true} for provider-backed listing categories
     */
    private boolean supportsRefresh(ListingType listingType) {
        return listingType == ListingType.STOCK || listingType == ListingType.FOREX;
    }
}
