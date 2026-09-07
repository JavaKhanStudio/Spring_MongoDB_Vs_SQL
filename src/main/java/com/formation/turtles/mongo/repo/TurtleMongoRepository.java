package com.formation.turtles.mongo.repo;

import java.util.List;
import java.util.Optional;

import com.formation.turtles.common.SpeciesStat;
import com.formation.turtles.mongo.model.ProgramRef;
import com.formation.turtles.mongo.model.Turtle;
import org.springframework.data.domain.Pageable;
import com.formation.turtles.mongo.projection.TurtleCard;
import com.formation.turtles.mongo.projection.TurtleLabel;
import com.formation.turtles.mongo.projection.TurtleNameAndSpecies;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

/**
 * Le repository MongoDB. A lire cote a cote avec
 * com.formation.turtles.sql.repo.TurtleRepository : les signatures des
 * derived queries sont identiques, seule la traduction differe.
 */
public interface TurtleMongoRepository extends MongoRepository<Turtle, String> {
    /** Une page, sans le comptage : renvoyer une List evite la commande « count ». */
    List<Turtle> findAllBy(Pageable pageable);

    // =====================================================================
    // 1. DERIVED QUERIES -- le nom de la methode EST la requete
    // =====================================================================

    /** -> { "species": "Chelonia mydas" } */
    List<Turtle> findBySpecies(String species);

    /**
     * Navigation dans un sous-document : le point du nom de methode devient le point
     * du chemin BSON.
     * -> { "measurements.shellLengthCm": { "$gt": 90 } }, tri decroissant
     */
    List<Turtle> findByMeasurementsShellLengthCmGreaterThanOrderByMeasurementsShellLengthCmDesc(Double minLength);

    /** Cherche dans un TABLEAU de sous-documents : -> { "observations.site": { "$in": [...] } } */
    List<Turtle> findByObservationsSiteIn(List<String> sites);

    /** -> { "tags": "balise-argos" } : sur un tableau, l'egalite signifie "contient". */
    List<Turtle> findByTags(String tag);

    /**
     * Traversee de la relation N-N : { "programs.programId": ObjectId(...) }.
     *
     * Une seule requete, servie par l'index multikey -- la ou le monde relationnel
     * enchaine deux jointures. Le prix a payer est ailleurs : c'est la tortue qui porte la
     * relation, donc c'est elle qu'il faut modifier pour inscrire ou desinscrire.
     */
    List<Turtle> findByProgramsProgramId(String programId);

    /**
     * « Les tortues du programme X », par le nom duplique :
     * { "programs.name": "Argos Océan Indien" }.
     *
     * Une requete la ou la reference seule en demanderait deux (resoudre le nom, puis
     * filtrer). Meme signature que cote JPA, ou c'est la jointure qui rend le service.
     */
    List<Turtle> findByProgramsName(String name);

    /**
     * « Toutes les tortues du CNRS » : { "programs.organisation": "CNRS" }.
     *
     * Une requete, aucune jointure, aucun $lookup -- uniquement parce
     * que l'organisme a ete duplique dans le document (voir ProgramRef).
     * Avec des identifiants nus, il faudrait d'abord chercher les programmes du CNRS.
     *
     * C'est le cas d'ecole de la denormalisation : on paie a l'ecriture (propager les
     * copies) ce qu'on economise a la lecture.
     */
    List<Turtle> findByProgramsOrganisation(String organisation);

    /** -> { "name": { "$regex": "...", "$options": "i" } } */
    List<Turtle> findByNameContainingIgnoreCase(String fragment);

    /**
     * Le champ Java s'appelle birthYear, il est stocke sous "annee_naissance"
     * (@Field). La derived query, elle, parle Java : c'est Spring Data qui traduit.
     */
    List<Turtle> findByBirthYearLessThan(Integer year);

    long countBySpecies(String species);

    boolean existsByName(String name);

    Optional<Turtle> findByName(String name);

    // =====================================================================
    // 2. @Query -- le filtre BSON ecrit a la main
    // =====================================================================

    /** ?0, ?1... sont les parametres de la methode, dans l'ordre. */
    @Query("{ 'species': ?0, 'measurements.weightKg': { $gte: ?1 } }")
    List<Turtle> heavyOnesOfSpecies(String species, double minWeight);

    /**
     * Spring traduit birthYear en annee_naissance, y compris dans un @Query.
     * Le nom stocke ne devient obligatoire que hors mapping : shell, Compass,
     * pipeline lance sur un nom de collection.
     */
    @Query("{ 'annee_naissance': { $lt: ?0 } }")
    List<Turtle> bornBefore(int year);

    /**
     * fields = la projection cote serveur : MongoDB ne renvoie que ces
     * champs. Moins d'octets sur le reseau, moins de travail pour le mapper.
     */
    @Query(value = "{ 'tags': ?0 }", fields = "{ 'name': 1, 'species': 1, '_id': 0 }")
    List<Turtle> taggedWith(String tag);

    /** Requetes de comptage / suppression, toujours en JSON brut. */
    @Query(value = "{ 'measurements.weightKg': { $gte: ?0 } }", count = true)
    long countHeavierThan(double minWeight);

    // =====================================================================
    // 3. PROJECTIONS -- ne remonter que ce dont on a besoin
    // =====================================================================

    /** Projection FERMEE (interface) : Spring Data en deduit le { name:1, species:1 }. */
    List<TurtleNameAndSpecies> findAllByOrderByName();

    /** Open projection (@Value SpEL) : le document entier est lu, puis recompose. */
    List<TurtleLabel> findBySexIgnoreCase(String sex);

    /**
     * Projection DYNAMIQUE : c'est l'appelant qui choisit la forme du resultat en
     * passant la classe cible (TurtleCard, TurtleNameAndSpecies,
     * Turtle.class...). Une methode, trois formes de reponse.
     */
    <T> List<T> findByHabitatName(String habitatName, Class<T> type);

    // =====================================================================
    // 4. @Aggregation -- un pipeline attache a une methode de repository
    // =====================================================================

    /**
     * Le meme resultat que TurtleRepository.statsBySpecies() en JPQL, mais
     * exprime en etapes successives. Le dernier $project sert a coller aux
     * noms du record SpeciesStat.
     */
    @Aggregation(pipeline = {
            "{ $group: { _id: '$species', count: { $sum: 1 }, avg: { $avg: '$measurements.shellLengthCm' }, max: { $max: '$measurements.weightKg' } } }",
            "{ $sort: { count: -1 } }",
            "{ $project: { _id: 0, key: '$_id', count: 1, avgShellLengthCm: '$avg', maxWeightKg: '$max' } }"
    })
    List<SpeciesStat> statsBySpecies();

    @Query("{ 'species': ?0 }")
    List<Turtle> ofSpecies(String species);


}
