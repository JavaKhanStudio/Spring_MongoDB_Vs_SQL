package com.formation.turtles.sql.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Un habitat : entite a part entiere, dans sa propre table, referencee par une
 * cle etrangere depuis turtle.
 *
 * Cote MongoDB, la meme information pourra etre embedded, referencee ou
 * dupliquee -- c'est le choix de modelisation le plus structurant du cours.
 */
@Entity
@Table(name = "habitat")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Habitat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String ocean;

    @Column(name = "water_temp_c")
    private Double waterTempC;

    @Column(name = "protected_area")
    private boolean protectedArea;
}
