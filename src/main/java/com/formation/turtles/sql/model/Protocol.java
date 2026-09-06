package com.formation.turtles.sql.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Le protocole de terrain d'un programme : materiel, frequence, criteres d'inclusion.
 *
 * Objet sans identite propre : aplati dans la table program, comme les mensurations
 * le sont dans turtle.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Protocol {

    @Column(name = "sampling_interval_days")
    private Integer samplingIntervalDays;

    @Column(name = "tagging_method")
    private String taggingMethod;

    @Column(name = "min_shell_length_cm")
    private Double minShellLengthCm;

    @Column(name = "satellite_tracking")
    private boolean satelliteTracking;
}
