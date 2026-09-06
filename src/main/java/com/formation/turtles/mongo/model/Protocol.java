package com.formation.turtles.mongo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Le protocole de terrain, cote document : un sous-document, sans annotation.
 *
 * A comparer avec com.formation.turtles.sql.model.Protocol, qui a besoin
 * d'@Embeddable et d'un @Column par champ.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Protocol {

    private Integer samplingIntervalDays;
    private String taggingMethod;
    private Double minShellLengthCm;
    private boolean satelliteTracking;
}
