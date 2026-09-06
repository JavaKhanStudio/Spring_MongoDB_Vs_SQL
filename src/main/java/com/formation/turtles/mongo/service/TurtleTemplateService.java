package com.formation.turtles.mongo.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.formation.turtles.mongo.model.Habitat;
import com.formation.turtles.mongo.model.Observation;
import com.formation.turtles.mongo.model.Program;
import com.formation.turtles.mongo.model.ProgramRef;
import com.formation.turtles.mongo.model.Turtle;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Tout ce que le repository ne sait pas (bien) faire.
 *
 * Regle simple a donner aux etudiants :
 *
 *   - MongoRepository pour le CRUD et les requetes connues a la compilation ;
 *   - MongoTemplate des qu'il faut composer la requete a l'execution, ne
 *     modifier qu'un champ, faire un upsert, un findAndModify ou du bulk.
 *
 * Les deux partagent le meme convertisseur et la meme connexion : on peut les
 * melanger dans le meme service sans arriere-pensee.
 */
@Service
@RequiredArgsConstructor
public class TurtleTemplateService {
    private final MongoTemplate mongoTemplate;

    // =====================================================================
    // 1. RECHERCHE MULTICRITERE DYNAMIQUE
    //    Impossible avec une derived query : il faudrait une methode par
    //    combinaison de criteres (2^n methodes...).
    // =====================================================================

    public List<Turtle> search(String species, String sex, Double minWeight, String tag, Integer limit) {
        Criteria criteria = new Criteria();

        if (StringUtils.hasText(species)) {
            criteria = criteria.and("species").is(species);
        }
        if (StringUtils.hasText(sex)) {
            criteria = criteria.and("sex").is(sex);
        }
        if (minWeight != null) {
            criteria = criteria.and("measurements.weightKg").gte(minWeight);
        }
        if (StringUtils.hasText(tag)) {
            criteria = criteria.and("tags").is(tag);
        }

        Query query = Query.query(criteria);
        if (limit != null && limit > 0) {
            query.limit(limit);
        }
        return mongoTemplate.find(query, Turtle.class);
    }

    // =====================================================================
    // 2. MISE A JOUR PARTIELLE
    //    repository.save(tortue) reecrit TOUT le document (et ecrase au passage
    //    ce qu'un autre client aurait modifie entre-temps).
    //    Ici, seul le champ vise part sur le reseau : { $set: { ... } }
    // =====================================================================

