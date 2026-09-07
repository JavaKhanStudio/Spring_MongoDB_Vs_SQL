package com.formation.turtles.seed;

import com.formation.turtles.mongo.repo.HabitatMongoRepository;
import com.formation.turtles.mongo.repo.ProgramMongoRepository;
import com.formation.turtles.mongo.repo.TurtleMongoRepository;
import com.formation.turtles.sql.repo.HabitatRepository;
import com.formation.turtles.sql.repo.ProgramRepository;
import com.formation.turtles.sql.repo.TurtleRepository;
import com.formation.turtles.trace.QueryTraces;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Remplit les deux bases, a la demande.
 *
 * Le chargement n'a volontairement pas lieu au demarrage : H2 vit dans la JVM et
 * survit tant que l'application tourne, alors que MongoDB est un serveur separe qui
 * peut redemarrer, etre recree ou etre vide sans que l'application le sache. Un
 * seed au demarrage seul donne alors deux mondes desynchronises. Ici, une seule
 * commande (POST /api/seed) remet les deux a plat.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeedService {
    private final SqlSeeder sqlSeeder;
    private final MongoSeeder mongoSeeder;

    private final TurtleRepository sqlTurtles;
    private final HabitatRepository sqlHabitats;
    private final ProgramRepository sqlPrograms;

    private final TurtleMongoRepository mongoTurtles;
    private final HabitatMongoRepository mongoHabitats;
    private final ProgramMongoRepository mongoPrograms;

    /** Vide puis recharge les deux bases. Idempotent : relancable autant de fois qu'on veut. */
    public SeedReport seed() {
        long start = System.currentTimeMillis();
        sqlSeeder.seed();
        mongoSeeder.seed();
        long millis = System.currentTimeMillis() - start;

        // Le chargement produit des centaines de requetes : on ne les fait pas remonter
        // dans la reponse HTTP, elles noieraient le mouchard pedagogique.
        QueryTraces.reset();

        SeedReport report = SeedReport.of("chargement", millis, sqlCounts(), mongoCounts());
        log.info("🐢 Jeu de données chargé en {} ms : {} tortues, {} habitats, {} programmes — des deux côtés.",
                millis, report.mongo().turtles(), report.mongo().habitats(), report.mongo().programs());
        return report;
    }

    /** Ce que contiennent les deux bases, sans rien y toucher. */
    public SeedReport status() {
        return SeedReport.of("etat", 0, sqlCounts(), mongoCounts());
    }

    private SeedReport.Counts sqlCounts() {
        return new SeedReport.Counts(sqlTurtles.count(), sqlHabitats.count(), sqlPrograms.count());
    }

    private SeedReport.Counts mongoCounts() {
        return new SeedReport.Counts(mongoTurtles.count(), mongoHabitats.count(), mongoPrograms.count());
    }
}
