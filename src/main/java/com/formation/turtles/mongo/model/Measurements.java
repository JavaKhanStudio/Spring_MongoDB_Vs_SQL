package com.formation.turtles.mongo.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Sous-document embedded.
 *
 * Comparez avec com.formation.turtles.sql.model.Measurements :
 * aucune annotation ici. Spring Data Mongo n'a pas besoin qu'on lui declare
 * un @Embeddable : tout objet devient naturellement un sous-document BSON.
 *
 * { "name": "Crush", "measurements": { "shellLengthCm": 98.5, "weightKg": 132.0 } }
 */
@Getter
@NoArgsConstructor
public class Measurements {
    private Double shellLengthCm;
    /** Le seul champ mutable du modele : il sert a la demo « save() reecrit tout ». */
    @Setter
    private Double weightKg;

    public Measurements(Double shellLengthCm, Double weightKg) {
        this.shellLengthCm = shellLengthCm;
        this.weightKg = weightKg;
    }
}
