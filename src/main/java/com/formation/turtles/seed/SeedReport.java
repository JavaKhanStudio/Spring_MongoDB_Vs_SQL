package com.formation.turtles.seed;

/**
 * Ce que contiennent les deux bases, cote a cote.
 *
 * Les memes chiffres des deux cotes : le jeu de donnees est unique (Dataset), seule
 * la facon de le ranger change.
 *
 * @param action "chargement" apres un POST, "etat" apres un GET
 * @param millis duree du chargement (0 pour un simple etat)
 * @param sql    ce que contient H2
 * @param mongo  ce que contient MongoDB
 * @param loaded false tant qu'un des deux mondes est vide
 */
public record SeedReport(String action, long millis, Counts sql, Counts mongo, boolean loaded) {

    /**
     * @param turtles  nombre de tortues
     * @param habitats nombre d'habitats
     * @param programs nombre de programmes
     */
    public record Counts(long turtles, long habitats, long programs) {
        boolean isEmpty() {
            return turtles == 0;
        }
    }

    static SeedReport of(String action, long millis, Counts sql, Counts mongo) {
        return new SeedReport(action, millis, sql, mongo, !sql.isEmpty() && !mongo.isEmpty());
    }
}
