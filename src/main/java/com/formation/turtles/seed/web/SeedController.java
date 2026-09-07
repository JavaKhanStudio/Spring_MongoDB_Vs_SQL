package com.formation.turtles.seed.web;

import com.formation.turtles.seed.SeedReport;
import com.formation.turtles.seed.SeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Le bouton "recharger" de la collection Bruno.
 *
 * A lancer une fois au debut de chaque seance, et a relancer des que MongoDB a
 * redemarre : les ObjectId sont alors regeneres, d'ou la requete 03 qui va
 * rechercher un identifiant frais.
 */
@RestController
@RequestMapping("/api/seed")
@RequiredArgsConstructor
public class SeedController {
    private final SeedService seed;

    /** Vide puis recharge H2 et MongoDB. */
    @PostMapping
    public SeedReport load() {
        return seed.seed();
    }

    /** Combien de documents / de lignes chaque monde contient-il en ce moment ? */
    @GetMapping
    public SeedReport status() {
        return seed.status();
    }
}
