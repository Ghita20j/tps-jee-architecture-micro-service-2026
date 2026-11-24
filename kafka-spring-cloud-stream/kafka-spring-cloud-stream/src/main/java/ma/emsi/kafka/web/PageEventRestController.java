package ma.emsi.kafka.web;

import ma.emsi.kafka.events.PageEvent;
import ma.emsi.kafka.handlers.PageEventHandler;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
public class PageEventRestController {

    private final StreamBridge streamBridge;
    private final InteractiveQueryService interactiveQueryService;
    private final Random random = new Random();

    public PageEventRestController(StreamBridge streamBridge,
                                   InteractiveQueryService interactiveQueryService) {
        this.streamBridge = streamBridge;
        this.interactiveQueryService = interactiveQueryService;
    }

    /** Service Producer : GET /publish/T1/P1 publie un evenement dans le topic T1. */
    @GetMapping("/publish/{topic}/{name}")
    public PageEvent publish(@PathVariable String topic, @PathVariable String name) {
        PageEvent event = new PageEvent(name,
                random.nextBoolean() ? "U1" : "U2",
                new Date(),
                10 + random.nextInt(10000));
        streamBridge.send(topic, event);
        return event;
    }

    /** Flux SSE : resultats de l'analyse temps reel, emis chaque seconde. */
    @GetMapping(value = "/analytics", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Long>> analytics() {
        return Flux.interval(Duration.ofSeconds(1)).map(seq -> {
            Map<String, Long> result = new HashMap<>();
            try {
                ReadOnlyWindowStore<String, Long> store = interactiveQueryService.getQueryableStore(
                        PageEventHandler.PAGE_COUNT_STORE, QueryableStoreTypes.windowStore());
                Instant now = Instant.now();
                try (KeyValueIterator<Windowed<String>, Long> it = store.fetchAll(now.minusSeconds(5), now)) {
                    while (it.hasNext()) {
                        KeyValue<Windowed<String>, Long> kv = it.next();
                        result.put(kv.key.key(), kv.value);
                    }
                }
            } catch (Exception e) {
                // Le store n'est pas encore pret (demarrage de Kafka Streams)
            }
            return result;
        });
    }
}
