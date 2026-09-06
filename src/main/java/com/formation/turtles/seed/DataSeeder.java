package com.formation.turtles.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Remplit les deux bases au demarrage, a chaque lancement. */
@Component
@Order(1)   // le chargement des données passe avant la démo console
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {
    private final SqlSeeder sqlSeeder;
    private final MongoSeeder mongoSeeder;

    @Override
    public void run(ApplicationArguments args) {
        sqlSeeder.seed();
        mongoSeeder.seed();
        log.info("🐢 Jeu de données chargé : {} tortues, {} habitats, {} programmes — des deux côtés.",
                Dataset.TURTLES.size(), Dataset.HABITATS.size(), Dataset.PROGRAMS.size());
    }
}
