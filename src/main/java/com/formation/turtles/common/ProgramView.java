package com.formation.turtles.common;

/**
 * La vue JSON complete d'un programme : quinze champs.
 *
 * Point de demonstration : quand cette vue est construite depuis l'extended reference
 * stockee dans une tortue, seuls name et organisation sont renseignes -- tout le reste
 * revient a null. On voit alors, en une reponse, exactement ce que la denormalisation
 * a choisi de copier... et ce qu'elle a laisse dans la collection programs.
 */
public record ProgramView(String id,
                          String name,
                          String acronym,
                          String organisation,
                          String principalInvestigator,
                          String contactEmail,
                          String website,
                          String description,
                          Long fundingEuros,
                          Integer startYear,
                          Integer endYear,
                          ProgramStatus status,
                          String focusSpecies,
                          String coordinationCountry,
                          ProtocolView protocol) {

    public record ProtocolView(Integer samplingIntervalDays, String taggingMethod,
                               Double minShellLengthCm, Boolean satelliteTracking) {
    }

    /** La vue reduite a ce que porte une extended reference : deux champs sur quinze. */
    public static ProgramView fromReference(String id, String name, String organisation) {
        return new ProgramView(id, name, null, organisation, null, null, null, null, null,
                null, null, null, null, null, null);
    }
}
