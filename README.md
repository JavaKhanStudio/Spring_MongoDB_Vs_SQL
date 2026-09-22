# 🐢 Spring Data MongoDB vs JPA — le même domaine, implémenté deux fois

<p align="center"><img src="docs/tortue.png" width="220" alt="Une tortue marine qui nage parmi les coraux"></p>

Support de cours. Une seule application Spring Boot contient **deux implémentations
complètes du même métier** — le suivi de tortues marines :

* `com.formation.turtles.sql` — JPA / Hibernate sur H2 ;
* `com.formation.turtles.mongo` — Spring Data MongoDB.

Mêmes données, mêmes réponses JSON. **Seul le chemin pour y arriver change** — et c'est
exactement ce que le projet rend visible : chaque réponse HTTP contient les requêtes
réellement envoyées à la base.

```jsonc
{
  "demo":       "Embedding : mensurations, tags et observations arrivent avec la tortue…",
  "world":      "MongoDB (Spring Data)",
  "layer":      "MongoRepository.findById()",
  "roundTrips": 1,                    // ← le chiffre qui porte tout le cours
  "queries":    [ { "engine": "MongoDB", "query": "{ \"find\": \"turtles\", … }" } ],
  "result":     { … }                 // ← identique côté SQL
}
```

Le même appel côté SQL renvoie `roundTrips: 4`, et les quatre `SELECT` qui vont avec.

---

## Démarrage

```bash
./run.sh          # démarre l'application — trouve le JDK tout seul
```

Ou directement, si votre `JAVA_HOME` pointe déjà sur un **JDK** :

```bash
./mvnw spring-boot:run
```

**Prérequis** : un JDK 17 ou 21, et un MongoDB accessible.

> ⚠️ Un **JRE ne suffit pas** : il n'a pas de compilateur. Si `java -version` répond mais
> que `javac -version` échoue, Maven s'arrête sur un message trompeur —
> `Fatal error compiling: error: release version 21 not supported`. Ce n'est ni Lombok ni
> Spring : c'est `javac` qui manque. `./run.sh` cherche un JDK dans `~/.jdks`, `~/.sdkman`,
> `/usr/lib/jvm` et `/opt/java`, et explique quoi installer s'il n'en trouve aucun.

> 💡 **Dans l'IDE** : le projet utilise Lombok. IntelliJ active seul le traitement des
> annotations ; Eclipse et VS Code demandent le plugin Lombok. Sans lui, l'IDE signale des
> `cannot find symbol: getName()` alors que la ligne de commande compile très bien.

* application : <http://localhost:8080> (le sommaire des paires d'endpoints)
* console H2 : <http://localhost:8080/h2-console> — JDBC `jdbc:h2:mem:turtles`, user `sa`, sans mot de passe
* mongo-express : <http://localhost:8081> — base `turtles_demo`

Les deux bases sont **rechargées à chaque démarrage** : on peut casser ce qu'on veut
pendant le cours.

### Configurer MongoDB

Par défaut l'application vise le conteneur MongoDB déjà présent sur le poste
(`localhost:27017`, utilisateur `amiral`), base **`turtles_demo`** — aucune autre base
n'est touchée. Tout est surchargeable sans modifier le code :

```bash
MONGO_HOST=localhost MONGO_PORT=27017 \
MONGO_USER=amiral   MONGO_PASSWORD=amiral \
MONGO_DB=turtles_demo ./mvnw spring-boot:run
```

Pour un MongoDB sans authentification, il suffit de remplacer l'URI dans
`src/main/resources/application.yml`.

---

## Le fil rouge

Une tortue, ses mensurations, ses observations de terrain, son habitat.

```
      MONDE RELATIONNEL (6 tables)              MONDE DOCUMENT (1 document)

  habitat ──1───N── turtle ──1───N── turtle_tag      {
                      │                              "_id": ObjectId("…"),
                      ├──1───N── observation         "name": "Crush",
                      │                              "annee_naissance": 1998,
                      └──N───N── program             "tags": ["balise-argos", "adulte"],
                            via turtle_program       "measurements": { "shellLengthCm": 98.5, … },
                                                     "observations": [ { "site": "Cairns", … }, … ],
                                                     "habitatId":  ObjectId("…"),
                                                     "habitatName": "Grande Barrière de corail",
                                                     "programs": [ { "programId": …,
                                                                     "name": …,
                                                                     "organisation": "CNRS" } ]
                                                   }
```

**1 296 lignes réparties sur 6 tables** d'un côté, **141 documents dans 3 collections**
de l'autre. 121 tortues, 12 habitats, 8 programmes de recherche, 696 observations, 265
tags, 194 inscriptions — définis **une seule fois** dans `seed/Dataset.java`, puis chargés
à l'identique des deux côtés.

