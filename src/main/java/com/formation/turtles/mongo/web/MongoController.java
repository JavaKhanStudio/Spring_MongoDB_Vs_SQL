package com.formation.turtles.mongo.web;

import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.formation.turtles.common.SpeciesStat;
import com.formation.turtles.common.TurtleView;
import com.formation.turtles.mongo.model.Habitat;
import com.formation.turtles.mongo.model.Turtle;
import com.formation.turtles.mongo.model.TurtleWithDbRef;
import com.formation.turtles.mongo.projection.TurtleCard;
import com.formation.turtles.mongo.projection.TurtleLabel;
import com.formation.turtles.mongo.projection.TurtleNameAndSpecies;
import com.formation.turtles.mongo.repo.HabitatMongoRepository;
import com.formation.turtles.mongo.repo.TurtleDbRefRepository;
import com.formation.turtles.mongo.repo.TurtleMongoRepository;
import com.formation.turtles.mongo.service.TurtleTemplateService;
import com.formation.turtles.trace.TracedResponse;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Le monde document. Chaque endpoint repond a la meme question que son jumeau
 * /api/sql, et affiche la commande BSON reellement envoyee au serveur.
 */
@RestController
@RequestMapping("/api/mongo")
@RequiredArgsConstructor
public class MongoController {
    private final TurtleMongoRepository turtles;
    private final HabitatMongoRepository habitats;
    private final TurtleDbRefRepository dbRefTurtles;
    private final TurtleTemplateService templateService;

    // =====================================================================
    // CHAPITRE 1 -- Concepts : collection, document, BSON, ObjectId
    // =====================================================================

    /** Le document BRUT, sans mapping : voila ce qui est vraiment stocke. */
    @GetMapping("/concepts/document/{id}")
    public ResponseEntity<TracedResponse<Document>> rawDocument(@PathVariable String id) {
        Document document = templateService.rawDocument(id);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(TracedResponse.mongo(
                "Le document tel quel : _id en ObjectId, tableaux et sous-documents inclus, "
                        + "et birthYear stocké sous le nom 'annee_naissance' (@Field).",
                "MongoTemplate.findById(..., Document.class)", document));
    }

    /**
     * Anatomie d'un ObjectId : 12 octets = 4 (horodatage) + 5 (aleatoire propre au
     * processus) + 3 (compteur). D'ou deux consequences a montrer aux etudiants :
     * un ObjectId porte sa date de creation, et il est generable par le client
     * sans aller-retour avec le serveur (contrairement a un auto-increment SQL).
     */
    @GetMapping("/concepts/objectid/{id}")
    public TracedResponse<Map<String, Object>> objectIdAnatomy(@PathVariable String id) {
        ObjectId objectId = new ObjectId(id);
        Map<String, Object> anatomy = new LinkedHashMap<>();
        anatomy.put("hexadecimal", objectId.toHexString());
        anatomy.put("octets", 12);
        anatomy.put("horodatage (4 octets)", objectId.getDate().toInstant().atZone(ZoneId.systemDefault()).toString());
        anatomy.put("aléatoire + compteur (8 octets)", objectId.toHexString().substring(8));
        anatomy.put("généré par", "le pilote, côté client — aucun aller-retour, contrairement à un AUTO_INCREMENT");
        anatomy.put("équivalent SQL", "BIGINT AUTO_INCREMENT (généré par le serveur, unique dans UNE table)");
        return TracedResponse.mongo("Anatomie de la clé primaire de MongoDB.", "org.bson.types.ObjectId", anatomy);
    }

    /** Deux documents de la meme collection peuvent ne pas avoir les memes champs. */
    @GetMapping("/concepts/sample")
    public TracedResponse<List<Document>> sample(@RequestParam(defaultValue = "3") int limit) {
        return TracedResponse.mongo(
                "Une collection n'impose aucun schéma : c'est l'application (et éventuellement "
                        + "un validateur JSON Schema) qui garantit la cohérence.",
                "MongoTemplate.find(..., Document.class)", templateService.rawSample(limit));
    }

