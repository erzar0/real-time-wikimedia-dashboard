package galactus.dashboard.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import galactus.dashboard.component.processor.recentchange.RecentchangeActiveUsersProcessor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

    @Autowired
    @Qualifier("kafkaProducerProps")
    private Properties kafkaProducerProps;

    @Override
    public void run(ApplicationArguments args) {
        try {
            WebClient client = WebClient.create("https://stream.wikimedia.org/v2/stream");

            ParameterizedTypeReference<ServerSentEvent<String>> type = new ParameterizedTypeReference<>() {
            };

            Flux<ServerSentEvent<String>> eventStream = client.get()
                    .uri("/recentchange")
                    .retrieve()
                    .bodyToFlux(type);
            eventStream.onErrorResume(error -> {
                System.out.println("Event stream error");
                return eventStream;
            });

            KafkaProducer<String, String> producer = new KafkaProducer<>(kafkaProducerProps);

            eventStream.subscribe(
                    content -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        if (content != null && content.data() != null) {
                            try {
                                JsonNode dataJsonNode = objectMapper.readTree(content.data());

                                if (dataJsonNode.hasNonNull("user")) {
                                    ProducerRecord<String, String> record = new ProducerRecord<>("recentchange", dataJsonNode.get("user").toString(), dataJsonNode.toString());
                                    producer.send(record);
                                }
                            } catch (JsonProcessingException e) {
                                System.err.println("Error processing event: " + e);
                            }
                        } else {
                            System.err.println("Received null content or event from SSE stream");
                        }
                    },
                    error -> run(null)

            );
        } catch (Exception exception) {
            System.out.println(exception.toString());
        }
    }
}