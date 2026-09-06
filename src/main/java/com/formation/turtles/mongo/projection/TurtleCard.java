package com.formation.turtles.mongo.projection;

/**
 * Projection en DTO : un record suffit. Mecanisme identique a la projection
 * fermee (seuls les champs listes sont demandes), avec en prime un type concret,
 * immuable, facile a serialiser.
 */
public record TurtleCard(String name, String species, String habitatName) {
}
