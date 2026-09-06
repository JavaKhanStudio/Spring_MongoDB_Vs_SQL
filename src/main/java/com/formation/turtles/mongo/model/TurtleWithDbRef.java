package com.formation.turtles.mongo.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * La meme tortue, mais liee a son habitat par @DBRef.
 *
 * Collection separee (turtles_dbref) pour pouvoir montrer les deux
 * strategies cote a cote sur le meme jeu de donnees.
 *
 * Ce que fait @DBRef : il stocke { "$ref": "habitats", "$id": ObjectId(...) }
 * et Spring Data resout la reference a la lecture, avec une requete
 * supplementaire par document. Sur 100 tortues, c'est 101 allers-retours -- le bon
 * vieux probleme "1 + N", importe tel quel dans le monde document.
 *
 * Ce que @DBRef ne fait pas : il n'existe pas cote serveur. Aucun
 * $lookup, aucun filtre, aucun tri ne peut porter sur l'habitat ainsi lie.
 * D'ou la regle : preferer une manual reference (voir Turtle.getHabitatId()),
 * ou l'embedding quand la donnee est petite et lue avec le parent.
 */
@Document(collection = "turtles_dbref")
@Getter
@NoArgsConstructor
public class TurtleWithDbRef {
    @Id
    private String id;

    private String name;

    private String species;

    /** lazy = false par defaut : l'habitat est charge des la lecture de la tortue. */
    @DBRef
    private Habitat habitat;

    public TurtleWithDbRef(String name, String species, Habitat habitat) {
        this.name = name;
        this.species = species;
        this.habitat = habitat;
    }
}
