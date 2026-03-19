package app.entities;

/**
 * Konstante za routing key-eve koje se koriste u RabbitMQ porukama.
 * Ove vrednosti se koriste za mapiranje na NotificationType.
 */
public final class RoutingKeys {
    /**
     * Routing key za kreiranje zaposlenog.
     */
    public static final String EMPLOYEE_CREATED = "employee.created";
    /**
     * Routing key za reset lozinke zaposlenog.
     */
    public static final String EMPLOYEE_PASSWORD_RESET = "employee.password_reset";
    /**
     * Routing key za deaktivaciju naloga zaposlenog.
     */
    public static final String EMPLOYEE_ACCOUNT_DEACTIVATED = "employee.account_deactivated";

    private RoutingKeys() {}
}