Douze tortues sont écrites à la main (Crush, Ondine, Goliath… celles que les supports
citent nommément) ; les autres sont générées par un `Random` à graine fixe, donc
**identiques à chaque démarrage**. Les chiffres de cette page sont donc reproductibles.

Le domaine couvre exprès les quatre formes de relation, pour que chacune ait son vis-à-vis
document : un objet embedded (`@Embeddable`), une liste de valeurs
(`@ElementCollection`), une liste d'objets (`@OneToMany`), une entité partagée
(`@ManyToOne`) et une relation **plusieurs-à-plusieurs** (`@ManyToMany` — la seule vraie
table de jointure).

---

## Parcours en 5 chapitres

| # | Chapitre | Mémo | Requêtes Bruno |
|---|---|---|---|
| 0 | **Le schéma des deux bases** (tables, documents, correspondance) | [docs/00-schema.md](docs/00-schema.md) | `bruno/01-concepts` |
| 1 | Concepts MongoDB et équivalents Spring Data | [docs/01-concepts.md](docs/01-concepts.md) | `bruno/01-concepts` |
| 2 | Mapping objet-document, embedding vs reference | [docs/02-mapping.md](docs/02-mapping.md) | `bruno/02-mapping` |
| 3 | MongoRepository vs MongoTemplate | [docs/03-repository-vs-template.md](docs/03-repository-vs-template.md) | `bruno/03-repository-vs-template` |
| 4 | Derived queries, `@Query`, projections | [docs/04-requetes-projections.md](docs/04-requetes-projections.md) | `bruno/04-requetes-projections` |
| 5 | Pipeline d'agrégation | [docs/05-agregation.md](docs/05-agregation.md) | `bruno/05-agregation` |
| 6 | **Plusieurs-à-plusieurs** et **dénormalisation** : table de jointure vs extended reference | [docs/02-mapping.md](docs/02-mapping.md#le-cas-plusieurs-à-plusieurs) | `bruno/06-many-to-many` |

### La page de synthèse

Une fiche récapitulative à projeter ou à partager aux étudiants — **schémas des deux
bases**, tableaux d'équivalence, arbre de décision embedding / reference, anatomie d'un
pipeline :
<https://claude.ai/code/artifact/bc1aabe9-e8fc-4fdb-8405-9b4b09054dc9>
(source : [docs/synthese.html](docs/synthese.html))

### La collection Bruno

Ouvrir [Bruno](https://www.usebruno.com), *Open Collection* → dossier `bruno/`, puis
sélectionner l'environnement **Local**.

⚠️ **Lancer d'abord `00 · Préparation → Récupérer un identifiant Mongo`** : les `ObjectId`
changent à chaque rechargement, cette requête range celui de la première tortue dans la
variable `mongoId`.

Chaque requête porte, dans son onglet **Docs**, ce qu'il faut regarder dans la réponse.
Les requêtes vont par paires SQL / MongoDB pour être jouées l'une après l'autre.

### La démo console

Sans client HTTP, pour projeter au tableau :

```bash
./run.sh --demo=all      # ou --demo=6
```

Chaque étape affiche son résultat **et** les requêtes qu'elle a produites.

---

## Structure du projet

```
run.sh                lanceur : trouve un JDK, puis ./mvnw spring-boot:run
lombok.config         marque le code généré @lombok.Generated (Jacoco, Sonar)
src/main/java/com/formation/turtles/
├── common/          TurtleView, SpeciesStat, SiteStat — les vues JSON, communes aux deux mondes
├── sql/
│   ├── model/       @Entity, @Embeddable, @OneToMany, @ManyToOne, @ElementCollection, @ManyToMany
│   ├── repo/        TurtleRepository — derived queries, @Query JPQL, projection, GROUP BY
│   ├── service/     TurtleSearchService — API Criteria (le pendant du MongoTemplate)
│   └── web/         SqlController, SqlProgramController (la relation N-N)
├── mongo/
│   ├── model/       @Document, @Id, @Field, @Indexed, @Transient, @DBRef
│   ├── repo/        TurtleMongoRepository — dérivées, @Query JSON, projections, @Aggregation
│   ├── projection/  interface fermée / ouverte (SpEL) / record DTO
│   ├── service/     TurtleTemplateService ($set, $inc, $push, upsert, findAndModify)
│   │                TurtleAggregationService ($group, $unwind, $lookup, $match, $bucket)
│   └── web/         MongoController, MongoTemplateController, MongoAggregationController,
│                    MongoProgramController (la relation N-N, sans table de jointure)
├── trace/           le mouchard : intercepteur Hibernate + CommandListener MongoDB
├── seed/            Dataset (le jeu de données) + un chargeur par monde
└── demo/            DemoRunner — la même comparaison, en console
```

### Vocabulaire : les termes restent en anglais

Le cours est en français, mais **les termes techniques gardent leur nom anglais** — c'est
celui que les étudiants liront dans la documentation Spring et MongoDB, dans les messages
d'erreur et dans les réponses de Stack Overflow.

| Terme employé ici | Ce que c'est | Où on le rencontre |
|---|---|---|
| **embedding** / **embedded** | ranger la donnée *dans* le document parent | `@Embeddable`/`@Embedded` (JPA), sous-document (MongoDB) |
| **manual reference** | stocker l'`_id` de l'autre document, et faire la 2ᵉ requête soi-même | l'alternative recommandée à `@DBRef` |
| **extended reference** | la référence *plus* une copie des champs qu'on lit tout le temps | motif de dénormalisation |
| **derived query** | la méthode dont le *nom* est la requête | `findBySpecies(...)` |
| **closed / open projection** | interface dont les méthodes correspondent à des champs / à du SpEL | `@Value("#{target...}")` |
| **index multikey** | index sur un tableau : chaque élément est indexé séparément | `@Indexed` sur une `List`/`Set` |
| **lazy loading** | charger l'association au moment où on y touche | `FetchType.LAZY`, à l'origine du « 1+N » |
| **upsert**, **`$set`**, **`$inc`**, **`$push`**, **`$lookup`**, **`$unwind`** | inchangés | ce sont des noms d'opérateurs |

Restent en français les termes dont c'est **la** dénomination française établie, et qu'on
retrouve tels quels dans n'importe quel cours de bases de données : *jointure*, *table de
jointure*, *clé primaire*, *clé étrangère*, *index*, *schéma*, *sous-document*,
*dénormalisation*, *pipeline d'agrégation*.

### Conventions Lombok

Le projet utilise Lombok, mais **pas `@Data`** — et c'est un point à expliquer aux
étudiants plutôt qu'à subir :

| Où | Ce qu'on met | Pourquoi |
|---|---|---|
| Entités JPA et documents Mongo | `@Getter` + `@NoArgsConstructor` | pas de setters : l'état change par des méthodes métier (`addObservation`, `enrolIn`, `linkTo`) |
| Services, contrôleurs, seeders | `@RequiredArgsConstructor` sur des champs `final` | l'injection par constructeur, sans le constructeur |
| Journalisation | `@Slf4j` | remplace le `private static final Logger` |
| Classes utilitaires | `@NoArgsConstructor(access = PRIVATE)` | interdit l'instanciation |
| Vues, projections, statistiques | **`record`** | Lombok n'y a rien à faire |

**Pourquoi pas `@Data` sur une entité** — trois pièges bien connus :

* il génère `equals`/`hashCode` sur **tous** les champs : sur une entité JPA, cela compare
  des collections en lazy loading et casse le contrat de `hashCode` dès que l'`id` est attribué
  par la base ;
* il génère `toString()`, qui traverse les associations en lazy loading — un `log.debug(turtle)`
  déclenche alors une cascade de requêtes, voire une `LazyInitializationException` ;
* il génère des setters partout, ce qui contredit le fait qu'une tortue et son habitat
  doivent rester cohérents.

Les constructeurs « métier » (`new Turtle(name, species, sex, birthYear, measurements)`)
sont volontairement écrits à la main : ils ne prennent pas l'`id`, qui est généré, donc
`@AllArgsConstructor` ne conviendrait pas.

Le fichier `lombok.config` marque le code généré avec `@lombok.Generated`, pour que Jacoco
et SonarQube ne le comptent pas comme du code non testé.

### Comment fonctionne le mouchard

Deux branchements symétriques, ~150 lignes en tout :

* **SQL** — un `StatementInspector` Hibernate déclaré dans `application.yml` ; Hibernate
  l'appelle avec le SQL final juste avant JDBC.
* **MongoDB** — un `CommandListener` ajouté au `MongoClientSettings` ; le pilote lui passe
  chaque commande BSON (débarrassée ici de la plomberie de session pour rester lisible).

Les deux déposent leur trace dans un `ThreadLocal`, remis à zéro par un filtre à chaque
requête HTTP. C'est un outil **pédagogique** : en production, ce rôle revient à Micrometer
ou OpenTelemetry.

---

## Dépannage

| Symptôme | Cause probable |
|---|---|
| `Command … requires authentication` | identifiants MongoDB : voir *Configurer MongoDB* |
| `Connection refused` sur 27017 | le conteneur MongoDB n'est pas démarré |
| `release version 21 not supported` | pas de `javac` : un JRE, pas un JDK. Lancez `./run.sh`, ou pointez `JAVA_HOME` sur un JDK 17+ |
| `cannot find symbol: getName()` dans l'IDE seulement | Lombok : activer le traitement des annotations (IntelliJ le fait seul ; plugin Lombok requis sur Eclipse et VS Code) |
| Une requête Bruno renvoie 404 | `mongoId` est périmé : rejouez `00 · Préparation` |
# Spring_MongoDB_Vs_SQL
