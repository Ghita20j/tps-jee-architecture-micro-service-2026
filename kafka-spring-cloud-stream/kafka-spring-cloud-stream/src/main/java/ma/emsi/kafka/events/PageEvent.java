package ma.emsi.kafka.events;

import java.util.Date;

/** Evenement de visite d'une page web. */
public record PageEvent(String name, String user, Date date, long duration) {
}
