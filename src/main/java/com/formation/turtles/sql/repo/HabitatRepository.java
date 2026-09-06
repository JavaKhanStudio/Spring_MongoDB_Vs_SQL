package com.formation.turtles.sql.repo;

import java.util.Optional;

import com.formation.turtles.sql.model.Habitat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HabitatRepository extends JpaRepository<Habitat, Long> {
    Optional<Habitat> findByName(String name);

    /**
     * Changer la temperature : UN update, UNE ligne.
     *
     * La temperature n'est stockee qu'a un seul endroit, donc toutes les tortues de cet
     * habitat voient la nouvelle valeur instantanement. C'est le benefice de la
     * normalisation sur un champ qui bouge souvent.
     */
    @Modifying
    @Query("update Habitat h set h.waterTempC = :value where h.name = :name")
    int updateTemperature(@Param("name") String name, @Param("value") double value);
}
