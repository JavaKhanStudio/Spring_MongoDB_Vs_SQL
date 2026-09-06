package com.formation.turtles.sql.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.formation.turtles.common.SiteStat;
import com.formation.turtles.common.SpeciesStat;
import com.formation.turtles.common.TurtleView;
import com.formation.turtles.sql.model.Turtle;
import com.formation.turtles.sql.repo.TurtleRepository;
import com.formation.turtles.sql.service.TurtleSearchService;
import com.formation.turtles.trace.QueryTrace;
import com.formation.turtles.trace.QueryTraces;
import com.formation.turtles.trace.TracedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Le monde relationnel. Chaque endpoint a un jumeau exact sous /api/mongo,
 * et chaque reponse contient le SQL reellement execute.
 */
@RestController
@RequestMapping("/api/sql")
@Transactional(readOnly = true)   // indispensable : open-in-view est desactive
@RequiredArgsConstructor
public class SqlController {
    private final TurtleRepository turtles;
    private final TurtleSearchService searchService;
    private final JdbcTemplate jdbcTemplate;

    // =====================================================================
    // CHAPITRE 1 -- Concepts : ou vivent physiquement les donnees ?
    // =====================================================================

    /** Les lignes brutes des tables qui composent UNE tortue. A comparer au document unique. */
    @GetMapping("/concepts/rows/{id}")
    public TracedResponse<Map<String, Object>> rawRows(@PathVariable Long id) {
        Map<String, Object> tables = new LinkedHashMap<>();
        tables.put("turtle", traced("select * from turtle where id = ?", id));
        tables.put("turtle_tag", traced("select * from turtle_tag where turtle_id = ?", id));
        tables.put("observation", traced("select * from observation where turtle_id = ?", id));
        tables.put("habitat", traced(
                "select h.* from habitat h join turtle t on t.habitat_id = h.id where t.id = ?", id));
        tables.put("turtle_program", traced("select * from turtle_program where turtle_id = ?", id));
        tables.put("program", traced(
                "select p.* from program p join turtle_program tp on tp.program_id = p.id where tp.turtle_id = ?", id));

        return TracedResponse.sql(
                "Une tortue = 6 tables. Il faut autant de requêtes (ou de jointures) pour la reconstituer.",
                "JdbcTemplate (SQL brut)", tables);
    }

    /** Le schema physique : les tables creees par Hibernate a partir des entites. */
    @GetMapping("/concepts/schema")
    public TracedResponse<List<Map<String, Object>>> schema() {
        List<Map<String, Object>> columns = traced("""
                select table_name, column_name, data_type, is_nullable
                from information_schema.columns
                where table_schema = 'PUBLIC'
                order by table_name, ordinal_position
                """);
        return TracedResponse.sql(
                "Le schéma existe AVANT la donnée : ddl-auto l'a créé au démarrage. "
                        + "MongoDB, lui, n'a rien à créer.",
                "information_schema", columns);
    }

    /**
     * Les index du schema.
     *
     * A comparer avec /api/mongo/concepts/indexes. Le point a montrer :
     * l'index pose sur turtle_tag.tag est ce qui rend « toutes les tortues baguees
     * Argos » rapide -- son pendant MongoDB est l'index multikey sur le tableau
     * tags.
     */
    @GetMapping("/concepts/indexes")
    public TracedResponse<List<Map<String, Object>>> indexes() {
        List<Map<String, Object>> rows = traced("""
                select i.table_name, i.index_name, i.index_type_name,
                       listagg(ic.column_name, ', ') within group (order by ic.ordinal_position) as columns
                from information_schema.indexes i
                join information_schema.index_columns ic
                  on ic.index_name = i.index_name
                 and ic.table_name = i.table_name
                 and ic.table_schema = i.table_schema
                where i.table_schema = 'PUBLIC'
                group by i.table_name, i.index_name, i.index_type_name
                order by i.table_name, i.index_name
                """);
        return TracedResponse.sql(
                "Les index créés : ceux des clés (PK, FK, UNIQUE) et celui déclaré à la main "
                        + "sur turtle_tag.tag — sans lui, chercher par tag balaie toute la table.",
                "information_schema", rows);
    }

    /**
     * Les contraintes du schema : cles primaires, cles etrangeres, unicite.
     *
     * C'est la partie du modele relationnel qui n'a aucun equivalent cote
     * MongoDB : le moteur y garantit l'unicite d'un index, mais jamais l'integrite
     * referentielle entre deux collections.
     */
    @GetMapping("/concepts/constraints")
    public TracedResponse<List<Map<String, Object>>> constraints() {
        List<Map<String, Object>> rows = traced("""
                select tc.table_name, tc.constraint_type, tc.constraint_name,
                       listagg(kcu.column_name, ', ') within group (order by kcu.ordinal_position) as columns
                from information_schema.table_constraints tc
                join information_schema.key_column_usage kcu
                  on kcu.constraint_name = tc.constraint_name
                 and kcu.table_schema = tc.table_schema
                where tc.table_schema = 'PUBLIC'
                group by tc.table_name, tc.constraint_type, tc.constraint_name
                order by tc.table_name, tc.constraint_type
                """);
        return TracedResponse.sql(
                "Les garde-fous du modèle relationnel : PK, FK, UNIQUE. "
                        + "MongoDB n'a que l'unicité d'index — l'intégrité référentielle est à la "
                        + "charge de l'application.",
                "information_schema", rows);
    }

    /**
     * Le JdbcTemplate court-circuite Hibernate : son SQL n'est donc pas vu par
     * l'intercepteur. On l'ajoute a la main au collecteur pour que la reponse reste
     * complete.
     */
    private final List<Map<String, Object>> traced(String sql, Object... args) {
        QueryTraces.add(QueryTrace.sql(sql.replaceAll("\\s+", " ").trim()));
        return jdbcTemplate.queryForList(sql, args);
    }

