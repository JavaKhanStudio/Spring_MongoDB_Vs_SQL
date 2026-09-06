package com.formation.turtles.mongo.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Collection habitats.
 *
 * Un habitat est une entite a part entiere (partagee par plusieurs tortues,
 * mise a jour independamment) : c'est le cas typique ou l'on ne l'embedded
 * pas. On le reference.
 */
@Document(collection = "habitats")
@Getter
@NoArgsConstructor
public class Habitat {
    @Id
    private String id;   // String + @Id => MongoDB y range un ObjectId

    @Indexed(unique = true)
    private String name;

    private String ocean;
    private Double waterTempC;
    private boolean protectedArea;

    public Habitat(String name, String ocean, Double waterTempC, boolean protectedArea) {
        this.name = name;
        this.ocean = ocean;
        this.waterTempC = waterTempC;
        this.protectedArea = protectedArea;
    }
}
