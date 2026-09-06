package com.formation.turtles.mongo.model;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Observation de terrain, embedded dans le document tortue.
 *
 * Pas de @Id, pas de cle etrangere, pas de table : ce n'est qu'un element
 * du tableau observations. La contrepartie : on ne peut pas interroger une
 * observation "toute seule" -- on part toujours de la tortue.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Observation {
    private LocalDate date;
    private String site;
    private String observer;
    private Integer healthScore;
}
