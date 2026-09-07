package com.formation.turtles.sql.service;

import java.util.ArrayList;
import java.util.List;

import com.formation.turtles.sql.model.Turtle;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * L'equivalent JPA du TurtleTemplateService.search(...) : construire une
 * requete a l'execution, critere par critere.
 *
 * Meme idee, meme structure
 * MongoTemplate n'est pas une bizarrerie de MongoDB, c'est le pendant de
 * l'API Criteria de JPA. Comparez simplement les deux fichiers cote a cote.
 *
 * com.formation.turtles.mongo.service.TurtleTemplateService;
 */
@Service
@RequiredArgsConstructor
public class TurtleSearchService {
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<Turtle> search(String species, String sex, Double minWeight, String tag, Integer limit) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Turtle> query = builder.createQuery(Turtle.class);
        Root<Turtle> turtle = query.from(Turtle.class);

        List<Predicate> predicates = new ArrayList<>();
        if (StringUtils.hasText(species)) {
            predicates.add(builder.equal(turtle.get("species"), species));
        }
        if (StringUtils.hasText(sex)) {
            predicates.add(builder.equal(turtle.get("sex"), sex));
        }
        if (minWeight != null) {
            predicates.add(builder.greaterThanOrEqualTo(
                    turtle.get("measurements").get("weightKg"), minWeight));
        }
        if (StringUtils.hasText(tag)) {
            // Un tag est dans une AUTRE table : il faut une jointure explicite.
            predicates.add(builder.equal(turtle.join("tags"), tag));
        }

        query.select(turtle).distinct(true).where(predicates.toArray(Predicate[]::new));

        var typed = entityManager.createQuery(query);
        if (limit != null && limit > 0) {
            typed.setMaxResults(limit);
        }
        return typed.getResultList();
    }
}
