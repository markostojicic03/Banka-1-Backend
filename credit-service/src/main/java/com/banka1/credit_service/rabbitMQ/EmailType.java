package com.banka1.credit_service.rabbitMQ;

/**
 * Enum defining types of email notifications sent by the employee-service via RabbitMQ.
 * Each type carries the corresponding RabbitMQ routing key.
 */
public enum EmailType {

    CREDIT_APPROVED("credit.approved"),

    CREDIT_DENIED("credit.denied"),

    CREDIT_INSTALLMENT_FAILED("credit.installment.failed");


    private final String routingKey;

    EmailType(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getRoutingKey() {
        return routingKey;
    }
}
