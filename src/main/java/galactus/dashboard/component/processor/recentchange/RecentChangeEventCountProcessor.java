package galactus.dashboard.component.processor.recentchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
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
public class RecentChangeEventCountProcessor {

    private static final Serde<String> STRING_SERDE = Serdes.String();
    private static final ObjectMapper mapper = new ObjectMapper();


    @Autowired
    void buildPipeline(StreamsBuilder streamsBuilder) {

        streamsBuilder.stream("recentchange", Consumed.with(STRING_SERDE, STRING_SERDE))
                .mapValues(RecentChangeEventCountProcessor::readJsonNode)
                .filter((k, v) -> v != null)
                .groupByKey()
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(10)))
                .count()
                .suppress(Suppressed.untilWindowCloses(unbounded()))
                .toStream()
                .map((windowedKey, count) -> KeyValue.pair(windowedKeyToString(windowedKey), count.toString()))
                .peek((k, v) -> System.out.println("k: " + k + ", v: " + v))
                .to("recentchange.event_count", Produced.with(STRING_SERDE, STRING_SERDE));
    }

    static private JsonNode readJsonNode(String value)
    {
        try {
            return mapper.readTree(value);
        } catch (Exception e) {
            return null;
        }
    }
    static private String windowedKeyToString(Windowed<String> windowedKey) {

        return String.format("[%s@%s/%s]"
                , windowedKey.key()
                , convertEpochToDateTime(windowedKey.window().startTime().getEpochSecond())
                , convertEpochToDateTime(windowedKey.window().endTime().getEpochSecond()));
    }

    static private String convertEpochToDateTime(long epochMillis) {
        Instant instant = Instant.ofEpochMilli(epochMillis);
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return localDateTime.format(formatter);
    }
}
