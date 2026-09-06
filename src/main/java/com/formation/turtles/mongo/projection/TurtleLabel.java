package com.formation.turtles.mongo.projection;

import org.springframework.beans.factory.annotation.Value;

/**
 * Projection ouverte : la valeur est calculee par une expression SpEL.
 *
 * Consequence importante a montrer aux etudiants : comme l'expression peut
 * toucher n'importe quel champ, Spring Data ne peut plus optimiser et charge le
 * document entier. Une open projection est confortable, pas economique.
 */
public interface TurtleLabel {
    @Value("#{target.name + ' (' + target.species + ')'}")
    String getLabel();

    @Value("#{target.measurements != null ? target.measurements.weightKg : null}")
    Double getWeightKg();
}
