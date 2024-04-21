package galactus.dashboard.component.processor.recentchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Suppressed;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.TimeZone;

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
                .map((windowedKey, count) -> KeyValue.pair(windowedKey.toString(), count.toString()))
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
}
