package galactus.dashboard.component.processor.recentchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RecentchangeActiveUsersProcessor extends BaseEventProcessor {

    @Data
    private static class WikiUser {
        String username;
        AtomicInteger changesLength = new AtomicInteger();
        boolean isBot;

        public WikiUser(String username, int changesLength, boolean isBot) {
            this.username = username;
            this.changesLength.set(changesLength);
            this.isBot = isBot;
        }

        void updateChangesLength(int diff) {
            changesLength.addAndGet(diff);
        }
    }

    private static String serializeCollectionToJson(Collection<WikiUser> map) {
        try {
            return mapper.writeValueAsString(map);
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
                .map((windowedKey, map) -> {
                    String jsonString = serializeCollectionToJson(map.values());
                    return KeyValue.pair(windowedKeyToString(windowedKey), jsonString);
                })
                .peek((k, v) -> System.out.println("k: " + k + ", top list: " + v))
                .to("recentchange.active_users", Produced.with(STRING_SERDE, STRING_SERDE));

    }
}
