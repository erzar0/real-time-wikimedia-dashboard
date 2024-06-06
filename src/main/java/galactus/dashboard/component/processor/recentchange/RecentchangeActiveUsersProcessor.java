package galactus.dashboard.component.processor.recentchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import galactus.dashboard.component.processor.BaseEventProcessor;
import galactus.dashboard.entity.RecentchangeActiveUsersEntity;
import galactus.dashboard.repository.RecentchangeActiveUsersRepository;
import lombok.Data;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Suppressed;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RecentchangeActiveUsersProcessor extends BaseEventProcessor {

    private final static int AGGREGATION_WINDOW_SIZE_DAYS = 1;
    @Autowired
    private RecentchangeActiveUsersRepository recentchangeActiveUsersRepository;
    private ConcurrentHashMap<String, WikiUser> wikiUsers = new ConcurrentHashMap<>();

    /**
     * Constructor for RecentchangeActiveUsersProcessor.
     *
     * @param recentchangeActiveUsersRepository Repository for recording top lists of active users.
     */
    public RecentchangeActiveUsersProcessor(RecentchangeActiveUsersRepository recentchangeActiveUsersRepository) {
        this.recentchangeActiveUsersRepository = recentchangeActiveUsersRepository;
    }

    /**
     * Represents a Wikimedia user with their username, length of the changes made on the pages as amount of characters, and bot status.
     */

    @Data
    private static class WikiUser {
        String username;
        AtomicInteger changesLength = new AtomicInteger();
        boolean isBot;

        /**
         * Constructor for WikiUser.
         *
         * @param username       The username of the user.
         * @param changesLength  The length of changes made by the user.
         * @param isBot          Indicates if the user is a bot.
         */
        @JsonCreator
        public WikiUser(@JsonProperty("username") String username, @JsonProperty("changesLength") int changesLength, @JsonProperty("bot") boolean isBot) {
            this.username = username;
            this.changesLength.set(changesLength);
            this.isBot = isBot;
        }

        /**
         * Updates the changes length by adding the specified difference.
         *
         * @param diff The difference to add to the changes length.
         */
        void updateChangesLength(int diff) {
            changesLength.addAndGet(diff);
        }
    }

    /**
     * Builds the Kafka Streams pipeline.
     *
     * @param streamsBuilder The StreamsBuilder used to construct the pipeline.
     */
    @Autowired
    @Override
    public void buildPipeline(StreamsBuilder streamsBuilder) {

        streamsBuilder.stream("recentchange", Consumed.with(STRING_SERDE, STRING_SERDE))
                .filter((k, v) -> v != null)
                .mapValues(RecentchangeActiveUsersProcessor::readJsonNode)
                .filter(this::isValidJsonNode)
                .mapValues(RecentchangeActiveUsersProcessor::serializeJsonNode)
                .groupBy((k, v) -> "", Grouped.with(STRING_SERDE, STRING_SERDE))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofDays(AGGREGATION_WINDOW_SIZE_DAYS)))
                .aggregate(
                        this::initializeAggregate,
                        this::aggregateChanges,
                        Materialized.with(STRING_SERDE, STRING_SERDE))
                .suppress(Suppressed.untilTimeLimit(Duration.ofSeconds(AGGREGATION_INTERVAL_SECONDS), Suppressed.BufferConfig.unbounded()))
                .toStream()
                .map(this::convertToKeyValue)
                .peek(this::logAndPersist)
                .to("recentchange.active_users", Produced.with(STRING_SERDE, STRING_SERDE));
    }

    /**
     * Crops and serializes the users collection to JSON.
     *
     * @return The JSON string representing the cropped users collection.
     * @throws JsonProcessingException If there is an error processing the JSON.
     */
    private String croppedAndSerializedUsersCollection() throws JsonProcessingException {
        List<WikiUser> users = new ArrayList<>();
        List<WikiUser> bots = new ArrayList<>();

        for (WikiUser wikiUser : wikiUsers.values()) {
            if (wikiUser.isBot()) {
                bots.add(wikiUser);
            } else {
                users.add(wikiUser);
            }
        }

        users.sort(Comparator.comparingInt(a -> a.getChangesLength().get()));

        bots.sort(Comparator.comparingInt(a -> a.getChangesLength().get()));

        List<WikiUser> topUsersMostChanges = users.subList(Math.max(users.size() - 10, 0), users.size());
        List<WikiUser> topUsersLeastChanges = users.subList(0, Math.min(10, users.size()));

        List<WikiUser> topBotsMostChanges = bots.subList(Math.max(bots.size() - 10, 0), bots.size());
        List<WikiUser> topBotsLeastChanges = bots.subList(0, Math.min(10, bots.size()));

        Map<String, List<WikiUser>> resultMap = new HashMap<>();

        resultMap.put("topUsersMostChanges", topUsersMostChanges);
        resultMap.put("topUsersLeastChanges", topUsersLeastChanges);
        resultMap.put("topBotsMostChanges", topBotsMostChanges);
        resultMap.put("topBotsLeastChanges", topBotsLeastChanges);

        try {
            return mapper.writeValueAsString(resultMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates or creates new entry in the repository.
     *
     * @param activeUsers The active users data to update or create.
     */
    public void updateOrCreateRecentchangeActiveUsers(String activeUsers) {
        Optional<RecentchangeActiveUsersEntity> optionalEntity = recentchangeActiveUsersRepository.findTopByOrderByCreatedAtDesc();

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        if (optionalEntity.isPresent()) {
            RecentchangeActiveUsersEntity entity = optionalEntity.get();
            if (entity.getCreatedAt().isAfter(startOfDay) && entity.getCreatedAt().isBefore(endOfDay)) {
                entity.setValue(activeUsers);
                recentchangeActiveUsersRepository.save(entity);
                return;
            }
        }

        RecentchangeActiveUsersEntity newEntity = new RecentchangeActiveUsersEntity(activeUsers);
        recentchangeActiveUsersRepository.save(newEntity);
    }

    /**
     * Validates the JSON node to check if it contains the required fields.
     *
     * @param k The key (unused).
     * @param v The JSON node to validate.
     * @return true if the JSON node is valid, false otherwise.
     */
    private boolean isValidJsonNode(String k, JsonNode v) {
        return v.hasNonNull("length") &&
                v.hasNonNull("bot") &&
                v.get("length").hasNonNull("old") &&
                v.get("length").hasNonNull("new");
    }

    /**
     * Initializes the aggregate by creating a new ConcurrentHashMap.
     *
     * @return An empty string.
     */
    private String initializeAggregate() {
        wikiUsers = new ConcurrentHashMap<>();
        return "";
    }

    /**
     * Aggregates the changes by updating the wikiUsers map.
     *
     * @param k      The key (unused).
     * @param v      The value containing the changes data.
     * @param ignore An ignored parameter.
     * @return An empty string.
     */
    private String aggregateChanges(String k, String v, String ignore) {
        try {
            JsonNode jsonNode = mapper.readTree(v);
            String username = jsonNode.get("user").asText();
            boolean isBot = jsonNode.get("bot").asBoolean();
            int diff = jsonNode.get("length").get("new").asInt() - jsonNode.get("length").get("old").asInt();

            WikiUser user = wikiUsers.getOrDefault(username, new WikiUser(username, diff, isBot));
            user.updateChangesLength(diff);
            wikiUsers.put(username, user);

        } catch (JsonProcessingException e) {
            System.out.println("Polacy nic się nie stało");
        }
        return "";
    }

    /**
     * Converts the aggregated data to a KeyValue pair.
     *
     * @param windowedKey The windowed key.
     * @param ignore      An ignored parameter.
     * @return A KeyValue pair with the windowed key and the cropped and serialized users collection.
     */
    private KeyValue<String, String> convertToKeyValue(Windowed<String> windowedKey, String ignore) {
        try {
            return KeyValue.pair(windowedKeyToString(windowedKey), croppedAndSerializedUsersCollection());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Logs the active users data and updates or creates the new entry.
     *
     * @param k The key.
     * @param v The active users data.
     */
    private void logAndPersist(String k, String v) {
        System.out.println("k: " + k + ", active users top lists: " + v);
        updateOrCreateRecentchangeActiveUsers(v);
    }
}
