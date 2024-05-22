package galactus.dashboard.component.processor.recentchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import galactus.dashboard.component.processor.BaseEventProcessor;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Suppressed;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.apache.kafka.streams.kstream.Suppressed.BufferConfig.unbounded;

@Component
public class RecentChangeEventCountProcessor extends BaseEventProcessor {

    @Autowired
    @Override
    public void buildPipeline(StreamsBuilder streamsBuilder) {

//        streamsBuilder.stream("recentchange", Consumed.with(STRING_SERDE, STRING_SERDE))
//                .filter((k, v) -> v != null)
//                .mapValues(RecentChangeEventCountProcessor::readJsonNode)
//                .groupBy((k, v) -> "constant-key")
//                .windowedBy(TimeWindows.ofSizeAndGrace(Duration.ofSeconds(10), Duration.ofSeconds(10)))
//                .count()
//                .suppress(Suppressed.untilWindowCloses(unbounded()))
//                .toStream()
//                .map((windowedKey, count) -> KeyValue.pair(windowedKeyToString(windowedKey), count.toString()))
//                .peek((k, v) -> System.out.println("k: " + k + ", v: " + v))
//                .to("recentchange.event_count", Produced.with(STRING_SERDE, STRING_SERDE));

        streamsBuilder.stream("recentchange", Consumed.with(STRING_SERDE, STRING_SERDE))
                .filter((k, v) -> v != null)
                .groupBy((k, v) -> "", Grouped.with(STRING_SERDE, STRING_SERDE))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(10)))
                .count()
                .suppress(Suppressed.untilWindowCloses(unbounded()))
                .toStream()
                .map((windowedKey, count) -> KeyValue.pair(windowedKeyToString(windowedKey), count.toString()))
                .peek((k, v) -> System.out.println("k: " + k + ", v: " + v))
                .to("recentchange.event_count", Produced.with(STRING_SERDE, STRING_SERDE));

    }
}
