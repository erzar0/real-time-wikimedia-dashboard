package galactus.dashboard.component.processor.recentchange;

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
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RecentchangeMostActiveUsersProcessor extends BaseEventProcessor {

    @Data
    private static class WikiUser {
        String username;
        AtomicInteger changesLength = new AtomicInteger();
        boolean isBot;

        public WikiUser(String username, int changesLength, boolean isBot)
        {
            this.username = username;
            this.changesLength.set(changesLength);
            this.isBot = isBot;
        }

        void updateChangesLength(int diff) {
            changesLength.addAndGet(diff);
        }
    }

    @Autowired
    @Override
    public void buildPipeline(StreamsBuilder streamsBuilder) {

        streamsBuilder.stream("recentchange", Consumed.with(STRING_SERDE, STRING_SERDE))
                .filter((k, v) -> v != null)
                .mapValues(RecentchangeMostActiveUsersProcessor::readJsonNode)
                .filter((k, v) -> v.hasNonNull("length") && v.hasNonNull("bot") && v.hasNonNull("user"))
                .filter((k, v) -> v.get("length").hasNonNull("old") && v.get("length").hasNonNull("new"))
                .groupBy((k, v) -> "", Grouped.with(STRING_SERDE, new JsonSerde<JsonNode>()))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofHours(1)))
                .aggregate(
                        () -> new ConcurrentHashMap<String, WikiUser>(),
                        (k, v, map) -> {
                            String username = v.get("user").asText();
                            boolean isBot = v.get("bot").asBoolean();
                            int diff = v.get("length").get("new").asInt() - v.get("length").get("old").asInt();

                            WikiUser user = map.getOrDefault(k, new WikiUser(username, diff, isBot));
                            user.updateChangesLength(diff);
                            map.put(username, user);

                            return map;
                        },
                        Materialized.with(STRING_SERDE, new JsonSerde<>(ConcurrentHashMap.class)))
                .suppress(Suppressed.untilTimeLimit(Duration.ofSeconds(10), Suppressed.BufferConfig.unbounded()))

                .toStream()
                .map((windowedKey, json) -> KeyValue.pair(windowedKeyToString(windowedKey), json.toString()))
                .peek((k, v) -> System.out.println("k: " + k + ", top list: " + v))
                .to("recentchange.most_active_users", Produced.with(STRING_SERDE, STRING_SERDE));

    }
}
