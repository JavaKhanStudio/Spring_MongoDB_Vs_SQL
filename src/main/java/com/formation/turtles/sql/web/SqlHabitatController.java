package com.formation.turtles.sql.web;

import java.util.List;

import com.formation.turtles.common.HabitatReport;
import com.formation.turtles.common.TurtleBrief;
import com.formation.turtles.sql.model.Habitat;
import com.formation.turtles.sql.model.Turtle;
import com.formation.turtles.sql.repo.HabitatRepository;
import com.formation.turtles.sql.repo.TurtleRepository;
import com.formation.turtles.trace.TracedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La temperature de l'eau : le champ qu'on a choisi de NE PAS copier.
 *
 * L'habitat sert de contre-exemple a lui-meme. Son NOM est duplique dans chaque tortue
 * (habitatName) parce qu'il ne change presque jamais et qu'on l'affiche partout. Sa
 * TEMPERATURE ne l'est pas, parce qu'elle change tout le temps.
 *
 * La decision de denormaliser ne se prend donc pas entite par entite, mais CHAMP PAR
 * CHAMP -- et le critere est la frequence de mise a jour multipliee par le nombre de
 * copies a propager.
 */
@RestController
@RequestMapping("/api/sql/habitats")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SqlHabitatController {

    private final HabitatRepository habitats;
    private final TurtleRepository turtles;

    /** LECTURE : une seule requete ramene les tortues ET la temperature du moment. */
    @GetMapping("/{name}/turtles")
    public ResponseEntity<TracedResponse<HabitatReport>> turtlesOf(@PathVariable String name) {
        List<Turtle> found = turtles.findByHabitatNameWithHabitat(name);

        // L'habitat est deja la : le join fetch l'a ramene avec les tortues. Aller le
        // rechercher couterait une requete de plus -- exactement celle que le monde
        // document est oblige de faire.
        Habitat habitat = found.isEmpty()
                ? habitats.findByName(name).orElse(null)
                : found.get(0).getHabitat();
        if (habitat == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(TracedResponse.sql(
                "UNE requête suffit : le « join fetch » ramène la température du moment en "
                        + "même temps que les tortues. Rien n'est dupliqué, donc rien ne peut "
                        + "être périmé — mais il a fallu une jointure.",
                "@Query JPQL avec join fetch", report(habitat, found)));
    }

    /** ECRITURE : un UPDATE, une ligne -- et toutes les tortues voient la nouvelle valeur. */
    @PatchMapping("/{name}/temperature")
    @Transactional
    public TracedResponse<String> updateTemperature(@PathVariable String name,
                                                    @RequestParam double value) {
        int updated = habitats.updateTemperature(name, value);
        return TracedResponse.sql(
                "UN update, UNE ligne. La température n'existe qu'à un seul endroit : toutes "
                        + "les tortues de cet habitat voient la nouvelle valeur immédiatement, "
                        + "sans rien à propager.",
                "@Modifying @Query (JPQL)", updated + " ligne(s) mise(s) à jour");
    }

    private HabitatReport report(Habitat habitat, List<Turtle> found) {
        return new HabitatReport(habitat.getName(), habitat.getOcean(), habitat.getWaterTempC(),
                habitat.isProtectedArea(), found.size(),
                found.stream()
                        .map(t -> new TurtleBrief(String.valueOf(t.getId()), t.getName(), t.getSpecies()))
                        .toList());
    }
}
