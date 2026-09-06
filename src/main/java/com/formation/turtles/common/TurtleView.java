package com.formation.turtles.common;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * La vue JSON d'une tortue, commune aux deux mondes.
 *
 * Point pedagogique : les deux implementations renvoient exactement la meme
 * forme de reponse. Ce qui change n'est donc jamais le resultat, mais le chemin
 * pour l'obtenir -- visible dans le champ queries de l'enveloppe.
 */
public record TurtleView(String id,
                         String name,
                         String species,
                         String sex,
                         Integer birthYear,
                         Set<String> tags,
                         MeasurementsView measurements,
                         HabitatView habitat,
                         List<ObservationView> observations) {

    public record MeasurementsView(Double shellLengthCm, Double weightKg) {
    }

    public record HabitatView(String id, String name, String ocean, Double waterTempC, Boolean protectedArea) {
    }

    public record ObservationView(LocalDate date, String site, String observer, Integer healthScore) {
    }
}
