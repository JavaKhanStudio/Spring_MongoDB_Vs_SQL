package com.formation.turtles.mongo.service;

import java.util.List;

import com.formation.turtles.common.CountByLabel;
import com.formation.turtles.common.SiteStat;
import com.formation.turtles.common.SpeciesStat;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.stereotype.Service;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.bucket;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.limit;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.lookup;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.unwind;
import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Le pipeline d'agregation : des documents entrent, traversent des etapes, des
 * documents sortent.
 *
 *   [documents] -> $match -> $unwind -> $group -> $sort -> $project -> [resultats]
 *
 * Chaque methode renvoie aussi le pipeline en JSON (explain) : les
 * etudiants voient l'API Java d'un cote, le pipeline reel de l'autre, et peuvent
 * copier-coller ce dernier dans mongosh ou Compass.
 */
@Service
@RequiredArgsConstructor
public class TurtleAggregationService {
    private final MongoTemplate mongoTemplate;

    /** Le resultat + le pipeline qui l'a produit. */
    public record PipelineResult<T>(String explain, List<T> rows) {
    }

    // ---------------------------------------------------------------------
    // 1. L'equivalent exact du GROUP BY : compter et moyenner par espece
    //    SQL : select species, count(*), avg(shell_length_cm) ... group by species
    // ---------------------------------------------------------------------
    public PipelineResult<SpeciesStat> statsBySpecies() {
        Aggregation aggregation = newAggregation(
                group("species")
                        .count().as("count")
                        .avg("measurements.shellLengthCm").as("avgShellLengthCm")
                        .max("measurements.weightKg").as("maxWeightKg"),
                sort(Sort.Direction.DESC, "count"),
                // $group met la cle de regroupement dans _id : on la renomme pour
                // coller au record SpeciesStat.
                project("count", "avgShellLengthCm", "maxWeightKg").and("_id").as("key").andExclude("_id"));

        return run(aggregation, SpeciesStat.class);
    }

    // ---------------------------------------------------------------------
    // 2. $unwind : deplier le tableau embedded.
    //    Une tortue avec 3 observations devient 3 documents, un par observation.
    //    C'est l'etape qui n'a pas d'equivalent en SQL (la table fille joue deja
    //    ce role), et celle qui deroute le plus au debut.
    // ---------------------------------------------------------------------
    public PipelineResult<SiteStat> topSites(int top) {
        Aggregation aggregation = newAggregation(
                unwind("observations"),
                group("observations.site")
                        .count().as("observations")
                        .avg("observations.healthScore").as("avgHealthScore"),
                sort(Sort.Direction.DESC, "observations"),
                limit(top),
                project("observations", "avgHealthScore").and("_id").as("site").andExclude("_id"));

        return run(aggregation, SiteStat.class);
    }

    // ---------------------------------------------------------------------
    // 3. $lookup : LA jointure de MongoDB, executee cote serveur.
    //    C'est ce que @DBRef ne sait pas faire. Elle fonctionne ici parce que
    //    turtles.habitatId est stocke en ObjectId (voir @Field(targetType=...)).
    // ---------------------------------------------------------------------
    public PipelineResult<Document> byHabitatWithLookup() {
        Aggregation aggregation = newAggregation(
                lookup("habitats", "habitatId", "_id", "habitat"),
                unwind("habitat"),
                group("habitat.name")
                        .count().as("turtles")
                        .avg("measurements.weightKg").as("avgWeightKg")
                        .first("habitat.ocean").as("ocean")
                        .first("habitat.waterTempC").as("waterTempC"),
                sort(Sort.Direction.DESC, "turtles"),
                project("turtles", "avgWeightKg", "ocean", "waterTempC").and("_id").as("habitat").andExclude("_id"));

        return run(aggregation, Document.class);
    }

    // ---------------------------------------------------------------------
    // 4. $match en premier : filtrer AVANT de grouper.
    //    Regle de perf n°1 du pipeline -- un $match place en tete peut utiliser
    //    les index, place en fin il travaille sur tout le flux.
    // ---------------------------------------------------------------------
    public PipelineResult<SpeciesStat> statsForProtectedOnly() {
        Aggregation aggregation = newAggregation(
                match(where("measurements.weightKg").gt(50)),
                group("species")
                        .count().as("count")
                        .avg("measurements.shellLengthCm").as("avgShellLengthCm")
                        .max("measurements.weightKg").as("maxWeightKg"),
                sort(Sort.Direction.DESC, "count"),
                project("count", "avgShellLengthCm", "maxWeightKg").and("_id").as("key").andExclude("_id"));

        return run(aggregation, SpeciesStat.class);
    }

    // ---------------------------------------------------------------------
    // 5. $bucket : histogramme. Aucun equivalent simple en SQL standard.
    // ---------------------------------------------------------------------
    public PipelineResult<Document> weightBuckets() {
        Aggregation aggregation = newAggregation(
                bucket("measurements.weightKg")
                        .withBoundaries(0, 50, 100, 200, 600)
                        .withDefaultBucket("hors bornes")
                        .andOutputCount().as("turtles")
                        .andOutput("name").push().as("names"));

        return run(aggregation, Document.class);
    }

    // ---------------------------------------------------------------------
    // 6. Le pipeline peut partir de N'IMPORTE QUELLE collection.
    //    Ici on part de « programs » : le $lookup cherche les tortues dont le
    //    TABLEAU programIds contient l'_id du programme. Un foreignField de type
    //    tableau se comporte comme un « contient » -- et les programmes sans
    //    tortue sont conserves, comme avec un LEFT JOIN.
    // ---------------------------------------------------------------------
    public PipelineResult<CountByLabel> turtlesPerProgram() {
        Aggregation aggregation = newAggregation(
                lookup("turtles", "_id", "programs.programId", "turtles"),
                project().and("name").as("label").and(ArrayOperators.Size.lengthOfArray("turtles")).as("count")
                        .andExclude("_id"),
                sort(Sort.Direction.DESC, "count"));

        return run(aggregation, "programs", CountByLabel.class);
    }

    private final <T> PipelineResult<T> run(Aggregation aggregation, Class<T> type) {
        return run(aggregation, "turtles", type);
    }

    private final <T> PipelineResult<T> run(Aggregation aggregation, String collection, Class<T> type) {
        AggregationResults<T> results = mongoTemplate.aggregate(aggregation, collection, type);
        return new PipelineResult<>(asJson(aggregation), results.getMappedResults());
    }

    /** Le pipeline tel qu'on pourrait le coller dans mongosh ou Compass. */
    private final String asJson(Aggregation aggregation) {
        return aggregation.toPipeline(Aggregation.DEFAULT_CONTEXT).stream()
                .map(Document::toJson)
                .collect(java.util.stream.Collectors.joining(",\n  ", "[\n  ", "\n]"));
    }
}
