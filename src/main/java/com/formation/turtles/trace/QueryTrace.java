package com.formation.turtles.trace;

/**
 * Une requete reellement partie vers la base, capturee a la volee.
 *
 * C'est la piece maitresse du support de cours : chaque endpoint REST renvoie,
 * a cote de son resultat, la liste des requetes que Spring Data a fabriquees pour
 * lui. On voit donc le flux etage par etage :
 *
 *   Controleur  ->  Repository / Template  ->  requete generee  ->  base
 *
 * @param layer    couche qui a declenche la requete (Hibernate, pilote Mongo)
 * @param engine   "MongoDB" ou "SQL (H2)"
 * @param query    la requete telle qu'envoyee au serveur (JSON BSON ou SQL)
 * @param millis   duree mesuree cote client, quand le moteur nous la donne
 */
public record QueryTrace(String layer, String engine, String query, Long millis) {

    public static QueryTrace sql(String query) {
        return new QueryTrace("Hibernate / JPA", "SQL (H2)", query, null);
    }

    public static QueryTrace mongo(String query, long millis) {
        return new QueryTrace("Pilote MongoDB", "MongoDB", query, millis);
    }
}
