package galactus.dashboard.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

            ParameterizedTypeReference<ServerSentEvent<String>> type = new ParameterizedTypeReference<>() {};

            Flux<ServerSentEvent<String>> eventStream = client.get()
                    .uri("/recentchange")
                    .retrieve()
                    .bodyToFlux(type)
                    .onErrorContinue((error, obj) -> System.out.printf("error:[%s], obj:[%s]%n", error, obj));

            KafkaProducer<String, String> producer = new KafkaProducer<>(kafkaProducerProps);

            eventStream.subscribe(
                    content -> {
                        if (content != null && content.event() != null) {
                            ObjectMapper objectMapper = new ObjectMapper();
                            try {
                                JsonNode dataJsonNode = objectMapper.readTree(content.data());

                                ProducerRecord<String, String> record = new ProducerRecord<>("recentchange", "null", dataJsonNode.toString());

                                producer.send(record);
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