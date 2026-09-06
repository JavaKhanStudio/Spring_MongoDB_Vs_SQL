package com.formation.turtles.sql.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Cote SQL, un objet "sans identite" ne devient pas une table : il est aplati dans
 * la table proprietaire via @Embeddable / @Embedded.
 *
 * C'est l'equivalent le plus direct de l'embedding MongoDB... a une nuance
 * pres, decisive : ici l'aplatissement est obligatoirement a un seul niveau
 * et un seul exemplaire. Un tableau de sous-documents, lui, n'a pas d'equivalent
 * naturel : il faut une table fille (voir Observation).
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // exige par JPA
@AllArgsConstructor
public class Measurements {
    @Column(name = "shell_length_cm")
    private Double shellLengthCm;

    @Column(name = "weight_kg")
    private Double weightKg;

}
