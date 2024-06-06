package galactus.dashboard.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for publishing Kafka messages to WebSocket destinations.
 * <p>
 * This service listens to specific Kafka topics and forwards the received messages to corresponding WebSocket destinations.
 */
@Service
@Slf4j
public class KafkaAggregatesPublisherService {
    @Autowired
    SimpMessagingTemplate template;

    /**
     * Listens to the "recentchange.event_count" Kafka topic and forwards the message to the WebSocket destination "/topic/recentchange-event-count".
     *
     * @param message the message received from the Kafka topic
     */

    @KafkaListener(topics = "recentchange.event_count")
    public void publishRecentchangeEventCount(@Payload String message) {
        template.convertAndSend("/topic/recentchange-event-count", message);
    }

    /**
     * Listens to the "recentchange.length_change" Kafka topic and forwards the message to the WebSocket destination "/topic/recentchange-length-change".
     *
     * @param message the message received from the Kafka topic
     */
    @KafkaListener(topics = "recentchange.length_change")
    public void publishRecentchangeLengthChange(@Payload String message) {
        template.convertAndSend("/topic/recentchange-length-change", message);
    }

    /**
     * Listens to the "recentchange.active_users" Kafka topic and forwards the message to the WebSocket destination "/topic/recentchange-active-users".
     *
     * @param message the message received from the Kafka topic
     */
    @KafkaListener(topics = "recentchange.active_users")
    public void publishRecentchangeActiveUsers(@Payload String message) {
        template.convertAndSend("/topic/recentchange-active-users", message);
    }
}
