package galactus.dashboard.component.processor.recentchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import galactus.dashboard.component.processor.BaseEventProcessor;
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
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RecentchangeActiveUsersProcessor extends BaseEventProcessor {

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

        // Iterate over the list
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
                        () -> new ConcurrentHashMap<String, WikiUser>(),
                        (k, v, map) -> {
                            try {
                                JsonNode jsonNode = mapper.readTree(v);
                                String username = jsonNode.get("user").asText();
                                boolean isBot = jsonNode.get("bot").asBoolean();
                                int diff = jsonNode.get("length").get("new").asInt() - jsonNode.get("length").get("old").asInt();

                                WikiUser user = map.getOrDefault(k, new WikiUser(username, diff, isBot));
                                user.updateChangesLength(diff);
                                map.put(username, user);

                                return map;
                            } catch (JsonProcessingException e) {
                                System.out.println("Polacy nic się nie stało");
                                return map;
                            }
                        },
                        Materialized.with(STRING_SERDE, new JsonSerde<>(ConcurrentHashMap.class)))
                .suppress(Suppressed.untilTimeLimit(Duration.ofSeconds(30), Suppressed.BufferConfig.unbounded()))
                .toStream()
                .peek((k, v) -> {
                    try {
                        System.out.println(mapper.writeValueAsString(v.values()));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map((windowedKey, map) -> {
                    try {
                        String jsonString = mapper.writeValueAsString(map.values());
                        jsonString = cropUsersCollection(jsonString);
                        return KeyValue.pair(windowedKeyToString(windowedKey), jsonString);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .peek((k, v) -> System.out.println("k: " + k + ", top list: " + v))
                .to("recentchange.active_users", Produced.with(STRING_SERDE, STRING_SERDE));

    }
}
