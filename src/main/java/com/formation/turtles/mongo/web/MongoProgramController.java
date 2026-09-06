package com.formation.turtles.mongo.web;

import java.util.List;

import com.formation.turtles.common.CountByLabel;
import com.formation.turtles.common.ProgramView;
import com.formation.turtles.common.TurtleBrief;
import com.formation.turtles.mongo.model.Program;
import com.formation.turtles.mongo.repo.ProgramMongoRepository;
import com.formation.turtles.mongo.repo.TurtleMongoRepository;
import com.formation.turtles.mongo.service.TurtleAggregationService;
import com.formation.turtles.mongo.service.TurtleTemplateService;
import com.formation.turtles.trace.TracedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La relation plusieurs-a-plusieurs, cote document.
 *
 * Le fait marquant du chapitre : il n'y a pas de table de jointure. MongoDB n'en
 * a pas la notion. Il faut donc choisir un cote qui portera la relation -- ici la tortue,
 * avec son tableau programIds -- ou accepter de la dupliquer des deux cotes.
 */
@RestController
@RequestMapping("/api/mongo/programs")
@RequiredArgsConstructor
public class MongoProgramController {
    private final ProgramMongoRepository programs;
    private final TurtleMongoRepository turtles;
    private final TurtleTemplateService templateService;
    private final TurtleAggregationService aggregationService;

    /** Le catalogue, compté par un $lookup dont le champ distant est un TABLEAU. */
    @GetMapping
    public TracedResponse<TurtleAggregationService.PipelineResult<CountByLabel>> catalogue() {
        return TracedResponse.mongo(
                "Le pipeline part cette fois de la collection « programs » : le $lookup cherche "
                        + "les tortues dont le TABLEAU programIds contient l'_id du programme. "
                        + "Un $lookup dont le foreignField est un tableau se comporte comme un "
                        + "« contient » — et les programmes sans tortue sont conservés, comme avec "
                        + "un LEFT JOIN.",
                "MongoTemplate.aggregate($lookup sur un tableau)", aggregationService.turtlesPerProgram());
    }

    /**
     * SENS 1 : les tortues d'un programme. Une requete -- grace a la copie du nom.
     */
    @GetMapping("/{name}/turtles")
    public TracedResponse<List<TurtleBrief>> turtlesOf(@PathVariable String name) {
        List<TurtleBrief> found = turtles.findByProgramsName(name).stream()
                .map(t -> new TurtleBrief(t.getId(), t.getName(), t.getSpecies()))
                .toList();
        return TracedResponse.mongo(
                "UNE requête, aucune jointure : { \"programs.name\": \"…\" }. C'est la copie du "
                        + "nom dans le document qui rend ça possible — comparez avec "
                        + "/{name}/turtles-by-id, qui n'utilise que l'identifiant.",
                "MongoRepository (derived query sur un champ dupliqué)", found);
    }

    /**
     * Le meme resultat sans la copie : il faut resoudre le nom en identifiant.
     * C'est ce que couterait un tableau d'identifiants nus.
     */
    @GetMapping("/{name}/turtles-by-id")
    public TracedResponse<List<TurtleBrief>> turtlesOfByReference(@PathVariable String name) {
        List<TurtleBrief> found = programs.findByName(name)
                .map(program -> turtles.findByProgramsProgramId(program.getId()).stream()
                        .map(t -> new TurtleBrief(t.getId(), t.getName(), t.getSpecies()))
                        .toList())
                .orElseGet(List::of);
        return TracedResponse.mongo(
                "DEUX requêtes : résoudre le nom en _id, puis filtrer sur programs.programId. "
                        + "Voilà exactement ce que la duplication du nom fait économiser — et c'est "
                        + "aussi le chemin sûr, puisque l'identifiant, lui, ne change jamais.",
                "MongoRepository × 2 (par la référence seule)", found);
    }

