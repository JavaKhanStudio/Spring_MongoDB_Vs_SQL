package com.formation.turtles.common;

/**
 * Vue allegee : juste de quoi identifier une tortue.
 *
 * Utilisee par les endpoints de la relation N-N, pour que le nombre de requetes
 * mesure la traversee de la relation et rien d'autre. La vue complete
 * TurtleView, elle, declenche cote JPA le chargement de toutes les collections
 * en lazy loading -- c'est le sujet du chapitre 2, pas de celui-ci.
 */
public record TurtleBrief(String id, String name, String species) {
}
