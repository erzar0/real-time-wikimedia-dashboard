package galactus.dashboard.component.processor.recentchange;

import com.fasterxml.jackson.databind.JsonNode;
import galactus.dashboard.component.processor.BaseEventProcessor;
import galactus.dashboard.entity.RecentchangeLengthChangeEntity;
import galactus.dashboard.repository.RecentchangeLengthChangeRepository;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Suppressed;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static org.apache.kafka.streams.kstream.Suppressed.BufferConfig.unbounded;

@Component
public class RecentchangeLengthChangeProcessor extends BaseEventProcessor {


    @Autowired
    private RecentchangeLengthChangeRepository recentchangeLengthChangeRepository;

    /**
     * Builds the Kafka Streams pipeline for processing recent change length changes.
     *
     * @param streamsBuilder The StreamsBuilder used to construct the pipeline.
     */
    @Autowired
    @Override
    public void buildPipeline(StreamsBuilder streamsBuilder) {
        streamsBuilder.stream("recentchange", Consumed.with(STRING_SERDE, STRING_SERDE))
                .filter((k, v) -> v != null)
                .mapValues(RecentchangeLengthChangeProcessor::readJsonNode)
                .filter(this::hasLengthNode)
                .mapValues(v -> v.get("length"))
                .filter(this::hasOldAndNewLength)
                .mapValues(this::calculateLengthChange)
                .groupBy((k, v) -> "", Grouped.with(STRING_SERDE, INTEGER_SERDE))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(AGGREGATION_INTERVAL_SECONDS)))
                .aggregate(
                        () -> 0,
                        (k, v, agg) -> agg + v,
                        Materialized.with(STRING_SERDE, INTEGER_SERDE))
                .suppress(Suppressed.untilWindowCloses(unbounded()))
                .toStream()
                .map(this::convertToKeyValue)
                .peek(this::logAndPersist)
                .to("recentchange.length_change", Produced.with(STRING_SERDE, STRING_SERDE));
    }

    /**
     * Checks if the JSON node contains a non-null 'length' field.
     *
     * @param k The key (unused).
     * @param v The JSON node to check.
     * @return true if the 'length' field is non-null, false otherwise.
     */
    private boolean hasLengthNode(String k, JsonNode v) {
        return v.hasNonNull("length");
    }

    /**
     * Checks if the 'length' node contains non-null 'old' and 'new' fields.
     *
     * @param k The key (unused).
     * @param v The 'length' node to check.
     * @return true if both 'old' and 'new' fields are non-null, false otherwise.
     */
    private boolean hasOldAndNewLength(String k, JsonNode v) {
        return v.hasNonNull("old") && v.hasNonNull("new");
    }

    /**
     * Calculates the difference between the 'new' and 'old' lengths.
     *
     * @param v The 'length' node.
     * @return The difference between 'new' and 'old' lengths.
     */
    private Integer calculateLengthChange(JsonNode v) {
        return v.get("new").asInt() - v.get("old").asInt();
    }

    /**
     * Converts the windowed key and aggregate to a KeyValue pair.
     *
     * @param windowedKey The windowed key.
     * @param aggregate   The aggregate value.
     * @return A KeyValue pair with the string representation of the windowed key and the aggregate.
     */
    private KeyValue<String, String> convertToKeyValue(Windowed<String> windowedKey, Integer aggregate) {
        return KeyValue.pair(windowedKeyToString(windowedKey), aggregate.toString());
    }

    /**
     * Logs the length change and persists it to the repository.
     *
     * @param k The key.
     * @param v The length change value.
     */
    private void logAndPersist(String k, String v) {
        System.out.println("k: " + k + ", length change: " + v);
        RecentchangeLengthChangeEntity e = new RecentchangeLengthChangeEntity(Integer.parseInt(v));
        recentchangeLengthChangeRepository.save(e);
    }
}

