package galactus.dashboard.component.processor.recentchange;

import galactus.dashboard.component.processor.BaseEventProcessor;
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

import static org.apache.kafka.streams.kstream.Suppressed.BufferConfig.unbounded;

@Component
public class RecentchangeLengthChangeProcessor extends BaseEventProcessor {

    @Autowired
    @Override
    public void buildPipeline(StreamsBuilder streamsBuilder) {

        streamsBuilder.stream("recentchange", Consumed.with(STRING_SERDE, STRING_SERDE))
                .filter((k, v) -> v != null)
                .mapValues(RecentchangeLengthChangeProcessor::readJsonNode)
                .filter((k, v) -> v.hasNonNull("length"))
                .mapValues(v -> v.get("length"))
                .filter((k, v) -> v.hasNonNull("old") && v.hasNonNull("new"))
                .mapValues((k, v) -> v.get("new").asInt() - v.get("old").asInt())
                .groupBy((k, v) -> "", Grouped.with(STRING_SERDE, INTEGER_SERDE))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(10)))
                .aggregate(
                        () -> 0,
                        (k, v, agg) -> agg + v,
                        Materialized.with(STRING_SERDE, INTEGER_SERDE))
                .suppress(Suppressed.untilWindowCloses(unbounded()))
                .toStream()
                .map((windowedKey, aggregate) -> KeyValue.pair(windowedKeyToString(windowedKey), aggregate.toString()))
                .peek((k, v) -> System.out.println("k: " + k + ", length change: " + v))
                .to("recentchange.length_change", Produced.with(STRING_SERDE, STRING_SERDE));

    }
}
