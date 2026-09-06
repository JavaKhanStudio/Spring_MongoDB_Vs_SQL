package com.formation.turtles.common;

/**
 * « Combien de tortues par programme ? » — la meme question posee au GROUP BY sur la
 * table de jointure, et au pipeline d'agregation.
 */
public record CountByLabel(String label, Long count) {
}
