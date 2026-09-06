package com.formation.turtles.mongo.web;

import java.util.Map;

import com.formation.turtles.common.SiteStat;
import com.formation.turtles.common.SpeciesStat;
import com.formation.turtles.mongo.service.TurtleAggregationService;
import com.formation.turtles.trace.TracedResponse;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CHAPITRE 5 -- Le pipeline d'agregation.
 *
 * Chaque reponse contient le pipeline (champ result.explain) et la
 * commande aggregate reelle (champ queries) : de quoi copier-coller
 * dans mongosh ou Compass pour experimenter.
 */
@RestController
@RequestMapping("/api/mongo/aggregation")
@RequiredArgsConstructor
public class MongoAggregationController {
    private final TurtleAggregationService aggregationService;

    @GetMapping("/species")
    public TracedResponse<TurtleAggregationService.PipelineResult<SpeciesStat>> species() {
        return TracedResponse.mongo(
                "$group + $sort + $project : l'équivalent exact du GROUP BY de /api/sql/stats/species, "
                        + "mais décomposé en étapes que l'on assemble soi-même.",
                "MongoTemplate.aggregate()", aggregationService.statsBySpecies());
    }

    @GetMapping("/sites")
    public TracedResponse<TurtleAggregationService.PipelineResult<SiteStat>> sites(
            @RequestParam(defaultValue = "5") int top) {
        return TracedResponse.mongo(
                "$unwind : chaque observation embedded redevient un document à part entière, "
                        + "puis on groupe dessus. C'est l'étape sans équivalent SQL — parce qu'en SQL "
                        + "la table fille joue déjà ce rôle.",
                "MongoTemplate.aggregate($unwind)", aggregationService.topSites(top));
    }

    @GetMapping("/habitats")
    public TracedResponse<TurtleAggregationService.PipelineResult<Document>> habitats() {
        return TracedResponse.mongo(
                "$lookup : LA jointure de MongoDB, exécutée côté serveur. Elle fonctionne parce que "
                        + "habitatId est stocké en ObjectId — et c'est précisément ce que @DBRef ne "
                        + "permet pas de faire.",
                "MongoTemplate.aggregate($lookup)", aggregationService.byHabitatWithLookup());
    }

    @GetMapping("/filtered")
    public TracedResponse<TurtleAggregationService.PipelineResult<SpeciesStat>> filtered() {
        return TracedResponse.mongo(
                "$match placé EN TÊTE du pipeline : il peut utiliser les index et réduit le flux "
                        + "avant le $group. Règle de performance n°1.",
                "MongoTemplate.aggregate($match d'abord)", aggregationService.statsForProtectedOnly());
    }

    @GetMapping("/buckets")
    public TracedResponse<TurtleAggregationService.PipelineResult<Document>> buckets() {
        return TracedResponse.mongo(
                "$bucket : un histogramme en une étape. À écrire en SQL, il faudrait un CASE WHEN "
                        + "imbriqué dans un GROUP BY.",
                "MongoTemplate.aggregate($bucket)", aggregationService.weightBuckets());
    }

    @GetMapping("/cheatsheet")
    public Map<String, String> cheatsheet() {
        return Map.of(
                "$match", "where       — filtrer des documents",
                "$project", "select    — choisir/renommer/calculer des champs",
                "$group", "group by    — agréger ($sum, $avg, $min, $max, $push, $first)",
                "$sort", "order by     — trier",
                "$limit / $skip", "limit / offset",
                "$unwind", "(pas d'équivalent) — déplier un tableau en N documents",
                "$lookup", "left join  — jointure côté serveur",
                "$bucket", "(CASE WHEN) — histogramme par intervalles",
                "$addFields / $set", "colonne calculée",
                "$out / $merge", "create table as select — écrire le résultat dans une collection");
    }
}
