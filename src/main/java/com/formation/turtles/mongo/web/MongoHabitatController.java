package com.formation.turtles.mongo.web;

import java.util.List;

import com.formation.turtles.common.HabitatReport;
import com.formation.turtles.common.TurtleBrief;
import com.formation.turtles.mongo.repo.HabitatMongoRepository;
import com.formation.turtles.mongo.repo.TurtleMongoRepository;
import com.formation.turtles.mongo.service.TurtleTemplateService;
import com.formation.turtles.trace.TracedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Le meme habitat, cote document -- et la demonstration que la denormalisation se
 * decide CHAMP PAR CHAMP.
 *
 * Dans chaque tortue on a copie habitatName, pas waterTempC. Consequence, dans les
 * deux sens :
 *
 *   - a l'ECRITURE, changer la temperature ne touche qu'un document. Renommer
 *     l'habitat, lui, obligerait a reecrire toutes les tortues concernees.
 *   - a la LECTURE, afficher la temperature demande une requete de plus, la ou le nom
 *     etait deja la.
 *
 * On paie a la lecture ce qu'on a refuse de payer a l'ecriture. C'est le meme
 * arbitrage que pour les programmes, mais pris dans l'autre sens.
 */
@RestController
@RequestMapping("/api/mongo/habitats")
@RequiredArgsConstructor
public class MongoHabitatController {

    private final HabitatMongoRepository habitats;
    private final TurtleMongoRepository turtles;
    private final TurtleTemplateService templateService;

    /** LECTURE : deux requetes, parce que la temperature n'a pas ete copiee. */
    @GetMapping("/{name}/turtles")
    public ResponseEntity<TracedResponse<HabitatReport>> turtlesOf(@PathVariable String name) {
        return habitats.findByName(name)
                .map(habitat -> {
                    List<TurtleBrief> found = turtles.findByHabitatName(name, TurtleBrief.class);
                    HabitatReport report = new HabitatReport(habitat.getName(), habitat.getOcean(),
                            habitat.getWaterTempC(), habitat.isProtectedArea(), found.size(), found);
                    return TracedResponse.mongo(
                            "DEUX requêtes : les tortues portent le NOM de l'habitat (copie "
                                    + "dénormalisée), mais pas sa TEMPÉRATURE — il faut donc aller la "
                                    + "chercher. C'est la contrepartie exacte du gain montré ailleurs : "
                                    + "ce qu'on n'a pas copié, il faut le lire.",
                            "MongoRepository × 2 (habitat, puis tortues)", report);
                })
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** ECRITURE : un $set, un document, zero tortue touchee. */
    @PatchMapping("/{name}/temperature")
    public TracedResponse<String> updateTemperature(@PathVariable String name,
                                                    @RequestParam double value) {
        long modified = templateService.updateHabitatTemperature(name, value);
        return TracedResponse.mongo(
                "UN $set, UN document, ZÉRO tortue touchée — précisément parce que la "
                        + "température n'a PAS été copiée. Comparez avec le renommage d'un "
                        + "organisme (chapitre 6) : le nom, lui, est dupliqué, donc le changer "
                        + "oblige à propager. Même entité, deux champs, deux traitements.",
                "MongoTemplate.updateFirst($set) sur habitats",
                modified + " document modifié — aucune tortue à propager");
    }
}
