package com.formation.turtles.common;

/**
 * Statistiques par site d'observation.
 *
 * Cote SQL : un GROUP BY sur la table fille observation.
 * Cote MongoDB : il faut d'abord $unwind le tableau embedded pour
 * retrouver le meme "grain" -- une ligne par observation.
 */
public record SiteStat(String site, Long observations, Double avgHealthScore) {
}
