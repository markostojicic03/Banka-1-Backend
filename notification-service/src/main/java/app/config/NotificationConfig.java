package app.config;

import app.dto.EmailTemplate;
import app.entities.NotificationType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuration for notification templates and routing keys loaded from properties.
 */
@Configuration
public class NotificationConfig {

    @Bean
    public NotificationProperties notificationProperties() {
        return new NotificationProperties();
    }

    @Bean
    public Map<String, EmailTemplate> emailTemplates(NotificationProperties properties) {
        return properties.getTemplates();
    }

    @Bean
    public Map<String, NotificationType> routingKeys(NotificationProperties properties) {
        return properties.getRoutingKeys();
    }
}
