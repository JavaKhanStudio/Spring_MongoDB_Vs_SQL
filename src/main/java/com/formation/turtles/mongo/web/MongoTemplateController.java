package com.formation.turtles.mongo.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.formation.turtles.common.TurtleView;
import com.formation.turtles.mongo.model.Observation;
import com.formation.turtles.mongo.model.Turtle;
import com.formation.turtles.mongo.repo.TurtleMongoRepository;
import com.formation.turtles.mongo.service.TurtleTemplateService;
import com.formation.turtles.trace.TracedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CHAPITRE 3 -- MongoRepository vs MongoTemplate.
 *
 * Les endpoints vont par paires : la meme intention, faite au repository puis au
 * template. La difference se lit dans la commande envoyee au serveur.
 */
@RestController
@RequestMapping("/api/mongo/template")
@RequiredArgsConstructor
public class MongoTemplateController {
    private final TurtleTemplateService templateService;
    private final TurtleMongoRepository turtles;

    /** Recherche multicritere : impossible a exprimer en derived query. */
    @GetMapping("/search")
    public TracedResponse<List<TurtleView>> search(@RequestParam(required = false) String species,
                                                   @RequestParam(required = false) String sex,
                                                   @RequestParam(required = false) Double minWeight,
                                                   @RequestParam(required = false) String tag,
                                                   @RequestParam(required = false) Integer limit) {
        List<Turtle> found = templateService.search(species, sex, minWeight, tag, limit);
        return TracedResponse.mongo(
                "Critères assemblés à l'exécution. En derived queries il faudrait une méthode "
                        + "par combinaison de filtres.",
                "MongoTemplate + Criteria", MongoViews.of(found));
    }

    /** LE contre-exemple : save() reecrit tout le document. */
    @PatchMapping("/{id}/weight-by-save")
    public ResponseEntity<TracedResponse<String>> weightBySave(@PathVariable String id,
                                                               @RequestParam double weightKg) {
        return turtles.findById(id)
                .map(turtle -> {
                    // On modifie UN champ... et save() renvoie quand meme le document entier.
                    turtle.getMeasurements().setWeightKg(weightKg);
                    turtles.save(turtle);
                    return TracedResponse.mongo(
                            "Repository.save() : 1 lecture + 1 réécriture COMPLÈTE du document. "
                                    + "Regardez la commande : tous les champs y sont, y compris les "
                                    + "observations. Tout ce qu'un autre client aurait modifié entre-temps "
                                    + "est écrasé au passage.",
                            "MongoRepository.save()", "document réécrit intégralement");
                })
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** La bonne facon : n'envoyer que le changement. */
    @PatchMapping("/{id}/weight")
    public TracedResponse<Long> weight(@PathVariable String id, @RequestParam double weightKg) {
        long modified = templateService.updateWeight(id, weightKg);
        return TracedResponse.mongo(
                "MongoTemplate : un seul $set part sur le réseau, sans lecture préalable. "
                        + "Comparez la taille de la commande avec /weight-by-save.",
                "MongoTemplate.updateFirst($set)", modified);
    }

    @PatchMapping("/{id}/grow")
    public TracedResponse<Long> grow(@PathVariable String id, @RequestParam double deltaKg) {
        return TracedResponse.mongo(
                "$inc : incrément atomique côté serveur. Aucune lecture, donc aucune course entre clients.",
                "MongoTemplate.updateFirst($inc)", templateService.grow(id, deltaKg));
    }

    @PostMapping("/{id}/observations")
    public TracedResponse<Long> addObservation(@PathVariable String id,
                                               @RequestParam String site,
                                               @RequestParam(defaultValue = "8") int healthScore,
                                               @RequestParam(defaultValue = "Anonyme") String observer) {
        Observation observation = new Observation(LocalDate.now(), site, observer, healthScore);
        return TracedResponse.mongo(
                "$push : on ajoute une observation au tableau embedded sans charger les précédentes. "
                        + "L'équivalent SQL serait un INSERT dans la table fille.",
                "MongoTemplate.updateFirst($push)", templateService.addObservation(id, observation));
    }

    @PatchMapping("/tag")
    public TracedResponse<Long> tagSpecies(@RequestParam String species, @RequestParam String tag) {
        return TracedResponse.mongo(
                "$addToSet + updateMulti : toute une espèce taguée en UNE commande.",
                "MongoTemplate.updateMulti($addToSet)", templateService.tagSpecies(species, tag));
    }

    @PostMapping("/upsert")
    public TracedResponse<String> upsert(@RequestParam String name,
                                         @RequestParam(defaultValue = "Chelonia mydas") String species,
                                         @RequestParam(defaultValue = "10") double weightKg) {
        return TracedResponse.mongo(
                "Upsert : « crée-la si elle n'existe pas, mets-la à jour sinon », en un aller-retour "
                        + "atomique. Le repository ne sait pas faire.",
                "MongoTemplate.upsert()", templateService.upsertByName(name, species, weightKg));
    }

    @PatchMapping("/{id}/rename")
    public ResponseEntity<TracedResponse<TurtleView>> rename(@PathVariable String id, @RequestParam String newName) {
        return templateService.renameAndReturn(id, newName)
                .map(turtle -> TracedResponse.mongo(
                        "findAndModify : modifie ET renvoie le document dans la même opération atomique.",
                        "MongoTemplate.findAndModify(returnNew)", MongoViews.of(turtle)))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/projection")
    public TracedResponse<List<Map<String, Object>>> projection() {
        List<Turtle> found = templateService.namesOnly();
        List<Map<String, Object>> rows = found.stream()
                .map(t -> Map.<String, Object>of("name", t.getName(), "species", t.getSpecies()))
                .toList();
        return TracedResponse.mongo(
                "Projection au Template : query.fields().include(...). Même mécanisme que la "
                        + "closed projection, mais choisi à l'exécution.",
                "MongoTemplate + Query.fields()", rows);
    }
}