    /**
     * Les index de chaque collection, a comparer avec /api/sql/concepts/indexes.
     *
     * Point cle : un index pose sur un tableau (tags, programIds)
     * devient automatiquement un index multikey -- MongoDB indexe chaque element
     * separement. C'est ce qui rend « toutes les tortues baguees Argos » aussi rapide
     * qu'une recherche sur un champ scalaire, sans aucune table annexe.
     */
    @GetMapping("/concepts/indexes")
    public TracedResponse<Map<String, List<String>>> indexes() {
        Map<String, List<String>> byCollection = new LinkedHashMap<>();
        for (String collection : List.of("turtles", "habitats", "programs")) {
            byCollection.put(collection, templateService.indexesOf(collection));
        }
        return TracedResponse.mongo(
                "Les index déclarés par @Indexed et créés au démarrage. Ceux qui portent sur "
                        + "un tableau (tags, programIds) sont automatiquement MULTIKEYS : chaque "
                        + "élément est indexé séparément.",
                "MongoTemplate.indexOps().getIndexInfo()", byCollection);
    }

    // =====================================================================
    // CHAPITRE 2 -- Mapping : embedding vs reference
    // =====================================================================

    @GetMapping("/turtles")
    public TracedResponse<List<TurtleView>> all(@RequestParam(defaultValue = "20") int limit) {
        List<Turtle> found = turtles.findAllBy(PageRequest.of(0, limit));
        return TracedResponse.mongo(
                "La même page en UN aller-retour, quel que soit ?limit : observations et tags sont "
                        + "dans le document, et le nom de l'habitat y est dénormalisé.",
                "MongoRepository.findAll(Pageable)", MongoViews.of(found));
    }