    public long updateWeight(String id, double weightKg) {
        UpdateResult result = mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(id)),
                new Update().set("measurements.weightKg", weightKg),
                Turtle.class);
        return result.getModifiedCount();
    }

    /** $inc : increment atomique cote serveur, sans lire le document avant. */
    public long grow(String id, double deltaKg) {
        return mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(id)),
                new Update().inc("measurements.weightKg", deltaKg),
                Turtle.class).getModifiedCount();
    }

    /**
     * $push : ajoute une observation au tableau embedded sans jamais charger
     * les precedentes. Le pendant SQL serait un INSERT dans la table fille.
     */
    public long addObservation(String id, Observation observation) {
        return mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(id)),
                new Update().push("observations", observation),
                Turtle.class).getModifiedCount();
    }

    /**
     * Inscription a un programme : $addToSet sur le tableau de references.
     *
     * L'identifiant est converti en ObjectId pour correspondre au type stocke
     * (voir @Field(targetType = OBJECT_ID) sur le champ) -- sans quoi la reference
     * serait ecrite en chaine et le $lookup ne la retrouverait pas.
     */
    public long enrolInProgram(String turtleId, Program program) {
        return mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(turtleId)),
                new Update().addToSet("programs", ProgramRef.of(program)),
                Turtle.class).getModifiedCount();
    }

    /**
     * Changer la temperature d'un habitat : UN $set, UN document, ZERO tortue touchee.
     *
     * Et c'est exactement parce qu'on ne l'a PAS copiee dans les tortues. Comparez avec
     * renameOrganisation() juste en dessous : le nom, lui, est duplique, donc le
     * renommer oblige a propager. Meme entite, deux champs, deux traitements : la
     * decision de denormaliser se prend champ par champ, jamais entite par entite.
     */
    public long updateHabitatTemperature(String name, double value) {
        return mongoTemplate.updateFirst(
                Query.query(Criteria.where("name").is(name)),
                new Update().set("waterTempC", value),
                Habitat.class).getModifiedCount();
    }

    /**
     * LE prix de la denormalisation, en une methode.
     *
     * Renommer un organisme demande deux ecritures : la source de verite dans
     * programs, puis toutes les copies dans turtles. Entre les deux, la
     * base est incoherente -- et rien, dans le moteur, n'impose la seconde.
     *
     * $[element] + arrayFilters vise les seuls sous-documents concernes,
     * sans toucher aux autres programmes de la meme tortue.
     *
     * Cote relationnel, la meme operation est un UPDATE d'une seule ligne : l'organisme
     * n'est stocke qu'une fois.
     */
    public RenameResult renameOrganisation(String from, String to) {
        long programs = mongoTemplate.updateMulti(
                Query.query(Criteria.where("organisation").is(from)),
                new Update().set("organisation", to),
                Program.class).getModifiedCount();

        long turtles = mongoTemplate.updateMulti(
                Query.query(Criteria.where("programs.organisation").is(from)),
                new Update().set("programs.$[element].organisation", to)
                        .filterArray(Criteria.where("element.organisation").is(from)),
                Turtle.class).getModifiedCount();

        return new RenameResult(programs, turtles);
    }

    /**
     * Le resultat d'un renommage : combien de documents source ont ete modifies,
     * et combien de copies il a fallu propager.
     */
    public record RenameResult(long programs, long turtles) {
    }

    /** $addToSet sur TOUS les documents qui matchent : une seule requete. */
    public long tagSpecies(String species, String tag) {
        return mongoTemplate.updateMulti(
                Query.query(Criteria.where("species").is(species)),
                new Update().addToSet("tags", tag),
                Turtle.class).getModifiedCount();
    }

    // =====================================================================
    // 3. UPSERT et FIND-AND-MODIFY : deux operations atomiques que le
    //    repository n'expose pas.
    // =====================================================================

    /** Cree la tortue si elle n'existe pas, la met a jour sinon -- en un seul aller-retour. */
    public String upsertByName(String name, String species, double weightKg) {
        UpdateResult result = mongoTemplate.upsert(
                Query.query(Criteria.where("name").is(name)),
                new Update().set("species", species)
                        .set("measurements.weightKg", weightKg)
                        .setOnInsert("sex", "inconnu"),
                Turtle.class);
        return result.getUpsertedId() == null
                ? "document existant mis a jour"
                : "document cree : " + result.getUpsertedId();
    }

    /** Modifie et renvoie le document dans la meme operation atomique. */
    public Optional<Turtle> renameAndReturn(String id, String newName) {
        return Optional.ofNullable(mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(id)),
                new Update().set("name", newName),
                FindAndModifyOptions.options().returnNew(true),
                Turtle.class));
    }

    // =====================================================================
    // 4. PROJECTION AU TEMPLATE
    // =====================================================================

    public List<Turtle> namesOnly() {
        Query query = new Query();
        query.fields().include("name").include("species").exclude("_id");
        return mongoTemplate.find(query, Turtle.class);
    }

    // =====================================================================
    // 5. LE DOCUMENT BRUT
    //    Meme collection, mais on demande un org.bson.Document au lieu d'une
    //    Turtle : c'est le BSON tel qu'il est stocke, sans mapping. Ideal pour
    //    montrer _id / ObjectId / le champ renomme par @Field.
    // =====================================================================

    /** Les champs stockes en tableau : leur index est donc multikey. */
    private static final List<String> ARRAY_FIELDS = List.of("tags", "programs", "observations",
            "programs.programId", "programs.organisation");

    /** Les index d'une collection, en une ligne lisible chacun. */
    public List<String> indexesOf(String collection) {
        return mongoTemplate.indexOps(collection).getIndexInfo().stream()
                .map(index -> {
                    String keys = index.getIndexFields().stream()
                            .map(field -> field.getKey() + ": "
                                    + (field.getDirection() == Sort.Direction.DESC ? -1 : 1))
                            .collect(Collectors.joining(", ", "{ ", " }"));
                    boolean multikey = index.getIndexFields().stream()
                            .anyMatch(field -> ARRAY_FIELDS.contains(field.getKey()));
                    return index.getName() + " : " + keys
                            + (index.isUnique() ? "  — unique" : "")
                            + (multikey ? "  — MULTIKEY (le champ est un tableau)" : "");
                })
                .toList();
    }

    public Document rawDocument(String id) {
        return mongoTemplate.findById(id, Document.class, "turtles");
    }

    public List<Document> rawSample(int limit) {
        return mongoTemplate.find(new Query().limit(limit), Document.class, "turtles");
    }
}
