package com.formation.turtles.trace;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Collecteur de requetes, par thread.
 *
 * Le pilote MongoDB synchrone et l'intercepteur Hibernate travaillent tous les
 * deux sur le thread appelant : un simple ThreadLocal suffit donc a
 * rattacher les requetes capturees a la requete HTTP en cours. Ce n'est pas un
 * outil de production (ce serait le role de Micrometer / OpenTelemetry), c'est un
 * micro-mouchard pedagogique.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class QueryTraces {
    private static final ThreadLocal<List<QueryTrace>> CURRENT = ThreadLocal.withInitial(ArrayList::new);

    public static void add(QueryTrace trace) {
        CURRENT.get().add(trace);
    }

    /** Les requetes capturees depuis le dernier reset(). */
    public static List<QueryTrace> captured() {
        return List.copyOf(CURRENT.get());
    }

    public static void reset() {
        CURRENT.get().clear();
    }

    public static void release() {
        CURRENT.remove();
    }
}
