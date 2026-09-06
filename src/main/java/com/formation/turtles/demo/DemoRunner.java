package com.formation.turtles.demo;

import java.util.List;

import com.formation.turtles.common.SpeciesStat;
import com.formation.turtles.mongo.repo.ProgramMongoRepository;
import com.formation.turtles.mongo.repo.TurtleMongoRepository;
import com.formation.turtles.mongo.service.TurtleAggregationService;
import com.formation.turtles.mongo.service.TurtleTemplateService;
import com.formation.turtles.sql.repo.ProgramRepository;
import com.formation.turtles.sql.repo.TurtleRepository;
import com.formation.turtles.sql.service.TurtleSearchService;
import com.formation.turtles.sql.web.SqlViews;
import com.formation.turtles.trace.QueryTrace;
import com.formation.turtles.trace.QueryTraces;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * La meme comparaison que les endpoints REST, mais dans la console — pratique pour
 * projeter au tableau sans client HTTP.
 *
 *   ./mvnw spring-boot:run -Dspring-boot.run.arguments=--demo=all
 *   ./mvnw spring-boot:run -Dspring-boot.run.arguments=--demo=3
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class DemoRunner implements ApplicationRunner {
    private final TurtleRepository sqlTurtles;
    private final ProgramRepository sqlPrograms;
    private final TurtleSearchService sqlSearch;
    private final TurtleMongoRepository mongoTurtles;
    private final ProgramMongoRepository mongoPrograms;
    private final TurtleTemplateService template;
    private final TurtleAggregationService aggregation;
    private final TransactionTemplate transaction;

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("demo")) {
            return;
        }
        String chapter = args.getOptionValues("demo").isEmpty() ? "all" : args.getOptionValues("demo").get(0);

        if (matches(chapter, "1")) {
            chapter1();
        }
        if (matches(chapter, "2")) {
            chapter2();
        }
        if (matches(chapter, "3")) {
            chapter3();
        }
        if (matches(chapter, "4")) {
            chapter4();
        }
        if (matches(chapter, "5")) {
            chapter5();
        }
        if (matches(chapter, "6")) {
            chapter6();
        }
    }

    private boolean matches(String asked, String chapter) {
        return "all".equalsIgnoreCase(asked) || chapter.equals(asked);
    }

    // ---------------------------------------------------------------------

    private void chapter1() {
        title("1", "Concepts : où vit une tortue ?");

        record("SQL — les lignes de la table turtle", () ->
                transaction.execute(status -> sqlTurtles.findAll().size() + " lignes dans turtle "
                        + "(+ turtle_tag, + observation, + habitat)"));

        record("MongoDB — le document brut", () -> {
            var document = template.rawSample(1).get(0);
            return "\n" + document.toJson();
        });

        note("Un document contient déjà ses tags et ses observations. La table, non.");
    }

    private void chapter2() {
        title("2", "Mapping : embedding vs reference");

        record("SQL — une tortue complète (lazy loading)", () ->
                transaction.execute(status -> {
                    var turtle = sqlTurtles.findAll().get(0);
                    return SqlViews.of(turtle).name() + " avec " + turtle.getObservations().size()
                            + " observations et " + turtle.getTags().size() + " tags";
                }));

        record("MongoDB — la même tortue, embedded", () -> {
            var turtle = mongoTurtles.findAll().get(0);
            return turtle.getName() + " avec " + turtle.getObservations().size()
                    + " observations et " + turtle.getTags().size() + " tags";
        });

        note("Même résultat. Comptez les requêtes ci-dessus : c'est tout le sujet du chapitre.");
    }

    private void chapter3() {
        title("3", "Repository vs Template");

        record("Repository — derived query figée à la compilation", () ->
                mongoTurtles.findBySpecies("Chelonia mydas").size() + " tortues vertes");

        record("Template — critères assemblés à l'exécution", () ->
                template.search("Chelonia mydas", "M", 50.0, null, null).size() + " résultats");

        record("Criteria API (JPA) — le pendant côté relationnel", () ->
                sqlSearch.search("Chelonia mydas", "M", 50.0, null, null).size() + " résultats");

        var crush = mongoTurtles.findByName("Crush").orElseThrow();
        record("Template — $inc atomique, sans lecture préalable", () ->
                template.grow(crush.getId(), 1.5) + " document modifié");
    }

    private void chapter4() {
        title("4", "Requêtes et projections");

        record("Dérivée : findByObservationsSiteIn (dans un tableau embedded)", () ->
                mongoTurtles.findByObservationsSiteIn(List.of("Cairns")).size() + " tortues");

        record("@Query JSON brut : nom STOCKÉ (annee_naissance)", () ->
                mongoTurtles.bornBefore(2005).size() + " tortues nées avant 2005");

        record("Closed projection : seuls name et species remontent", () ->
                mongoTurtles.findAllByOrderByName().size() + " projections");

        record("Open projection (SpEL) : document entier lu", () ->
                mongoTurtles.findBySexIgnoreCase("F").size() + " projections");

        note("Comparez la présence — ou l'absence — de la clause 'projection' dans les commandes.");
    }

    private void chapter5() {
        title("5", "Agrégation");

        record("SQL — GROUP BY species", () ->
                transaction.execute(status -> format(sqlTurtles.statsBySpecies())));

        record("MongoDB — $group + $sort + $project", () ->
                format(aggregation.statsBySpecies().rows()));

        record("MongoDB — $unwind sur les observations (sans équivalent SQL direct)", () ->
                aggregation.topSites(3).rows().toString());

        System.out.println("\nPipeline exécuté :\n" + aggregation.topSites(3).explain());
    }

    private void chapter6() {
        title("6", "Plusieurs-à-plusieurs : la table qui disparaît");

        String programme = "Argos Océan Indien";

        record("SQL — les tortues d'un programme (1 requête, 2 jointures)", () ->
                transaction.execute(status -> sqlTurtles.findByProgramsName(programme).size() + " tortues"));

        record("MongoDB — les mêmes (2 requêtes, 0 jointure)", () -> {
            var found = mongoPrograms.findByName(programme)
                    .map(p -> mongoTurtles.findByProgramsProgramId(p.getId()).size())
                    .orElse(0);
            return found + " tortues";
        });

        record("SQL — GROUP BY sur la table de jointure", () ->
                transaction.execute(status -> sqlPrograms.countTurtlesByProgram().toString()));

        record("MongoDB — $lookup dont le foreignField est un tableau", () ->
                aggregation.turtlesPerProgram().rows().toString());

        record("SQL — les tortues du CNRS (2 jointures, rien de dupliqué)", () ->
                transaction.execute(status -> sqlTurtles.findByProgramsOrganisation("CNRS").size() + " tortues"));

        record("MongoDB — les mêmes (0 jointure, organisme dupliqué)", () ->
                mongoTurtles.findByProgramsOrganisation("CNRS").size() + " tortues");

        note("Côté SQL la relation vit ENTRE les deux tables : les deux sens coûtent pareil. "
                + "Côté document elle vit d'UN côté : on choisit lequel privilégier.");
    }

    // ---------------------------------------------------------------------

    private String format(List<SpeciesStat> stats) {
        StringBuilder builder = new StringBuilder();
        for (SpeciesStat stat : stats) {
            builder.append(String.format("%n    %-28s n=%-3d moy=%-8.1f max=%.1f",
                    stat.key(), stat.count(),
                    stat.avgShellLengthCm() == null ? 0 : stat.avgShellLengthCm(),
                    stat.maxWeightKg() == null ? 0 : stat.maxWeightKg()));
        }
        return builder.toString();
    }

    private void title(String number, String label) {
        System.out.printf("%n%n╔══ CHAPITRE %s ── %s%n", number, label);
    }

    private void note(String text) {
        System.out.printf("%n  💡 %s%n", text);
    }

    /** Execute l'action, affiche son resultat, puis les requetes qu'elle a produites. */
    private void record(String label, java.util.function.Supplier<Object> action) {
        QueryTraces.reset();
        Object result = action.get();
        List<QueryTrace> traces = QueryTraces.captured();

        System.out.printf("%n  ▸ %s%n    → %s%n    → %d requête(s) :%n", label, result, traces.size());
        for (QueryTrace trace : traces) {
            System.out.printf("        [%s] %s%n", trace.engine(), abbreviate(trace.query()));
        }
    }

    private String abbreviate(String query) {
        String flat = query.replaceAll("\\s+", " ").trim();
        return flat.length() <= 220 ? flat : flat.substring(0, 217) + "...";
    }
}
