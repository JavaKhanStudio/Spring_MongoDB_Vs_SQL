package com.formation.turtles.mongo.projection;

/**
 * Projection fermee : toutes les methodes correspondent a des champs
 * existants. Spring Data en deduit la projection et demande a MongoDB
 * { "name": 1, "species": 1 } -- le reste du document ne quitte jamais
 * le serveur.
 */
public interface TurtleNameAndSpecies {
    String getName();

    String getSpecies();
}
