package com.formation.turtles.seed;

import java.util.HashMap;
import java.util.Map;

import com.formation.turtles.sql.model.Habitat;
import com.formation.turtles.sql.model.Measurements;
import com.formation.turtles.sql.model.Observation;
import com.formation.turtles.sql.model.Program;
import com.formation.turtles.sql.model.Protocol;
import com.formation.turtles.sql.model.Turtle;
import com.formation.turtles.sql.repo.HabitatRepository;
import com.formation.turtles.sql.repo.ProgramRepository;
import com.formation.turtles.sql.repo.TurtleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Charge Dataset dans H2 : six tables a remplir dans le bon ordre. */
@Component
@RequiredArgsConstructor
public class SqlSeeder {
    private final TurtleRepository turtles;
    private final HabitatRepository habitats;
    private final ProgramRepository programs;

    @Transactional
    public void seed() {
        // Les enfants d'abord : turtle_tag, observation et turtle_program partent
        // en cascade avec leur tortue.
        turtles.deleteAll();
        habitats.deleteAll();
        programs.deleteAll();
        // Sans ce flush, Hibernate garderait les suppressions en attente et les
        // executerait APRES les insertions ci-dessous (l'ordre du flush est fige :
        // insertions puis suppressions). Un second chargement violerait alors la
        // contrainte d'unicite sur habitat.name.
        turtles.flush();

        // 1. Les parents d'abord : sans habitat en base, pas de cle etrangere valide.
        Map<String, Habitat> byName = new HashMap<>();
        for (Dataset.HabitatSeed seed : Dataset.HABITATS) {
            Habitat habitat = habitats.save(
                    new Habitat(null, seed.name(), seed.ocean(), seed.waterTempC(), seed.protectedArea()));
            byName.put(seed.name(), habitat);
        }

        // 1 bis. Les programmes : eux aussi doivent exister avant d'etre references.
        Map<String, Program> programsByName = new HashMap<>();
        for (Dataset.ProgramSeed seed : Dataset.PROGRAMS) {
            programsByName.put(seed.name(), programs.save(new Program(
                    seed.name(), seed.acronym(), seed.organisation(), seed.principalInvestigator(),
                    seed.contactEmail(), seed.website(), seed.description(), seed.fundingEuros(),
                    seed.startYear(), seed.endYear(), seed.status(), seed.focusSpecies(),
                    seed.coordinationCountry(),
                    new Protocol(seed.protocol().samplingIntervalDays(), seed.protocol().taggingMethod(),
                            seed.protocol().minShellLengthCm(), seed.protocol().satelliteTracking()))));
        }

        // 2. Puis les tortues, leurs tags et leurs observations (cascade ALL).
        for (Dataset.TurtleSeed seed : Dataset.TURTLES) {
            Turtle turtle = new Turtle(seed.name(), seed.species(), seed.sex(), seed.birthYear(),
                    new Measurements(seed.shellLengthCm(), seed.weightKg()));
            seed.tags().forEach(turtle::addTag);
            // Goliath vit "en Océan ouvert" : aucun habitat connu -> FK nulle.
            turtle.setHabitat(byName.get(seed.habitatName()));
            seed.observations().forEach(o ->
                    turtle.addObservation(new Observation(o.date(), o.site(), o.observer(), o.healthScore())));
            // Chaque inscription = une ligne dans la table de jointure turtle_program.
            seed.programs().forEach(name -> turtle.enrolIn(programsByName.get(name)));
            turtles.save(turtle);
        }
    }
}
