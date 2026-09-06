package com.formation.turtles.sql.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Une observation de terrain.
 *
 * Elle n'a aucun sens en dehors de sa tortue, et pourtant le modele relationnel
 * l'oblige a devenir une table, avec sa propre cle primaire et une cle
 * etrangere de retour. Lire une tortue et ses observations = une jointure (ou un
 * second SELECT).
 *
 * Cote MongoDB, ces memes observations vivront dans le document tortue :
 * un seul aller-retour, aucune jointure. C'est le coeur du chapitre "embedding
 * vs reference".
 */
@Entity
@Table(name = "observation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Observation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "observed_on", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String site;

    private String observer;

    @Column(name = "health_score")
    private Integer healthScore;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turtle_id")
    private Turtle turtle;

    public Observation(LocalDate date, String site, String observer, Integer healthScore) {
        this.date = date;
        this.site = site;
        this.observer = observer;
        this.healthScore = healthScore;
    }

    void attachTo(Turtle turtle) {
        this.turtle = turtle;
    }
}
