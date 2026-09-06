package com.formation.turtles.common;

/**
 * Une ligne de statistique, commune au GROUP BY SQL et au $group
 * du pipeline d'agregation MongoDB.
 */
public record SpeciesStat(String key, Long count, Double avgShellLengthCm, Double maxWeightKg) {
}
