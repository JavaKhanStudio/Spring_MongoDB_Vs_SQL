package com.formation.turtles.trace;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.CommandSucceededEvent;
import org.bson.BsonDocument;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

/**
 * Ecouteur de commandes du pilote MongoDB.
 *
 * Tout ce que Spring Data Mongo fabrique (find, aggregate, update...) finit en une
 * commande BSON envoyee au serveur. On l'attrape ici et on la rend en JSON : les
 * etudiants voient le document que leur derived query, leur @Query ou leur
 * pipeline ont reellement produit.
 */
public class MongoCommandTracer implements CommandListener {
    /** Commandes de plomberie du pilote : sans interet pedagogique. */
    private static final Set<String> IGNORED = Set.of(
            "hello", "ismaster", "ping", "buildInfo", "getLastError", "endSessions",
            "saslStart", "saslContinue", "createIndexes", "listCollections");

    private static final JsonWriterSettings READABLE = JsonWriterSettings.builder()
            .outputMode(JsonMode.RELAXED)   // {"$oid": "..."} plutot que du binaire brut
            .indent(true)
            .build();

    /** Champs techniques ajoutes par le pilote, sans interet pour le cours. */
    private static final Set<String> NOISE = Set.of(
            "lsid", "txnNumber", "$clusterTime", "$readPreference", "$db", "signature");

    private final Map<Integer, String> inFlight = new ConcurrentHashMap<>();

    @Override
    public void commandStarted(CommandStartedEvent event) {
        if (IGNORED.contains(event.getCommandName())) {
            return;
        }
        // On recopie la commande sans la plomberie de session : les etudiants doivent
        // voir la requete, pas l'identifiant de session ni l'horloge du cluster.
        // (Le document d'origine est un RawBsonDocument : immuable, on ne peut pas
        // simplement lui retirer des cles.)
        BsonDocument readable = new BsonDocument();
        event.getCommand().forEach((key, value) -> {
            if (!NOISE.contains(key)) {
                readable.put(key, value);
            }
        });
        inFlight.put(event.getRequestId(), readable.toJson(READABLE));
    }

    @Override
    public void commandSucceeded(CommandSucceededEvent event) {
        String command = inFlight.remove(event.getRequestId());
        if (command != null) {
            QueryTraces.add(QueryTrace.mongo(command, event.getElapsedTime(TimeUnit.MILLISECONDS)));
        }
    }

    @Override
    public void commandFailed(CommandFailedEvent event) {
        inFlight.remove(event.getRequestId());
    }
}
