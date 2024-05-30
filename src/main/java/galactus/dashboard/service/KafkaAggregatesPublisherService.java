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
    public void consumeRecentchangeEventCount(@Payload String message) {
        template.convertAndSend("/topic/recentchange-event-count", message);
    }

    @KafkaListener(topics = "recentchange.length_change")
    public void consumeRecentchangeLengthChange(@Payload String message) {
        template.convertAndSend("/topic/recentchange-length-change", message);
    }

    @KafkaListener(topics = "recentchange.active_users")
    public void consumeRecentchangeActiveUsers(@Payload String message) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(message);
        List<JsonNode> users = new ArrayList<>();
        List<JsonNode> bots = new ArrayList<>();

        // Split users and bots
        for (JsonNode node : jsonNode) {
            if (node.get("bot").asBoolean()) {
                bots.add(node);
            } else {
                users.add(node);
            }
        }

        users.sort(Comparator.comparingInt(a -> a.get("changesLength").asInt()));

        bots.sort(Comparator.comparingInt(a -> a.get("changesLength").asInt()));

        List<JsonNode> topUsersMostChanges = users.subList(Math.max(users.size() - 10, 0), users.size());
        List<JsonNode> topUsersLeastChanges = users.subList(0, Math.min(10, users.size()));

        List<JsonNode> topBotsMostChanges = bots.subList(Math.max(bots.size() - 10, 0), bots.size());
        List<JsonNode> topBotsLeastChanges = bots.subList(0, Math.min(10, bots.size()));

        String usersJson = objectMapper.writeValueAsString(topUsersMostChanges) + "," + objectMapper.writeValueAsString(topUsersLeastChanges);
        String botsJson = objectMapper.writeValueAsString(topBotsMostChanges) + "," + objectMapper.writeValueAsString(topBotsLeastChanges);

        // Output results:
        log.info("Serialized Top 10 users with most and least changes: " + usersJson);
        log.info("Serialized Top 10 bots with most and least changes: " + botsJson);


        template.convertAndSend("/topic/recentchange-active-users", message);
    }
}
