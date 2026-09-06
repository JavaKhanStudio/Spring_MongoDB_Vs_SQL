package com.formation.turtles.trace;

import java.util.List;

/**
 * Enveloppe commune a tous les endpoints du projet.
 *
 * Les deux mondes repondent avec la meme forme, ce qui permet de poser les deux
 * reponses cote a cote dans Bruno : meme question, meme enveloppe ; seuls
 * queries et le nombre d'allers-retours changent.
 *
 * @param demo      ce que l'appel illustre
 * @param world     "SQL (JPA + H2)" ou "MongoDB (Spring Data)"
 * @param layer     par ou est passe l'appel (repository derive, template, pipeline...)
 * @param roundTrips nombre de requetes reellement envoyees a la base
 * @param queries   les requetes elles-memes, dans l'ordre
 * @param result    la donnee demandee
 */
public record TracedResponse<T>(String demo,
                                String world,
                                String layer,
                                int roundTrips,
                                List<QueryTrace> queries,
                                T result) {

    public static <T> TracedResponse<T> sql(String demo, String layer, T result) {
        return build(demo, "SQL (JPA + H2)", layer, result);
    }

    public static <T> TracedResponse<T> mongo(String demo, String layer, T result) {
        return build(demo, "MongoDB (Spring Data)", layer, result);
    }

    private static <T> TracedResponse<T> build(String demo, String world, String layer, T result) {
        List<QueryTrace> queries = QueryTraces.captured();
        return new TracedResponse<>(demo, world, layer, queries.size(), queries, result);
    }
}
