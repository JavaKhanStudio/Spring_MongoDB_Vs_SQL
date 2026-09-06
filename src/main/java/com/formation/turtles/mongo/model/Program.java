package com.formation.turtles.mongo.model;

import com.formation.turtles.common.ProgramStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Collection programs.
 *
 * Meme entite que com.formation.turtles.sql.model.Program -- mais sans son cote
 * inverse : ici, aucune table de jointure n'existe. La relation N-N est portee par
 * le tableau Turtle.getPrograms() du cote tortue.
 *
 * Comptez les champs : quinze, dont un protocole imbrique et une description de
 * plusieurs lignes. L'extended reference stockee dans chaque tortue n'en recopie que
 * deux. C'est tout l'interet de la denormalisation ciblee : on copie l'etiquette,
 * pas le dossier.
 */
@Document(collection = "programs")
@Getter
@NoArgsConstructor
public class Program {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private String acronym;

    @Indexed
    private String organisation;

    private String principalInvestigator;
    private String contactEmail;
    private String website;
    private String description;
    private Long fundingEuros;
    private Integer startYear;
    private Integer endYear;

    /** Stocke sous forme de chaine ("ACTIF"), sans rien avoir a declarer. */
    private ProgramStatus status;

    private String focusSpecies;
    private String coordinationCountry;

    /** Sous-document : aucune annotation, aucune colonne a declarer. */
    private Protocol protocol;

    public Program(String name, String acronym, String organisation, String principalInvestigator,
                   String contactEmail, String website, String description, Long fundingEuros,
                   Integer startYear, Integer endYear, ProgramStatus status, String focusSpecies,
                   String coordinationCountry, Protocol protocol) {
        this.name = name;
        this.acronym = acronym;
        this.organisation = organisation;
        this.principalInvestigator = principalInvestigator;
        this.contactEmail = contactEmail;
        this.website = website;
        this.description = description;
        this.fundingEuros = fundingEuros;
        this.startYear = startYear;
        this.endYear = endYear;
        this.status = status;
        this.focusSpecies = focusSpecies;
        this.coordinationCountry = coordinationCountry;
        this.protocol = protocol;
    }
}
