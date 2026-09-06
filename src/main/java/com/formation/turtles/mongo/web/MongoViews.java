package com.formation.turtles.mongo.web;

import java.util.List;
import java.util.Set;

import com.formation.turtles.common.TurtleView;
import com.formation.turtles.mongo.model.Habitat;
import com.formation.turtles.mongo.model.Turtle;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Document -> vue JSON.
 *
 * Aucune transaction, aucun lazy loading : tout ce que la vue demande
 * etait deja dans le document lu. La seule exception est l'habitat, qui est
 * reference -- et qu'il faut donc aller chercher explicitement (voir le
 * controleur).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MongoViews {
    public static TurtleView of(Turtle turtle, Habitat habitat) {
        return new TurtleView(
                turtle.getId(),
                turtle.getName(),
                turtle.getSpecies(),
                turtle.getSex(),
                turtle.getBirthYear(),
                Set.copyOf(turtle.getTags()),
                turtle.getMeasurements() == null ? null : new TurtleView.MeasurementsView(
                        turtle.getMeasurements().getShellLengthCm(),
                        turtle.getMeasurements().getWeightKg()),
                habitat == null ? null : new TurtleView.HabitatView(
                        habitat.getId(), habitat.getName(), habitat.getOcean(),
                        habitat.getWaterTempC(), habitat.isProtectedArea()),
                turtle.getObservations().stream()
                        .map(o -> new TurtleView.ObservationView(o.getDate(), o.getSite(), o.getObserver(),
                                o.getHealthScore()))
                        .toList());
    }

    /**
     * Version "liste" : on n'ouvre pas l'habitat. Le nom denormalise stocke
     * dans le document suffit a afficher la liste -- zero requete supplementaire.
     */
    public static TurtleView of(Turtle turtle) {
        return of(turtle, null);
    }

    public static List<TurtleView> of(List<Turtle> turtles) {
        return turtles.stream().map(MongoViews::of).toList();
    }
}
