package galactus.dashboard.component.processor.recentchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import galactus.dashboard.component.processor.BaseEventProcessor;
import galactus.dashboard.entity.RecentchangeActiveUsersEntity;
import galactus.dashboard.repository.RecentchangeActiveUsersRepository;
import lombok.Data;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Suppressed;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RecentchangeActiveUsersProcessor extends BaseEventProcessor {

    @Autowired
    private RecentchangeActiveUsersRepository recentchangeActiveUsersRepository;
    private ConcurrentHashMap<String, WikiUser> wikiUsers = new ConcurrentHashMap<>();

    public RecentchangeActiveUsersProcessor(RecentchangeActiveUsersRepository recentchangeActiveUsersRepository) {
        this.recentchangeActiveUsersRepository = recentchangeActiveUsersRepository;
    }

    @Data
    private static class WikiUser {
        String username;
        AtomicInteger changesLength = new AtomicInteger();
        boolean isBot;

        @JsonCreator
        public WikiUser(@JsonProperty("username") String username, @JsonProperty("changesLength") int changesLength, @JsonProperty("bot") boolean isBot) {
            this.username = username;
            this.changesLength.set(changesLength);
            this.isBot = isBot;
        }

        void updateChangesLength(int diff) {
            changesLength.addAndGet(diff);
        }
    }

    private static String cropUsersCollection(String serializedWikiUsers) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<WikiUser> users = new ArrayList<>();
        List<WikiUser> bots = new ArrayList<>();

        List<WikiUser> list = objectMapper.readValue(serializedWikiUsers, new TypeReference<>() {
        });

        for (WikiUser wikiUser : list) {
            if (wikiUser.isBot()) {
                bots.add(wikiUser);
            } else {
                users.add(wikiUser);
            }
        }

        users.sort(Comparator.comparingInt(a -> a.getChangesLength().get()));

        bots.sort(Comparator.comparingInt(a -> a.getChangesLength().get()));

        List<WikiUser> topUsersMostChanges = users.subList(Math.max(users.size() - 10, 0), users.size());
        List<WikiUser> topUsersLeastChanges = users.subList(0, Math.min(10, users.size()));

        List<WikiUser> topBotsMostChanges = bots.subList(Math.max(bots.size() - 10, 0), bots.size());
        List<WikiUser> topBotsLeastChanges = bots.subList(0, Math.min(10, bots.size()));

        Map<String, List<WikiUser>> resultMap = new HashMap<>();

        resultMap.put("topUsersMostChanges", topUsersMostChanges);
        resultMap.put("topUsersLeastChanges", topUsersLeastChanges);
        resultMap.put("topBotsMostChanges", topBotsMostChanges);
        resultMap.put("topBotsLeastChanges", topBotsLeastChanges);

        try {
            return mapper.writeValueAsString(resultMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateOrCreateRecentchangeActiveUsers(String activeUsers) {
        Optional<RecentchangeActiveUsersEntity> optionalEntity = recentchangeActiveUsersRepository.findTopByOrderByCreatedAtDesc();

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        if (optionalEntity.isPresent()) {
            RecentchangeActiveUsersEntity entity = optionalEntity.get();
            if (entity.getCreatedAt().isAfter(startOfDay) && entity.getCreatedAt().isBefore(endOfDay)) {
                entity.setValue(activeUsers);
                recentchangeActiveUsersRepository.save(entity);
                return;
            }
        }

        RecentchangeActiveUsersEntity newEntity = new RecentchangeActiveUsersEntity(activeUsers);
        recentchangeActiveUsersRepository.save(newEntity);
    }

    @Autowired
    @Override
    public void buildPipeline(StreamsBuilder streamsBuilder) {

        streamsBuilder.stream("recentchange", Consumed.with(STRING_SERDE, STRING_SERDE))
                .filter((k, v) -> v != null)
                .mapValues(RecentchangeActiveUsersProcessor::readJsonNode)
                .filter((k, v) -> v.hasNonNull("length")
                        && v.hasNonNull("bot")
                        && v.get("length").hasNonNull("old")
                        && v.get("length").hasNonNull("new"))
                .mapValues(RecentchangeActiveUsersProcessor::serializeJsonNode)
                .groupBy((k, v) -> "", Grouped.with(STRING_SERDE, STRING_SERDE))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofDays(1)))
                .aggregate(
                        () -> {
                            wikiUsers = new ConcurrentHashMap<>();
                            return "";
                        },
                        (k, v, ignore) -> {
                            try {
                                JsonNode jsonNode = mapper.readTree(v);
                                String username = jsonNode.get("user").asText();
                                boolean isBot = jsonNode.get("bot").asBoolean();
                                int diff = jsonNode.get("length").get("new").asInt() - jsonNode.get("length").get("old").asInt();

                                WikiUser user = wikiUsers.getOrDefault(k, new WikiUser(username, diff, isBot));
                                user.updateChangesLength(diff);
                                wikiUsers.put(username, user);

                                return "";
                            } catch (JsonProcessingException e) {
                                System.out.println("Polacy nic się nie stało");
                                return "";
                            }
                        },
                        Materialized.with(STRING_SERDE, STRING_SERDE))
                .suppress(Suppressed.untilTimeLimit(Duration.ofSeconds(10), Suppressed.BufferConfig.unbounded()))
                .toStream()
                .map((windowedKey, map) -> {
                    try {
                        String jsonString = mapper.writeValueAsString(wikiUsers.values());
                        jsonString = cropUsersCollection(jsonString);
                        return KeyValue.pair(windowedKeyToString(windowedKey), jsonString);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .peek((k, v) ->
                {
                    System.out.println("k: " + k + ", active users top lists: " + v);
                    updateOrCreateRecentchangeActiveUsers(v);
                })
                .to("recentchange.active_users", Produced.with(STRING_SERDE, STRING_SERDE));

    }
}
