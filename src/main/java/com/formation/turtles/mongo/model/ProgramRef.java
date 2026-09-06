package com.formation.turtles.mongo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

/**
 * Une extended reference (extended reference pattern) vers un programme.
 *
 * Le tableau programs de la tortue ne stocke pas que des identifiants : il
 * contient aussi les deux champs qu'on lit tout le temps, le nom du programme et son
 * organisme. C'est une duplication assumee.
 *
 * Ce qu'elle achete : « toutes les tortues du CNRS » devient
 * { "programs.organisation": "CNRS" } -- une seule requete, servie par un index
 * multikey, sans jointure ni $lookup. Avec de simples identifiants, il faudrait
 * d'abord chercher les programmes du CNRS, puis les tortues correspondantes.
 *
 * Ce qu'elle coute : si un organisme est renomme, il faut propager la copie dans
 * toutes les tortues concernees -- et rien, dans le moteur, ne l'imposera. On ne duplique
 * donc que des champs stables et souvent lus. Le programId reste la
 * source de verite : en cas de doute, c'est lui qui fait foi.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProgramRef {
    /** La reference : c'est elle qui fait foi. */
    @Field(targetType = FieldType.OBJECT_ID)
    private String programId;

    /** Copie denormalisee -- change rarement, lue partout. */
    private String name;

    /** Copie denormalisee -- ce qui rend « les tortues du CNRS » immediat. */
    private String organisation;

    public static ProgramRef of(Program program) {
        return new ProgramRef(program.getId(), program.getName(), program.getOrganisation());
    }
}
