package com.formation.turtles.sql.web;

import java.util.List;
import java.util.Set;

import com.formation.turtles.common.ProgramView;
import com.formation.turtles.common.TurtleView;
import com.formation.turtles.sql.model.Habitat;
import com.formation.turtles.sql.model.Program;
import com.formation.turtles.sql.model.Protocol;
import com.formation.turtles.sql.model.Turtle;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Entite JPA -> vue JSON.
 *
 * Le mapping doit se faire dans la transaction : toucher
 * getObservations() ou getHabitat() declenche le lazy loading
 * , donc de nouvelles requetes. C'est exactement ce qu'on veut montrer.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SqlViews {
    public static TurtleView of(Turtle turtle) {
        Habitat habitat = turtle.getHabitat();
        return new TurtleView(
                String.valueOf(turtle.getId()),
                turtle.getName(),
                turtle.getSpecies(),
                turtle.getSex(),
                turtle.getBirthYear(),
                Set.copyOf(turtle.getTags()),
                new TurtleView.MeasurementsView(
                        turtle.getMeasurements().getShellLengthCm(),
                        turtle.getMeasurements().getWeightKg()),
                habitat == null ? null : new TurtleView.HabitatView(
                        String.valueOf(habitat.getId()), habitat.getName(), habitat.getOcean(),
                        habitat.getWaterTempC(), habitat.isProtectedArea()),
                turtle.getObservations().stream()
                        .map(o -> new TurtleView.ObservationView(o.getDate(), o.getSite(), o.getObserver(),
                                o.getHealthScore()))
                        .toList());
    }

    /** Un programme complet : les quinze champs, protocole imbrique compris. */
    public static ProgramView of(Program program) {
        Protocol protocol = program.getProtocol();
        return new ProgramView(String.valueOf(program.getId()), program.getName(), program.getAcronym(),
                program.getOrganisation(), program.getPrincipalInvestigator(), program.getContactEmail(),
                program.getWebsite(), program.getDescription(), program.getFundingEuros(),
                program.getStartYear(), program.getEndYear(), program.getStatus(),
                program.getFocusSpecies(), program.getCoordinationCountry(),
                protocol == null ? null : new ProgramView.ProtocolView(protocol.getSamplingIntervalDays(),
                        protocol.getTaggingMethod(), protocol.getMinShellLengthCm(),
                        protocol.isSatelliteTracking()));
    }

    public static List<TurtleView> of(List<Turtle> turtles) {
        return turtles.stream().map(SqlViews::of).toList();
    }
}
