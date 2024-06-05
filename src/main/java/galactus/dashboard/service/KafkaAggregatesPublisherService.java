package galactus.dashboard.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class KafkaAggregatesPublisherService {
    @Autowired
    SimpMessagingTemplate template;

    @KafkaListener(topics = "recentchange.event_count")
    public void publishRecentchangeEventCount(@Payload String message) {
        template.convertAndSend("/topic/recentchange-event-count", message);
    }

    @KafkaListener(topics = "recentchange.length_change")
    public void publishRecentchangeLengthChange(@Payload String message) {
        template.convertAndSend("/topic/recentchange-length-change", message);
    }

    @KafkaListener(topics = "recentchange.active_users")
    public void publishRecentchangeActiveUsers(@Payload String message) {
        template.convertAndSend("/topic/recentchange-active-users", message);
    }
}
