package com.formation.turtles.mongo.config;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Rend les types BSON bruts lisibles dans les reponses JSON.
 *
 * Sans ca, Jackson serialise un ObjectId par ses accesseurs
 * ({"timestamp": …, "date": …}) : illisible, et surtout different de ce que
 * montrent la trace de commande, mongosh et Compass.
 *
 * On adopte donc la meme convention qu'eux — l'Extended JSON de MongoDB :
 * {"$oid": "…"} et {"$date": "…"}. Ne concerne que les documents lus
 * en org.bson.Document (endpoints /api/mongo/concepts/**) : les objets mappes
 * exposent, eux, des types Java normaux.
 */
@Configuration
public class BsonJacksonConfig {
    @Bean
    SimpleModule bsonJsonModule() {
        SimpleModule module = new SimpleModule("bson-extended-json");
        module.addSerializer(ObjectId.class, new JsonSerializer<>() {
            @Override
            public void serialize(ObjectId value, JsonGenerator json, SerializerProvider providers)
                    throws IOException {
                json.writeStartObject();
                json.writeStringField("$oid", value.toHexString());
                json.writeEndObject();
            }
        });
        module.addSerializer(Date.class, new JsonSerializer<>() {
            @Override
            public void serialize(Date value, JsonGenerator json, SerializerProvider providers)
                    throws IOException {
                json.writeStartObject();
                json.writeStringField("$date", Instant.ofEpochMilli(value.getTime()).toString());
                json.writeEndObject();
            }
        });
        return module;
    }
}
