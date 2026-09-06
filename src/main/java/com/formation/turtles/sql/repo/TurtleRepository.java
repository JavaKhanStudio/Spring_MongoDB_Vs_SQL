package com.formation.turtles.sql.repo;

import java.util.List;
import java.util.Optional;

import com.formation.turtles.common.SiteStat;
import com.formation.turtles.common.SpeciesStat;
import com.formation.turtles.sql.model.Turtle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Le repository JPA, a comparer ligne a ligne avec
 * com.formation.turtles.mongo.repo.TurtleMongoRepository.
 *
 * Les derived queries s'ecrivent exactement pareil dans les deux mondes :
 * c'est la promesse de Spring Data. Ce qui change, c'est ce que Spring Data en fait :
 * du JPQL traduit en SQL ici, un document de filtre BSON la-bas.
 */
public interface TurtleRepository extends JpaRepository<Turtle, Long> {
    /**
     * Une page, sans le comptage : en renvoyant une List (et non une Page), Spring Data
     * n'emet PAS le « select count(*) » que Pageable declenche d'habitude. Le compteur
     * d'allers-retours mesure alors la lecture, et rien d'autre.
     */
    List<Turtle> findAllBy(Pageable pageable);

    // ---------- 1. Requetes derivees : meme signature que cote Mongo ----------

    List<Turtle> findBySpecies(String species);

    List<Turtle> findByMeasurementsShellLengthCmGreaterThanOrderByMeasurementsShellLengthCmDesc(Double minLength);

    List<Turtle> findByNameContainingIgnoreCase(String fragment);

    // ---------- 2. Requete explicite : ici du JPQL, la-bas du JSON ----------

    @Query("""
            select t from Turtle t
            where t.species = :species
              and t.measurements.weightKg >= :minWeight
            order by t.measurements.weightKg desc
            """)
    List<Turtle> heavyOnesOfSpecies(@Param("species") String species, @Param("minWeight") double minWeight);

    /**
     * La parade classique au probleme "1 + N" : une jointure de chargement explicite.
     * Cote MongoDB, la question ne se pose meme pas quand les donnees sont embedded.
     */
    @Query("""
            select distinct t from Turtle t
            left join fetch t.observations
            left join fetch t.habitat
            where t.id = :id
            """)
    Optional<Turtle> findByIdWithEverything(@Param("id") Long id);

    // ---------- 2 bis. Traverser une collection ----------

    /**
     * « Toutes les tortues baguees Argos ».
     *
     * Sur une collection, le mot-cle derive est Containing (traduit en
     * :tag member of t.tags) la ou MongoDB se contente de findByTags --
     * l'egalite sur un tableau y signifiant deja « contient ».
     */
    List<Turtle> findByTagsContaining(String tag);

    /**
     * La meme question, ecrite explicitement : la jointure vers la table de valeurs est
     * alors visible dans le SQL genere.
     */
    @Query("select distinct t from Turtle t join t.tags g where g = :tag")
    List<Turtle> taggedWith(@Param("tag") String tag);

    /**
     * Traversee de la relation N-N : Spring Data enchaine les deux jointures
     * (turtle -> turtle_program -> program) sans qu'on ait rien a ecrire.
     */
    List<Turtle> findByProgramsName(String name);

    /**
     * « Toutes les tortues du CNRS » : la traversee continue jusqu'a un attribut du
     * programme. Trois tables, deux jointures, une requete -- et aucune donnee
     * dupliquee : l'organisme n'est stocke qu'a un seul endroit.
     */
    List<Turtle> findByProgramsOrganisation(String organisation);

    /**
     * Les tortues d'un habitat, avec l'habitat lui-meme : UNE requete.
     *
     * Le join fetch ramene la temperature du moment en meme temps que les tortues. Cote
     * document, la tortue ne porte que le NOM de l'habitat : il faut une seconde requete
     * pour obtenir la temperature. C'est le prix de ce qu'on n'a pas copie.
     */
    @Query("select t from Turtle t join fetch t.habitat h where h.name = :name")
    List<Turtle> findByHabitatNameWithHabitat(@Param("name") String name);

    // ---------- 3. Projection : ne remonter que deux colonnes ----------

    /** Projection fermee : Spring Data ne selectionne que name et species. */
    interface NameAndSpecies {
        String getName();

        String getSpecies();
    }

    List<NameAndSpecies> findAllByOrderByName();

    // ---------- 4. Agregation : le GROUP BY, ancetre du pipeline ----------

    @Query("""
            select new com.formation.turtles.common.SpeciesStat(
                       t.species,
                       count(t),
                       avg(t.measurements.shellLengthCm),
                       max(t.measurements.weightKg))
            from Turtle t
            group by t.species
            order by count(t) desc
            """)
    List<SpeciesStat> statsBySpecies();

    /**
     * L'equivalent SQL du $unwind : la table fille joue deja le role du
     * "deploiement" du tableau, il suffit de grouper dessus.
     */
    @Query("""
            select new com.formation.turtles.common.SiteStat(
                       o.site,
                       count(o),
                       avg(o.healthScore))
            from Turtle t join t.observations o
            group by o.site
            order by count(o) desc
            """)
    List<SiteStat> statsBySite();
}
