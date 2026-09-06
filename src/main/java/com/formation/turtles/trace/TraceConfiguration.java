package com.formation.turtles.trace;

import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Branche le mouchard MongoDB sur le pilote.
 *
 * Cote SQL le branchement se fait par une propriete dans exemple-application.yml ;
 * cote Mongo il se fait ici, sur le MongoClientSettings.
 */
@Configuration
public class TraceConfiguration {
    @Bean
    MongoClientSettingsBuilderCustomizer mongoCommandTracer() {
        return settings -> settings.addCommandListener(new MongoCommandTracer());
    }
}