    /** STRATEGIE 1 : EMBEDDING -- tout est dans le document, une seule requete. */
    @GetMapping("/links/embedded/{id}")
    public ResponseEntity<TracedResponse<TurtleView>> embedded(@PathVariable String id) {
        return turtles.findById(id)
                .map(MongoViews::of)
                .map(view -> TracedResponse.mongo(
                        "Embedding : mensurations, tags et observations arrivent avec la tortue. "
                                + "1 requête, 0 jointure.",
                        "MongoRepository.findById()", view))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** STRATEGIE 2 : MANUAL REFERENCE -- on decide, nous, d'aller chercher l'habitat. */
    @GetMapping("/links/manual/{id}")
    public ResponseEntity<TracedResponse<TurtleView>> manualReference(@PathVariable String id) {
        return turtles.findById(id)
                .map(turtle -> {
                    Habitat habitat = turtle.getHabitatId() == null
                            ? null
                            : habitats.findById(turtle.getHabitatId()).orElse(null);
                    return TracedResponse.mongo(
                            "Manual reference : 2 requêtes, mais c'est NOUS qui décidons de la seconde. "
                                    + "Et le champ reste utilisable par un $lookup côté serveur.",
                            "MongoRepository x2 (habitatId)", MongoViews.of(turtle, habitat));
                })
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** STRATEGIE 3 : @DBRef -- la resolution est automatique... et invisible. */
    @GetMapping("/links/dbref")
    public TracedResponse<List<TurtleWithDbRef>> dbRef(@RequestParam(defaultValue = "20") int limit) {
        List<TurtleWithDbRef> found = dbRefTurtles.findAllBy(PageRequest.of(0, limit));
        return TracedResponse.mongo(
                "@DBRef sur une page : comptez les requêtes — une par document, plus la première. "
                        + "Doublez ?limit, le compteur double. Spring Data résout chaque "
                        + "référence à la lecture — le problème 1+N, importé dans le monde document. "
                        + "À éviter : préférez la manual reference ou l'embedding.",
                "MongoRepository + @DBRef", found);
    }

    // =====================================================================
    // CHAPITRE 4 -- Requetes derivees, @Query, projections
    // =====================================================================

    @GetMapping("/turtles/species/{species}")
    public TracedResponse<List<TurtleView>> bySpecies(@PathVariable String species) {
        return TracedResponse.mongo(
                "Derived query : findBySpecies(...) — même signature que côté JPA, "
                        + "traduite ici en filtre BSON.",
                "MongoRepository (derived query)", MongoViews.of(turtles.findBySpecies(species)));
    }

    @GetMapping("/turtles/longer-than/{cm}")
    public TracedResponse<List<TurtleView>> longerThan(@PathVariable Double cm) {
        return TracedResponse.mongo(
                "Derived query traversant le sous-document : le point Java devient le point BSON "
                        + "('measurements.shellLengthCm').",
                "MongoRepository (derived query)",
                MongoViews.of(turtles
                        .findByMeasurementsShellLengthCmGreaterThanOrderByMeasurementsShellLengthCmDesc(cm)));
    }

    @GetMapping("/turtles/seen-at")
    public TracedResponse<List<TurtleView>> seenAt(@RequestParam List<String> sites) {
        return TracedResponse.mongo(
                "Derived query DANS un tableau de sous-documents : 'observations.site' $in [...]. "
                        + "Aucune jointure — c'est le gros avantage de l'embedding.",
                "MongoRepository (derived query)", MongoViews.of(turtles.findByObservationsSiteIn(sites)));
    }

    @GetMapping("/turtles/tagged/{tag}")
    public TracedResponse<List<TurtleView>> tagged(@PathVariable String tag) {
        return TracedResponse.mongo(
                "Sur un tableau, l'égalité signifie « contient » : { tags: 'balise-argos' }.",
                "MongoRepository (derived query)", MongoViews.of(turtles.findByTags(tag)));
    }

    @GetMapping("/queries/heavy")
    public TracedResponse<List<TurtleView>> heavy(@RequestParam String species, @RequestParam double minWeight) {
        return TracedResponse.mongo(
                "@Query en JSON brut : on écrit le filtre BSON à la main, ?0 et ?1 sont les paramètres.",
                "@Query", MongoViews.of(turtles.heavyOnesOfSpecies(species, minWeight)));
    }

    @GetMapping("/queries/born-before/{year}")
    public TracedResponse<List<TurtleView>> bornBefore(@PathVariable int year) {
        return TracedResponse.mongo(
                "PIÈGE : dans un @Query on écrit le nom STOCKÉ ('annee_naissance'), pas le nom Java "
                        + "('birthYear'). La derived query, elle, parle Java.",
                "@Query", MongoViews.of(turtles.bornBefore(year)));
    }

    @GetMapping("/queries/tagged/{tag}")
    public TracedResponse<List<Turtle>> taggedProjected(@PathVariable String tag) {
        return TracedResponse.mongo(
                "@Query + fields : la projection est faite PAR LE SERVEUR. Les champs absents "
                        + "reviennent à null — ici, tout sauf name et species.",
                "@Query (fields)", turtles.taggedWith(tag));
    }

    @GetMapping("/queries/count-heavier/{kg}")
    public TracedResponse<Long> countHeavier(@PathVariable double kg) {
        return TracedResponse.mongo(
                "@Query(count = true) : le comptage se fait côté serveur, rien ne remonte.",
                "@Query (count)", turtles.countHeavierThan(kg));
    }

    @GetMapping("/projections/closed")
    public TracedResponse<List<TurtleNameAndSpecies>> closedProjection() {
        return TracedResponse.mongo(
                "Closed projection : regardez la commande — Spring Data a ajouté "
                        + "{ name: 1, species: 1 }. Seuls ces champs traversent le réseau.",
                "Projection par interface", turtles.findAllByOrderByName());
    }

    @GetMapping("/projections/open")
    public TracedResponse<List<TurtleLabel>> openProjection(@RequestParam(defaultValue = "F") String sex) {
        return TracedResponse.mongo(
                "Open projection (@Value SpEL) : la commande n'a PAS de projection — "
                        + "le document entier est lu, puis recomposé côté application.",
                "Projection par interface + SpEL", turtles.findBySexIgnoreCase(sex));
    }

    @GetMapping("/projections/dynamic")
    public TracedResponse<List<TurtleCard>> dynamicProjection(@RequestParam String habitat) {
        return TracedResponse.mongo(
                "Projection DYNAMIQUE en DTO : la même méthode de repository peut renvoyer "
                        + "un record, une interface ou l'entité complète, au choix de l'appelant.",
                "Projection dynamique (Class<T>)", turtles.findByHabitatName(habitat, TurtleCard.class));
    }

    // =====================================================================
    // CHAPITRE 5 -- Agregation via @Aggregation sur le repository
    // =====================================================================

    @GetMapping("/stats/species")
    public TracedResponse<List<SpeciesStat>> statsBySpecies() {
        return TracedResponse.mongo(
                "@Aggregation : le pipeline est attaché à la méthode du repository. "
                        + "Comparez avec le GROUP BY JPQL de /api/sql/stats/species.",
                "@Aggregation", turtles.statsBySpecies());
    }
}
