package com.formation.turtles.common;

import java.util.List;

/**
 * Un habitat, sa temperature du moment, et les tortues qui l'occupent.
 *
 * La meme reponse des deux cotes -- mais pas au meme prix. Le monde relationnel la
 * produit en UNE requete : la jointure ramene la temperature fraiche avec les tortues.
 * Le monde document en demande DEUX : les tortues portent le NOM de l'habitat (copie
 * denormalisee), pas sa temperature -- il faut donc aller la chercher.
 *
 * C'est l'exacte contrepartie du gain montre ailleurs : ce qu'on n'a pas copie, il faut
 * aller le lire.
 */
public record HabitatReport(String name,
                            String ocean,
                            Double waterTempC,
                            boolean protectedArea,
                            int turtleCount,
                            List<TurtleBrief> turtles) {
}
