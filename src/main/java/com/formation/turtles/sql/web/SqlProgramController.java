package com.formation.turtles.sql.web;

import java.util.List;

import com.formation.turtles.common.CountByLabel;
import com.formation.turtles.common.ProgramView;
import com.formation.turtles.common.TurtleBrief;
import com.formation.turtles.sql.model.Program;
import com.formation.turtles.sql.model.Turtle;
import com.formation.turtles.sql.repo.ProgramRepository;
import com.formation.turtles.sql.repo.TurtleRepository;
import com.formation.turtles.trace.TracedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La relation plusieurs-a-plusieurs, cote relationnel.
 *
 * Une tortue suit plusieurs programmes de recherche, un programme suit plusieurs
 * tortues. C'est ici -- et seulement ici -- qu'apparait une vraie table de
 * jointure : turtle_program(turtle_id, program_id), deux cles etrangeres et
 * pas une colonne de donnees.
 */
@RestController
@RequestMapping("/api/sql/programs")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SqlProgramController {
    private final ProgramRepository programs;
    private final TurtleRepository turtles;

    /** Le catalogue : une entite a part entiere, qui existe meme sans tortue inscrite. */
    @GetMapping
    public TracedResponse<List<CountByLabel>> catalogue() {
        return TracedResponse.sql(
                "GROUP BY sur la table de jointure. Le « left join » garde les programmes "
                        + "auxquels aucune tortue n'est inscrite — un tag, lui, n'existe pas tant "
                        + "que personne ne le porte.",
                "@Query (JPQL) sur turtle_program", programs.countTurtlesByProgram());
    }

    /** SENS 1 : les tortues d'un programme. Une requête, deux jointures. */
    @GetMapping("/{name}/turtles")
    public TracedResponse<List<TurtleBrief>> turtlesOf(@PathVariable String name) {
        List<TurtleBrief> found = turtles.findByProgramsName(name).stream()
                .map(t -> new TurtleBrief(String.valueOf(t.getId()), t.getName(), t.getSpecies()))
                .toList();
        return TracedResponse.sql(
                "Les tortues d'un programme : UNE requête, deux jointures "
                        + "(turtle → turtle_program → program). La jointure permet de filtrer "
                        + "directement sur program.name — côté document, il faut d'abord résoudre "
                        + "le nom en _id. Revers de la médaille : une liste vide ne dit pas si le "
                        + "programme est inconnu ou simplement sans inscrit.",
                "JpaRepository (derived query à travers le @ManyToMany)", found);
    }

    /** SENS 2 : les programmes d'une tortue. Symétrique du précédent. */
    @GetMapping("/of-turtle/{id}")
    public ResponseEntity<TracedResponse<List<ProgramView>>> programsOf(@PathVariable Long id) {
        return turtles.findById(id)
                .map(turtle -> {
                    List<ProgramView> views = turtle.getPrograms().stream()
                            .map(SqlViews::of)
                            .toList();
                    return TracedResponse.sql(
                            "Les programmes d'une tortue : le lazy loading traverse la table de "
                                    + "jointure, et rapporte les QUINZE champs de chaque programme. "
                                    + "Comparez avec le jumeau MongoDB : là-bas, seuls les deux champs "
                                    + "recopiés dans la tortue sont renseignés.",
                            "JpaRepository + lazy loading", views);
                })
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Le jumeau de « toutes les tortues du CNRS » : une requête, deux jointures. */
    @GetMapping("/by-organisation/{organisation}/turtles")
    public TracedResponse<List<TurtleBrief>> turtlesOfOrganisation(@PathVariable String organisation) {
        List<TurtleBrief> found = turtles.findByProgramsOrganisation(organisation).stream()
                .map(t -> new TurtleBrief(String.valueOf(t.getId()), t.getName(), t.getSpecies()))
                .toList();
        return TracedResponse.sql(
                "UNE requête, deux jointures (turtle → turtle_program → program), et AUCUNE "
                        + "donnée dupliquée : l'organisme n'est stocké qu'à un seul endroit. "
                        + "C'est exactement ce que la jointure achète.",
                "JpaRepository (derived query jusqu'à program.organisation)", found);
    }

    /** Et son renommage : un UPDATE, une ligne. */
    @PatchMapping("/organisation")
    @Transactional
    public TracedResponse<String> renameOrganisation(@RequestParam String from, @RequestParam String to) {
        int updated = programs.renameOrganisation(from, to);
        return TracedResponse.sql(
                "UN update, sur les seules lignes de « program ». Toutes les tortues voient "
                        + "immédiatement le nouveau nom, sans rien à propager : c'est le bénéfice "
                        + "de la normalisation.",
                "@Modifying @Query (JPQL)", updated + " ligne(s) mise(s) à jour");
    }

    /** Inscrire une tortue : une ligne de plus dans la table de jointure. */
    @PostMapping("/enrol")
    @Transactional
    public ResponseEntity<TracedResponse<String>> enrol(@RequestParam Long turtleId,
                                                        @RequestParam String program) {
        Turtle turtle = turtles.findById(turtleId).orElse(null);
        Program found = programs.findByName(program).orElse(null);
        if (turtle == null || found == null) {
            return ResponseEntity.notFound().build();
        }
        turtle.enrolIn(found);
        // saveAndFlush, et pas save : Hibernate ne vide sa session qu'au commit, donc
        // apres la construction de la reponse. Sans flush explicite, l'INSERT dans la
        // table de jointure serait absent de la trace -- alors que c'est justement lui
        // qu'on veut montrer.
        turtles.saveAndFlush(turtle);
        return ResponseEntity.ok(TracedResponse.sql(
                "Inscription : un INSERT dans turtle_program. Ni la tortue ni le programme "
                        + "ne sont modifiés — c'est la table de jointure qui porte la relation.",
                "JpaRepository.save() → INSERT turtle_program",
                turtle.getName() + " est inscrite à « " + found.getName() + " »"));
    }
}
