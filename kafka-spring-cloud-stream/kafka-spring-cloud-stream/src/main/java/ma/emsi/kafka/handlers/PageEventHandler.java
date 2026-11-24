package ma.emsi.kafka.handlers;

import ma.emsi.kafka.events.PageEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
public class PageEventHandler {

    public static final String PAGE_COUNT_STORE = "page-count";
    private final Random random = new Random();

    /** Service Consumer : affiche les evenements recus sur T1. */
    @Bean
    public Consumer<PageEvent> pageEventConsumer() {
        return event -> {
            System.out.println("*******************");
            System.out.println("Consumer recu : " + event);
            System.out.println("*******************");
        };
    }

    /** Service Supplier : genere un evenement aleatoire periodiquement (T2). */
    @Bean
    public Supplier<PageEvent> pageEventSupplier() {
        return () -> new PageEvent(
                random.nextBoolean() ? "P1" : "P2",
                random.nextBoolean() ? "U1" : "U2",
                new Date(),
                10 + random.nextInt(10000));
    }

    /**
     * Service Data Analytics (Kafka Streams) :
     * nombre de visites par page sur des fenetres de 5 secondes.
     */
    @Bean
    public Function<KStream<String, PageEvent>, KStream<String, Long>> kStreamFunction() {
        return input -> input
                .filter((k, v) -> v.duration() > 100)
                .map((k, v) -> new KeyValue<>(v.name(), 0L))
                .groupByKey(Grouped.with(Serdes.String(), Serdes.Long()))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(5)))
                .count(Materialized.<String, Long, WindowStore<Bytes, byte[]>>as(PAGE_COUNT_STORE))
                .toStream()
                .map((windowedKey, count) -> new KeyValue<>(windowedKey.key(), count));
    }

    /** Consumer des resultats de l'analyse (T3). */
    @Bean
    public Consumer<Message<Long>> analyticsConsumer() {
        return msg -> System.out.println("Analytics -> " + msg.getHeaders().get("kafka_receivedMessageKey")
                + " = " + msg.getPayload());
    }
}
