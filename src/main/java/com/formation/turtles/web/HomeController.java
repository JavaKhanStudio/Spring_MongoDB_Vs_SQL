package com.formation.turtles.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Le sommaire : les paires d'endpoints, chapitre par chapitre. */
@RestController
public class HomeController {
    private record Pair(String question, String sql, String mongo) {
    }

    @GetMapping("/")
    public Map<String, Object> index() {
        Map<String, Object> index = new LinkedHashMap<>();
        index.put("projet", "🐢 Spring Data MongoDB vs JPA");
        index.put("mode d'emploi", "Chaque réponse contient le champ 'queries' : "
                + "la ou les requêtes réellement envoyées à la base.");
        index.put("0. Préparation", List.of(
                new Pair("Charger le jeu de données dans les deux bases (à faire en premier, "
                                + "et à refaire après tout redémarrage de MongoDB)",
                        "POST /api/seed", "POST /api/seed"),
                new Pair("Les deux bases sont-elles chargées ?",
                        "GET /api/seed", "GET /api/seed")));

        index.put("1. Concepts", List.of(
                new Pair("Où vit physiquement une tortue ?",
                        "/api/sql/concepts/rows/{id}", "/api/mongo/concepts/document/{id}"),
                new Pair("La clé primaire",
                        "/api/sql/concepts/schema", "/api/mongo/concepts/objectid/{id}")));

        index.put("2. Mapping objet-document", List.of(
                new Pair("Lire une tortue complète",
                        "/api/sql/turtles/{id}", "/api/mongo/links/embedded/{id}"),
                new Pair("Idem, optimisé",
                        "/api/sql/turtles/{id}/fetch-join", "/api/mongo/links/manual/{id}"),
                new Pair("Le piège du 1+N",
                        "/api/sql/turtles", "/api/mongo/links/dbref")));

        index.put("3. Repository vs Template", List.of(
                new Pair("Recherche multicritère dynamique",
                        "/api/sql/turtles/search?species=...&minWeight=...",
                        "/api/mongo/template/search?species=...&minWeight=..."),
                new Pair("Modifier un seul champ",
                        "(UPDATE ... SET colonne = ?)",
                        "PATCH /api/mongo/template/{id}/weight vs .../weight-by-save")));

        index.put("4. Requêtes et projections", List.of(
                new Pair("Derived query",
                        "/api/sql/turtles/species/{species}", "/api/mongo/turtles/species/{species}"),
                new Pair("Requête explicite (JPQL vs JSON)",
                        "/api/sql/queries/heavy?species=...&minWeight=...",
                        "/api/mongo/queries/heavy?species=...&minWeight=..."),
                new Pair("Projection",
                        "/api/sql/projections/names", "/api/mongo/projections/closed")));

        index.put("5. Agrégation", List.of(
                new Pair("Compter par espèce",
                        "/api/sql/stats/species", "/api/mongo/aggregation/species"),
                new Pair("Grouper sur les observations",
                        "/api/sql/stats/sites", "/api/mongo/aggregation/sites"),
                new Pair("Joindre les habitats",
                        "(join habitat)", "/api/mongo/aggregation/habitats")));

        index.put("outils", Map.of(
                "console H2", "http://localhost:8080/h2-console (jdbc:h2:mem:turtles / sa)",
                "mongo-express", "http://localhost:8081 — base turtles_demo",
                "aide-mémoire pipeline", "/api/mongo/aggregation/cheatsheet"));
        return index;
    }
}
