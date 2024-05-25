package galactus.dashboard.component.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Windowed;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public abstract class BaseEventProcessor {

    protected static final ObjectMapper mapper = new ObjectMapper();
    protected static final Serde<String> STRING_SERDE = Serdes.String();
    protected static final Serde<Integer> INTEGER_SERDE = Serdes.Integer();

    public abstract void buildPipeline(StreamsBuilder streamsBuilder);

    protected static JsonNode readJsonNode(String value) {
        try {
            return mapper.readTree(value);
        } catch (Exception e) {
            return null;
        }
    }

    protected static String serializeJsonNode(JsonNode value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    protected static String windowedKeyToString(Windowed<String> windowedKey) {
        return String.format("%s@%s",
                windowedKey.key(),
                convertEpochToDateTime(windowedKey.window().endTime().getEpochSecond()));
    }

    private static String convertEpochToDateTime(long epochSecs) {
        Instant instant = Instant.ofEpochSecond(epochSecs);
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return localDateTime.format(formatter);
    }
}
