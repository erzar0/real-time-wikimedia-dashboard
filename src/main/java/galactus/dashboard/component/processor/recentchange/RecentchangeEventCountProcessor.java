package galactus.dashboard.component.processor.recentchange;

import galactus.dashboard.component.processor.BaseEventProcessor;
import galactus.dashboard.entity.RecentchangeEventCountEntity;
import galactus.dashboard.repository.RecentchangeEventCountRepository;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Suppressed;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static org.apache.kafka.streams.kstream.Suppressed.BufferConfig.unbounded;

@Component
public class RecentchangeEventCountProcessor extends BaseEventProcessor {


    @Autowired
    private RecentchangeEventCountRepository eventchangeEventCountRepository;

    /**
     * Builds the Kafka Streams pipeline for processing recent change event counts.
     *
     * @param streamsBuilder The StreamsBuilder used to construct the pipeline.
     */
    @Autowired
    @Override
    public void buildPipeline(StreamsBuilder streamsBuilder) {

        streamsBuilder.stream("recentchange", Consumed.with(STRING_SERDE, STRING_SERDE))
                .filter((k, v) -> v != null)
                .groupBy((k, v) -> "", Grouped.with(STRING_SERDE, STRING_SERDE))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(AGGREGATION_INTERVAL_SECONDS)))
                .count()
                .suppress(Suppressed.untilWindowCloses(unbounded()))
                .toStream()
                .map((windowedKey, count) -> KeyValue.pair(windowedKeyToString(windowedKey), count.toString()))
                .peek((k, v) -> {
                    System.out.println("k: " + k + ", event count: " + v);
                    RecentchangeEventCountEntity e = new RecentchangeEventCountEntity(Integer.parseInt(v));
                    eventchangeEventCountRepository.save(e);
                })
                .to("recentchange.event_count", Produced.with(STRING_SERDE, STRING_SERDE));

    }
}
