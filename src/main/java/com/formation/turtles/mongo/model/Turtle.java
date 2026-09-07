package com.formation.turtles.mongo.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

/**
 * La tortue, version document. Une classe, une collection, un document.
 *
 * Le meme metier que com.formation.turtles.sql.model.Turtle, qui lui
 * s'etale sur six tables. Ici tout ce qui appartient a la tortue vit avec elle :
 *
 * {
 *   "_id"        : ObjectId("6653f1c2a1b2c3d4e5f60718"),
 *   "name"       : "Crush",
 *   "species"    : "Chelonia mydas",
 *   "annee_naissance": 1998,                      <- renomme par @Field
 *   "tags"       : ["balise-argos", "adulte"],    <- tableau, pas de table
 *   "measurements": { "shellLengthCm": 98.5, ... },  <- sous-document
 *   "observations": [ { "site": "Tulear", ... }, ... ], <- tableau de sous-documents
 *   "habitatId"  : ObjectId("6653f1c2a1b2c3d4e5f60711"),  <- manual reference
 *   "programs"   : [ { "programId": ObjectId(...),  <- relation N-N, sans table de jointure
 *                      "name": "Argos Océan Indien", <- copies denormalisees, assumees
 *                      "organisation": "CNRS" } ]
 * }
 */
@Document(collection = "turtles")
@CompoundIndexes({
        // Index multikey sur un tableau de SOUS-DOCUMENTS : MongoDB indexe chaque
        // element separement, exactement comme pour un tableau de scalaires.
        @CompoundIndex(name = "programs_ref", def = "{ 'programs.programId': 1 }"),
        @CompoundIndex(name = "programs_org", def = "{ 'programs.organisation': 1 }")
})
@Getter
@NoArgsConstructor
public class Turtle {
    /**
     * L'annotation @Id de Spring Data (pas celui de JPA !) : le champ est stocke sous le
     * nom _id. Type String cote Java, ObjectId cote base : la conversion est
     * automatique. Si on laisse la valeur nulle, c'est le driver qui genere l'ObjectId.
     */
    @Id
    private String id;

    @Indexed
    private String name;

    @Indexed
    private String species;

    private String sex;

    /**
     * L'annotation @Field decouple le nom Java du nom stocke.
     */
    @Field("annee_naissance")
    private Integer birthYear;

    /**
     * Un tableau de strings : aucune table de collection, c'est un champ comme un autre.
     *
     * Un @Indexed sur un tableau cree un index multikey : MongoDB indexe
     * chaque element separement, si bien que { tags: "balise-argos" } devient une
     * recherche indexee. C'est le pendant exact de l'index pose sur la colonne
     * turtle_tag.tag cote SQL.
     */
    @Indexed
    private Set<String> tags = new LinkedHashSet<>();

    /** EMBEDDING 1 : objet unique, toujours lu avec sa tortue. */
    private Measurements measurements;

    /** EMBEDDING 2 : tableau de sous-documents, borne (quelques dizaines max). */
    private List<Observation> observations = new ArrayList<>();

    /**
     * MANUAL REFERENCE : on stocke seulement l'identifiant de l'habitat.
     * C'est l'alternative recommandee a @DBRef : on choisit explicitement de
     * faire une seconde requete, ou un $lookup dans un pipeline.
     *
     * targetType = OBJECT_ID est ici indispensable : sans lui, Spring Data
     * ecrirait la reference sous forme de chaine, et un $lookup sur
     * habitats._id (un ObjectId) ne trouverait jamais rien. Piege classique.
     */
    @Field(targetType = FieldType.OBJECT_ID)
    private String habitatId;

    /**
     * LA relation plusieurs-a-plusieurs, cote document.
     *
     * MongoDB n'a pas de table de jointure : il faut choisir un cote. Ici, c'est
     * la tortue qui porte ses programmes -- non pas sous forme d'identifiants nus,
     * mais d'extended references : l'identifiant, plus une copie du nom et de
     * l'organisme (voir ProgramRef).
     *
     * Consequences :
     *
     *   - « les programmes d'une tortue » : immediat, c'est dans le document ;
     *   - « les tortues d'un programme » : { "programs.programId": ObjectId(...) } ;
     *   - « les tortues du CNRS » : { "programs.organisation": "CNRS" } -- une
     *     requete, sans jointure, ce que des identifiants nus ne permettraient pas.
     *
     * Cote SQL, la table de jointure sert tous ces sens a egalite, au prix d'une jointure
     * de plus ; ici, on paie a l'ecriture ce qu'on economise a la lecture.
     */
    private List<ProgramRef> programs = new ArrayList<>();

    /**
     * DENORMALISATION : une copie du nom de l'habitat, pour afficher une liste sans
     * aucune jointure. Le prix a payer : il faut la mettre a jour si l'habitat est
     * renomme. C'est un choix assume, pas un oubli.
     */
    private String habitatName;

    /** @Transient : calcule a la lecture, jamais ecrit dans le document. */
    @Transient
    @Getter(AccessLevel.NONE)   // le getter est ecrit a la main : il calcule
    private Integer age;

    public Turtle(String name, String species, String sex, Integer birthYear, Measurements measurements) {
        this.name = name;
        this.species = species;
        this.sex = sex;
        this.birthYear = birthYear;
        this.measurements = measurements;
    }

    // Methodes de commodite : embedding, reference, denormalisation -- trois gestes
    // qu'aucun setter genere ne saurait exprimer.

    public void addObservation(Observation observation) {
        this.observations.add(observation);
    }

    public void addTag(String tag) {
        this.tags.add(tag);
    }

    public void enrolIn(Program program) {
        this.programs.add(ProgramRef.of(program));
    }

    /** Reference manuelle + copie denormalisee du nom, d'un seul geste. */
    public void linkTo(Habitat habitat) {
        this.habitatId = habitat.getId();
        this.habitatName = habitat.getName();
    }

    /** Calcule a la lecture : le champ age n'est jamais ecrit ni lu. */
    public Integer getAge() {
        return birthYear == null ? null : LocalDate.now().getYear() - birthYear;
    }
}
