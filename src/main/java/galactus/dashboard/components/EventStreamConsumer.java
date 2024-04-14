package galactus.dashboard.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import java.util.Properties;


@Component
public class EventStreamConsumer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        try {
            WebClient client = WebClient.create("https://stream.wikimedia.org/v2/stream");

            ParameterizedTypeReference<ServerSentEvent<String>> type = new ParameterizedTypeReference<>() {};

            Flux<ServerSentEvent<String>> eventStream = client.get()
                    .uri("/recentchange")
                    .retrieve()
                    .bodyToFlux(type)
                    .onErrorContinue((error, obj) -> System.out.printf("error:[%s], obj:[%s]%n", error, obj));

            Properties kafkaProps = new Properties();
            kafkaProps.put("bootstrap.servers", "kafka:9092"); // Replace with your Kafka broker's address
            kafkaProps.put("key.serializer", StringSerializer.class.getName());
            kafkaProps.put("value.serializer", StringSerializer.class.getName());

            KafkaProducer<String, String> producer = new KafkaProducer<>(kafkaProps);

            eventStream.subscribe(
                    content -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        if (content != null && content.event() != null) {

                            try {
                                JsonNode jsonNode = objectMapper.readTree(content.data());

                                ProducerRecord<String, String> record = new ProducerRecord<>("recentchange", jsonNode.toString());
                                producer.send(record);

                                System.out.println(record);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            System.err.println("Received null content or event from SSE stream");
                        }
                    },
                    error -> System.err.println("Error receiving SSE: " + error)
            );
        } catch (Exception exception)
        {
            System.out.println(exception.toString());
        }
    }
}