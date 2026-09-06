package com.formation.turtles.common;

/**
 * Etat d'un programme de recherche.
 *
 * Le meme enum sert aux deux mondes. Cote JPA il faut le dire explicitement
 * (@Enumerated(EnumType.STRING), sans quoi Hibernate stocke l'ORDINAL -- et toute
 * insertion d'une valeur au milieu de l'enum corrompt les donnees existantes).
 * Cote MongoDB, Spring Data stocke le nom par defaut.
 */
public enum ProgramStatus {
    ACTIF,
    TERMINE,
    SUSPENDU
}
