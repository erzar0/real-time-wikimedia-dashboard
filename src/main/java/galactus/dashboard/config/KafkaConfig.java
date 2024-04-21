package galactus.dashboard.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.COMMIT_INTERVAL_MS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG;

@Configuration
@EnableKafka
@EnableKafkaStreams
public class KafkaConfig {

    @Value(value = "${spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;

    static final private String STRING_SERDE_CLASS_NAME = Serdes.String().getClass().getName();
    static final private String STRING_SERIALIZER_CLASS_NAME = StringSerializer.class.getName();
    static final private String AUTO_OFFSET_RESET = "latest";
    static final private String COMMIT_INTERVAL = "5000";
    static final private String CACHE_MAX_BYTES = "0";


    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    KafkaStreamsConfiguration kStreamsConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(APPLICATION_ID_CONFIG, "streams-app");
        props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, STRING_SERDE_CLASS_NAME);
        props.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, STRING_SERDE_CLASS_NAME);
        props.put(COMMIT_INTERVAL_MS_CONFIG, COMMIT_INTERVAL);
        props.put(STATESTORE_CACHE_MAX_BYTES_CONFIG, CACHE_MAX_BYTES);

        return new KafkaStreamsConfiguration(props);
    }

    @Bean(name = "kafkaProps")
    Properties kafkaProps(){
        Properties kafkaProps = new Properties();
        kafkaProps.put("bootstrap.servers", bootstrapAddress);
        kafkaProps.put("key.serializer", STRING_SERIALIZER_CLASS_NAME);
        kafkaProps.put("value.serializer", STRING_SERIALIZER_CLASS_NAME);
        kafkaProps.put("auto.offset.reset", AUTO_OFFSET_RESET);

        return kafkaProps;
    }

}