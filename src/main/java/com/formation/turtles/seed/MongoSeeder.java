package com.formation.turtles.seed;

import java.util.HashMap;
import java.util.Map;

import com.formation.turtles.mongo.model.Habitat;
import com.formation.turtles.mongo.model.Measurements;
import com.formation.turtles.mongo.model.Observation;
import com.formation.turtles.mongo.model.Program;
import com.formation.turtles.mongo.model.Protocol;
import com.formation.turtles.mongo.model.Turtle;
import com.formation.turtles.mongo.model.TurtleWithDbRef;
import com.formation.turtles.mongo.repo.HabitatMongoRepository;
import com.formation.turtles.mongo.repo.ProgramMongoRepository;
import com.formation.turtles.mongo.repo.TurtleDbRefRepository;
import com.formation.turtles.mongo.repo.TurtleMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Charge le meme Dataset dans MongoDB.
 *
 * A comparer avec SqlSeeder : ici, une tortue = un save. Ses tags
 * et ses observations partent avec elle, il n'y a rien a inserer ailleurs et aucun
 * ordre a respecter (sauf pour les habitats, qu'on veut referencer).
 */
@Component
@RequiredArgsConstructor
public class MongoSeeder {
    private final TurtleMongoRepository turtles;
    private final HabitatMongoRepository habitats;
    private final TurtleDbRefRepository dbRefTurtles;
    private final ProgramMongoRepository programs;

    public void seed() {
        turtles.deleteAll();
        habitats.deleteAll();
        dbRefTurtles.deleteAll();
        programs.deleteAll();

        Map<String, Habitat> byName = new HashMap<>();
        for (Dataset.HabitatSeed seed : Dataset.HABITATS) {
            Habitat habitat = habitats.save(
                    new Habitat(seed.name(), seed.ocean(), seed.waterTempC(), seed.protectedArea()));
            byName.put(seed.name(), habitat);
        }

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

        for (Dataset.TurtleSeed seed : Dataset.TURTLES) {
            Turtle turtle = new Turtle(seed.name(), seed.species(), seed.sex(), seed.birthYear(),
                    new Measurements(seed.shellLengthCm(), seed.weightKg()));
            seed.tags().forEach(turtle::addTag);
            seed.observations().forEach(o ->
                    turtle.addObservation(new Observation(o.date(), o.site(), o.observer(), o.healthScore())));

            // Aucune table de jointure ici : la tortue porte les identifiants.
            seed.programs().forEach(name -> turtle.enrolIn(programsByName.get(name)));

            Habitat habitat = byName.get(seed.habitatName());
            if (habitat != null) {
                // Reference manuelle + copie denormalisee du nom.
                turtle.linkTo(habitat);
            }
            turtles.save(turtle);

            // La collection jumelle, liee par @DBRef, pour la demo du chapitre 2.
            dbRefTurtles.save(new TurtleWithDbRef(seed.name(), seed.species(), habitat));
        }
    }
}
