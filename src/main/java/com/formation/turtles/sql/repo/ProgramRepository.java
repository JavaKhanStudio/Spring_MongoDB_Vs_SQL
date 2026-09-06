package com.formation.turtles.sql.repo;

import java.util.List;
import java.util.Optional;

import com.formation.turtles.common.CountByLabel;
import com.formation.turtles.sql.model.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProgramRepository extends JpaRepository<Program, Long> {
    Optional<Program> findByName(String name);

    /**
     * Le catalogue des programmes avec leur nombre de tortues : un GROUP BY sur la
     * table de jointure.
     *
     * Le left join est essentiel : sans lui, un programme sans aucune tortue
     * inscrite disparaitrait du resultat.
     */
    @Query("""
            select new com.formation.turtles.common.CountByLabel(p.name, count(t))
            from Program p left join p.turtles t
            group by p.name
            order by count(t) desc, p.name
            """)
    List<CountByLabel> countTurtlesByProgram();

    /**
     * Renommer un organisme : un UPDATE, sur les seules lignes de program.
     * Rien a propager -- l'organisme n'existe qu'a un endroit. A comparer avec les deux
     * ecritures qu'exige la version denormalisee cote document.
     */
    @Modifying
    @Query("update Program p set p.organisation = :to where p.organisation = :from")
    int renameOrganisation(@Param("from") String from, @Param("to") String to);
}
