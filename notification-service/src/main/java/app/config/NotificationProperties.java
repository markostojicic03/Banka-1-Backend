package app.config;

import app.dto.EmailTemplate;
import app.entities.NotificationType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Configuration properties for notification templates and routing keys.
 *
 * @ ConfigurationProperties anotacija sluzi da uzme vrednosti iz `application.properties`
 * i mapira ih u ovaj java objekat
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {

    private Map<String, EmailTemplate> templates;
    private Map<String, NotificationType> routingKeys;
}