    /** SENS 2 : les programmes d'une tortue. Le document porte déjà la réponse entière. */
    @GetMapping("/of-turtle/{id}")
    public ResponseEntity<TracedResponse<List<ProgramView>>> programsOf(@PathVariable String id) {
        return turtles.findById(id)
                .map(turtle -> {
                    List<ProgramView> views = turtle.getPrograms().stream()
                            .map(ref -> ProgramView.fromReference(ref.getProgramId(), ref.getName(),
                                    ref.getOrganisation()))
                            .toList();
                    return TracedResponse.mongo(
                            "UNE seule requête : le document contient déjà le nom et l'organisme "
                                    + "de chaque programme (extended reference). Avec des identifiants "
                                    + "nus, il en faudrait une seconde ($in) rien que pour les libellés. "
                                    + "Comptez les champs de la réponse : trois, contre quinze pour le "
                                    + "jumeau SQL. Description, budget, protocole et responsable n'ont "
                                    + "pas été recopiés — ils restent dans la collection programs. "
                                    + "On copie l'étiquette, pas le dossier.",
                            "MongoRepository.findById() — tout est dans le document", views);
                })
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * LE bénéfice de la duplication : « toutes les tortues du CNRS », en une requête.
     *
     * Sans la copie de l'organisme dans le document, il faudrait d'abord chercher les
     * programmes du CNRS, puis les tortues correspondantes -- ou un $lookup.
     */
    @GetMapping("/by-organisation/{organisation}/turtles")
    public TracedResponse<List<TurtleBrief>> turtlesOfOrganisation(@PathVariable String organisation) {
        List<TurtleBrief> found = turtles.findByProgramsOrganisation(organisation).stream()
                .map(t -> new TurtleBrief(t.getId(), t.getName(), t.getSpecies()))
                .toList();
        return TracedResponse.mongo(
                "UNE requête, aucune jointure : { \"programs.organisation\": \"CNRS\" }, servie par "
                        + "un index multikey. C'est possible uniquement parce que l'organisme est "
                        + "DUPLIQUÉ dans chaque tortue. Le monde relationnel, lui, y arrive aussi en "
                        + "une requête — mais avec deux jointures, et sans rien dupliquer.",
                "MongoRepository (derived query sur un sous-document dupliqué)", found);
    }

    /** ET son prix : renommer l'organisme oblige à propager la copie. */
    @PatchMapping("/organisation")
    public TracedResponse<TurtleTemplateService.RenameResult> renameOrganisation(@RequestParam String from,
                                                                                 @RequestParam String to) {
        return TracedResponse.mongo(
                "Le prix de la duplication : DEUX écritures. La source dans « programs », puis "
                        + "toutes les copies dans « turtles » ($[element] + arrayFilters). Entre les "
                        + "deux, la base est incohérente, et rien n'impose la seconde. Côté SQL, la "
                        + "même opération est un UPDATE d'une seule ligne.",
                "MongoTemplate.updateMulti() × 2", templateService.renameOrganisation(from, to));
    }

    /** Inscrire une tortue : un $addToSet, du côté qui porte la relation. */
    @PostMapping("/enrol")
    public ResponseEntity<TracedResponse<String>> enrol(@RequestParam String turtleId,
                                                        @RequestParam String program) {
        Program found = programs.findByName(program).orElse(null);
        if (found == null) {
            return ResponseEntity.notFound().build();
        }
        long modified = templateService.enrolInProgram(turtleId, found);
        return ResponseEntity.ok(TracedResponse.mongo(
                "Inscription : un $addToSet du sous-document { programId, name, organisation }. "
                        + "Pas de ligne à insérer ailleurs — mais on écrit une COPIE des libellés, "
                        + "qu'il faudra maintenir si la source change.",
                "MongoTemplate.updateFirst($addToSet)",
                modified + " document modifié — inscrit à « " + found.getName() + " »"));
    }
}
