# Event Driven Architecture – Kafka & Spring Cloud Stream

## Partie 1 – Kafka en local (avec Zookeeper)

> ⚠️ Kafka 4.x ne supporte plus Zookeeper (mode KRaft uniquement).
> Pour suivre l'énoncé, télécharger **Kafka 3.9.x** (Scala 2.13) : https://kafka.apache.org/downloads

Décompresser l'archive puis, depuis le dossier Kafka :

| Étape | Linux / macOS | Windows |
|---|---|---|
| Zookeeper | `bin/zookeeper-server-start.sh config/zookeeper.properties` | `bin\windows\zookeeper-server-start.bat config\zookeeper.properties` |
| Kafka server | `bin/kafka-server-start.sh config/server.properties` | `bin\windows\kafka-server-start.bat config\server.properties` |
| Producer | `bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic R1` | `bin\windows\kafka-console-producer.bat --bootstrap-server localhost:9092 --topic R1` |
| Consumer | `bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic R1 --from-beginning` | `bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic R1 --from-beginning` |

Chaque commande dans un terminal séparé. Taper des messages dans le producer → ils apparaissent dans le consumer.

Astuce Windows : placer Kafka dans un chemin court sans espaces (ex. `C:\kafka`).

## Partie 2 – Kafka avec Docker

```bash
docker compose up -d          # démarre zookeeper + broker
docker ps                     # vérifier les 2 conteneurs

# Créer un topic
docker exec broker kafka-topics --bootstrap-server broker:29092 --create --topic R2

# Producer (terminal 1)
docker exec -it broker kafka-console-producer --bootstrap-server broker:29092 --topic R2

# Consumer (terminal 2)
docker exec -it broker kafka-console-consumer --bootstrap-server broker:29092 --topic R2 --from-beginning
```

Arrêt : `docker compose down`.

## Partie 3 – Spring Cloud Stream

Prérequis : Java 17+, Maven, Kafka démarré sur `localhost:9092` (partie 1 ou 2).

```bash
mvn spring-boot:run
```

### Architecture

```
GET /publish/T1/P1 ──StreamBridge──▶ [T1] ──▶ pageEventConsumer (log)

pageEventSupplier (toutes les 200 ms) ──▶ [T2] ──▶ kStreamFunction (Kafka Streams)
                                                   filter duration>100
                                                   groupBy page, fenêtre 5 s, count
                                                        │            │
                                                        ▼            ▼
                                                      [T3]     store "page-count"
                                                        │            │
                                        analyticsConsumer (log)   GET /analytics (SSE)
                                                                     │
                                                              index.html (Smoothie.js)
```

| Service | Classe / bean | Topic |
|---|---|---|
| Producer REST | `PageEventRestController.publish` | T1 (ou tout topic dans l'URL) |
| Consumer | `pageEventConsumer` | T1 |
| Supplier | `pageEventSupplier` | T2 |
| Stream processing | `kStreamFunction` | T2 → T3 |
| Résultats temps réel | `GET /analytics` (Server-Sent Events) | store interactif |
| Application web | `static/index.html` | — |

### Tests

- Producer : http://localhost:8080/publish/T1/P1 → l'évènement s'affiche dans la console (Consumer).
- Voir le Supplier : `kafka-console-consumer --bootstrap-server localhost:9092 --topic T2`
- Voir les résultats du stream :
  ```bash
  kafka-console-consumer --bootstrap-server localhost:9092 --topic T3 \
    --property print.key=true \
    --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
  ```
- Flux brut : http://localhost:8080/analytics
- Dashboard temps réel : http://localhost:8080
