package galactus.dashboard.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.COMMIT_INTERVAL_MS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG;

@Configuration
public class KafkaConfig {

    @Value(value = "${spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;

    @Value(value = "${spring.kafka.consumer.group-id}")
    private String groupId;

    static final private String STRING_SERDE_CLASS_NAME = Serdes.String().getClass().getName();
    static final private String STRING_SERIALIZER_CLASS_NAME = StringSerializer.class.getName();
    static final private String STRING_DESERIALIZER_CLASS_NAME = StringDeserializer.class.getName();
    static final private String AUTO_OFFSET_RESET = "latest";
    static final private String COMMIT_INTERVAL = "5000";
    static final private String REQUEST_TIMEOUT_MS = "120000";
    static final private String CACHE_MAX_BYTES = "0";
    static final private String MAX_REQUEST_SIZE = "10485880";


    /**
     * Provides the default Kafka Streams configuration.
     *
     * @return a {@link KafkaStreamsConfiguration} object containing the configuration properties for Kafka Streams.
     */
    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    KafkaStreamsConfiguration kStreamsConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(APPLICATION_ID_CONFIG, "streams-app");
        props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, STRING_SERDE_CLASS_NAME);
        props.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, STRING_SERDE_CLASS_NAME);
        props.put(COMMIT_INTERVAL_MS_CONFIG, COMMIT_INTERVAL);
        props.put(STATESTORE_CACHE_MAX_BYTES_CONFIG, CACHE_MAX_BYTES);
        props.put(REQUEST_TIMEOUT_MS_CONFIG, REQUEST_TIMEOUT_MS);
        props.put(StreamsConfig.producerPrefix("max.request.size"), MAX_REQUEST_SIZE);

        return new KafkaStreamsConfiguration(props);
    }

    /**
     * Provides the configuration properties for Kafka Producer.
     *
     * @return a {@link Properties} object containing the configuration properties for Kafka Producer.
     */
    @Bean(name = "kafkaProducerProps")
    public Properties kafkaProducerProps(){
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapAddress);
        props.put("key.serializer", STRING_SERIALIZER_CLASS_NAME);
        props.put("value.serializer", STRING_SERIALIZER_CLASS_NAME);
        props.put("max.request.size", MAX_REQUEST_SIZE);

        return props;
    }

    /**
     * Provides the configuration properties for Kafka Consumer.
     *
     * @return a {@link Map} object containing the configuration properties for Kafka Consumer.
     */
    @Bean(name = "kafkaConsumerProps")
    public Map<String,Object> kafkaConsumerProps(){
        Map<String,Object> props= new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, STRING_DESERIALIZER_CLASS_NAME);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, STRING_DESERIALIZER_CLASS_NAME);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, AUTO_OFFSET_RESET);

        return props;
    }

    /**
     * Provides a {@link ConsumerFactory} for creating Kafka Consumers.
     *
     * @return a {@link ConsumerFactory} object for creating Kafka Consumers.
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory(){
        return new DefaultKafkaConsumerFactory<>(kafkaConsumerProps());
    }

    /**
     * Provides a {@link ConcurrentKafkaListenerContainerFactory} for creating Kafka Listener Containers.
     *
     * @return a {@link KafkaListenerContainerFactory} object for creating Kafka Listener Containers.
     */
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String,String>> kafkaListenerContainerFactory(){
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

}
