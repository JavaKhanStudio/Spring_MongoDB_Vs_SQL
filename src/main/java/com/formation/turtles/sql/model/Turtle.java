package com.formation.turtles.sql.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * La tortue, version relationnelle.
 *
 * Une seule notion metier, mais six tables a l'arrivee :
 *
 *   turtle           (la tortue + ses mensurations aplaties)
 *   observation      (table fille, FK turtle_id)
 *   turtle_tag       (table de collection, FK turtle_id -- PAS une table de jointure)
 *   habitat          (table parente, FK habitat_id dans turtle)
 *   program          (entite partagee)
 *   turtle_program   (LA table de jointure : deux FK, relation N-N)
 *
 * A comparer avec com.formation.turtles.mongo.model.Turtle : une seule
 * collection, un seul document, aucune jointure.
 */
@Entity
@Table(name = "turtle")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Turtle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String species;

    private String sex;

    @Column(name = "birth_year")
    private Integer birthYear;

    /**
     * Attention a ne pas confondre avec une table de jointure : turtle_tag ne
     * porte qu'une cle etrangere, et sa colonne tag contient la valeur
     * elle-meme. C'est une collection de valeurs, pas une relation vers une entite --
     * l'equivalent exact du tableau tags du document MongoDB. Comparer avec
     * programs, qui est un vrai @ManyToMany.
     *
     * L'index sur tag n'est pas decoratif : sans lui, « toutes les tortues
     * baguees Argos » balaie toute la table. Son pendant MongoDB est l'index multikey
     * declare sur le tableau tags.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "turtle_tag",
            joinColumns = @JoinColumn(name = "turtle_id"),
            indexes = @Index(name = "idx_turtle_tag_tag", columnList = "tag"))
    @Column(name = "tag")
    private Set<String> tags = new LinkedHashSet<>();

    /**
     * LA relation plusieurs-a-plusieurs : une tortue suit plusieurs programmes, un
     * programme suit plusieurs tortues.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "turtle_program",
            joinColumns = @JoinColumn(name = "turtle_id"),
            inverseJoinColumns = @JoinColumn(name = "program_id"))
    private Set<Program> programs = new LinkedHashSet<>();

    @Embedded
    private Measurements measurements;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habitat_id")
    @Setter   // le seul champ qui se remplace tel quel ; tout le reste passe par une methode metier
    private Habitat habitat;

    @OneToMany(mappedBy = "turtle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Observation> observations = new ArrayList<>();

    public Turtle(String name, String species, String sex, Integer birthYear, Measurements measurements) {
        this.name = name;
        this.species = species;
        this.sex = sex;
        this.birthYear = birthYear;
        this.measurements = measurements;
    }

    // Methodes de commodite : elles gardent les deux cotes de la relation coherents,
    // ce qu'un simple setter genere ne saurait pas faire.

    public void addObservation(Observation observation) {
        observation.attachTo(this);
        this.observations.add(observation);
    }

    public void addTag(String tag) {
        this.tags.add(tag);
    }

    public void enrolIn(Program program) {
        this.programs.add(program);
        program.getTurtles().add(this);
    }
}