    // =====================================================================
    // CHAPITRE 2 -- Mapping : le prix des relations
    // =====================================================================

    /** Lecture "naive" : la vue touche les collections en lazy loading -> 1 + N requetes. */
    @GetMapping("/turtles")
    public TracedResponse<List<TurtleView>> all(@RequestParam(defaultValue = "20") int limit) {
        List<TurtleView> views = SqlViews.of(turtles.findAllBy(PageRequest.of(0, limit)));
        return TracedResponse.sql(
                "Lecture d'une page de la liste. Regardez le nombre de requêtes : chaque collection "
                        + "en lazy loading en déclenche une de plus (problème 1+N). Augmentez ?limit "
                        + "et le compteur suit — c'est linéaire.",
                "JpaRepository.findAll(Pageable) + lazy loading", views);
    }

    @GetMapping("/turtles/{id}")
    public ResponseEntity<TracedResponse<TurtleView>> one(@PathVariable Long id) {
        return turtles.findById(id)
                .map(SqlViews::of)
                .map(view -> TracedResponse.sql(
                        "Une tortue complète : le SELECT principal, puis un SELECT par association.",
                        "JpaRepository.findById() + lazy loading", view))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** La parade : une jointure de chargement explicite. */
    @GetMapping("/turtles/{id}/fetch-join")
    public ResponseEntity<TracedResponse<TurtleView>> oneWithFetchJoin(@PathVariable Long id) {
        return turtles.findByIdWithEverything(id)
                .map(SqlViews::of)
                .map(view -> TracedResponse.sql(
                        "Même résultat en UNE requête, grâce à 'left join fetch'. "
                                + "C'est le travail que MongoDB évite en stockant en embedded.",
                        "@Query JPQL avec join fetch", view))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // =====================================================================
    // CHAPITRE 3 -- Repository vs API dynamique (Criteria)
    // =====================================================================

    @GetMapping("/turtles/search")
    public TracedResponse<List<TurtleView>> search(@RequestParam(required = false) String species,
                                                   @RequestParam(required = false) String sex,
                                                   @RequestParam(required = false) Double minWeight,
                                                   @RequestParam(required = false) String tag,
                                                   @RequestParam(required = false) Integer limit) {
        List<Turtle> found = searchService.search(species, sex, minWeight, tag, limit);
        return TracedResponse.sql(
                "Recherche multicritère construite à l'exécution. "
                        + "L'API Criteria de JPA est le pendant du MongoTemplate.",
                "EntityManager + CriteriaBuilder", SqlViews.of(found));
    }

    // =====================================================================
    // CHAPITRE 4 -- Requetes derivees, JPQL, projections
    // =====================================================================

    @GetMapping("/turtles/species/{species}")
    public TracedResponse<List<TurtleView>> bySpecies(@PathVariable String species) {
        return TracedResponse.sql(
                "Derived query : findBySpecies(...) — signature identique côté Mongo.",
                "JpaRepository (derived query)", SqlViews.of(turtles.findBySpecies(species)));
    }

    @GetMapping("/turtles/longer-than/{cm}")
    public TracedResponse<List<TurtleView>> longerThan(@PathVariable Double cm) {
        return TracedResponse.sql(
                "Derived query traversant l'objet embedded : MeasurementsShellLengthCmGreaterThan.",
                "JpaRepository (derived query)",
                SqlViews.of(turtles.findByMeasurementsShellLengthCmGreaterThanOrderByMeasurementsShellLengthCmDesc(cm)));
    }

    /**
     * « Toutes les tortues baguees Argos » : le jumeau SQL de
     * /api/mongo/turtles/tagged/{tag}.
     */
    @GetMapping("/turtles/tagged/{tag}")
    public TracedResponse<List<TurtleView>> tagged(@PathVariable String tag) {
        return TracedResponse.sql(
                "Chercher par tag traverse la table de valeurs : UNE jointure "
                        + "(turtle → turtle_tag). Côté MongoDB, le tag est dans le document : "
                        + "aucune jointure. Les deux ont besoin d'un index sur le tag.",
                "JpaRepository (findByTagsContaining → member of)",
                SqlViews.of(turtles.findByTagsContaining(tag)));
    }

    @GetMapping("/queries/heavy")
    public TracedResponse<List<TurtleView>> heavy(@RequestParam String species, @RequestParam double minWeight) {
        return TracedResponse.sql(
                "Requête explicite en JPQL — l'équivalent du @Query JSON de MongoDB.",
                "@Query (JPQL)", SqlViews.of(turtles.heavyOnesOfSpecies(species, minWeight)));
    }

    @GetMapping("/projections/names")
    public TracedResponse<List<TurtleRepository.NameAndSpecies>> names() {
        return TracedResponse.sql(
                "Closed projection : le SELECT ne remonte que deux colonnes.",
                "Projection par interface", turtles.findAllByOrderByName());
    }

    // =====================================================================
    // CHAPITRE 5 -- GROUP BY, l'ancetre du pipeline
    // =====================================================================

    @GetMapping("/stats/species")
    public TracedResponse<List<SpeciesStat>> statsBySpecies() {
        return TracedResponse.sql(
                "GROUP BY species : une seule requête déclarative, le moteur choisit le plan.",
                "@Query (JPQL) + constructeur", turtles.statsBySpecies());
    }

    @GetMapping("/stats/sites")
    public TracedResponse<List<SiteStat>> statsBySite() {
        return TracedResponse.sql(
                "GROUP BY sur la table fille : côté Mongo il faudra d'abord $unwind le tableau.",
                "@Query (JPQL) avec jointure", turtles.statsBySite());
    }
}
