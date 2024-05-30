package galactus.dashboard.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaAggregatesConsumerService {
    @Autowired
    SimpMessagingTemplate template;

    @KafkaListener(topics = "recentchange.event_count")
    public void consumeRecentchangeEventCount(@Payload String message) {
        template.convertAndSend("/topic/recentchange-event-count", message);
    }

    @KafkaListener(topics = "recentchange.active_users")
    public void consumeRecentchangeActiveUsers(@Payload String message) {
        template.convertAndSend("/topic/recentchange-active-users", message);
    }

    @KafkaListener(topics = "recentchange.length_change")
    public void consumeRecentchangeLengthChange(@Payload String message) {
        template.convertAndSend("/topic/recentchange-length-change", message);
    }
}
