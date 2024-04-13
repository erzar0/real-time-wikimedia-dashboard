package galactus.dashboard.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;


@Component
public class EventStreamConsumer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            WebClient client = WebClient.create("https://stream.wikimedia.org/v2/stream");

            ParameterizedTypeReference<ServerSentEvent<String>> type = new ParameterizedTypeReference<ServerSentEvent<String>>() {};

            Flux<ServerSentEvent<String>> eventStream = client.get()
                    .uri("/recentchange")
                    .retrieve()
                    .bodyToFlux(type);

            eventStream.subscribe(
                    content -> {
                        ObjectMapper objectMapper = new ObjectMapper();

                        JsonNode jsonNode = null;
                        if(content != null) {
                            try {
                                jsonNode = objectMapper.readTree(content.event());

//                                String eventId = jsonNode.get("spring-boot-app").get("ServerSentEvent").get("id").asText();
//                                String event = jsonNode.get("spring-boot-app").get("ServerSentEvent").get("event").asText();
//                                String data = jsonNode.get("spring-boot-app").get("ServerSentEvent").get("data").toString();

                                System.out.println(jsonNode.toPrettyString());
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
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