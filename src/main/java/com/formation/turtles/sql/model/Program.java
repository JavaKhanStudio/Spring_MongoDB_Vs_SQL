package com.formation.turtles.sql.model;

import java.util.LinkedHashSet;
import java.util.Set;

import com.formation.turtles.common.ProgramStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Un programme de recherche : « Argos Ocean Indien », « Plan national tortue luth »...
 *
 * Contrairement a un tag, un programme est une ENTITE a part entiere : quinze champs,
 * un protocole de terrain, un budget, un responsable, un cycle de vie. Il existe meme
 * si aucune tortue n'y est inscrite, et on veut pouvoir le renommer d'un seul UPDATE.
 * C'est ce qui justifie ici un vrai @ManyToMany, la ou les tags se contentent d'une
 * collection de valeurs.
 *
 * Cette richesse est aussi ce qui rend le choix de denormalisation lisible cote
 * document : le tableau programs d'une tortue ne recopie que name et organisation --
 * DEUX champs sur QUINZE. On copie ce qu'on affiche, pas l'entite.
 */
@Entity
@Table(name = "program")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String acronym;

    @Column(nullable = false)
    private String organisation;

    /** Le responsable scientifique : lu sur la fiche du programme, jamais sur celle d'une tortue. */
    @Column(name = "principal_investigator")
    private String principalInvestigator;

    @Column(name = "contact_email")
    private String contactEmail;

    private String website;

    /** Texte long : typiquement ce qu'on ne veut surtout PAS recopier dans chaque tortue. */
    @Column(length = 1000)
    private String description;

    @Column(name = "funding_euros")
    private Long fundingEuros;

    @Column(name = "start_year", nullable = false)
    private Integer startYear;

    @Column(name = "end_year")
    private Integer endYear;

    /** EnumType.STRING : sans lui, Hibernate stocke la position dans l'enum. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgramStatus status;

    @Column(name = "focus_species")
    private String focusSpecies;

    @Column(name = "coordination_country")
    private String coordinationCountry;

    /** Aplati dans la table program : quatre colonnes de plus, pas de table. */
    @Embedded
    private Protocol protocol;

    /**
     * Cote inverse de la relation : mappedBy dit que c'est Turtle.getPrograms() qui
     * possede la table de jointure. Sans lui, JPA creerait DEUX tables de jointure
     * pour la meme relation.
     */
    @ManyToMany(mappedBy = "programs")
    private Set<Turtle> turtles = new LinkedHashSet<>();

    public Program(String name, String acronym, String organisation, String principalInvestigator,
                   String contactEmail, String website, String description, Long fundingEuros,
                   Integer startYear, Integer endYear, ProgramStatus status, String focusSpecies,
                   String coordinationCountry, Protocol protocol) {
        this.name = name;
        this.acronym = acronym;
        this.organisation = organisation;
        this.principalInvestigator = principalInvestigator;
        this.contactEmail = contactEmail;
        this.website = website;
        this.description = description;
        this.fundingEuros = fundingEuros;
        this.startYear = startYear;
        this.endYear = endYear;
        this.status = status;
        this.focusSpecies = focusSpecies;
        this.coordinationCountry = coordinationCountry;
        this.protocol = protocol;
    }
}
