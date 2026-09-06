# 4 · Derived queries, `@Query` et projections

> À jouer en parallèle : `bruno/04-requetes-projections` — `mongo/repo/TurtleMongoRepository.java`

## Les derived queries : le nom EST la requête

Le point le plus rassurant du cours : **les signatures sont identiques dans les deux
mondes**. Ouvrez `sql/repo/TurtleRepository.java` et `mongo/repo/TurtleMongoRepository.java`
côte à côte.

```java
List<Turtle> findBySpecies(String species);
```

| | traduction |
|---|---|
| JPA | `select … from turtle where species = ?` |
| Mongo | `{ "species": "Chelonia mydas" }` |

### Naviguer dans les sous-documents

```java
findByMeasurementsShellLengthCmGreaterThan(Double cm)
```
→ `{ "measurements.shellLengthCm": { "$gt": 90 } }`

Le point du nom de méthode devient le point du chemin BSON. Côté JPA, la même méthode
traverse un `@Embeddable` — c'est-à-dire deux colonnes de la même table.

### Chercher dans un tableau

```java
findByObservationsSiteIn(List<String> sites)
```
→ `{ "observations.site": { "$in": ["Cairns", "El Nido"] } }`

**Sans jointure** : c'est le gain concret de l'embedding. Côté SQL, la même question
passe forcément par la table `observation`.

⚠️ Sur un tableau, l'**égalité signifie « contient »** : `{ "tags": "balise-argos" }`
sélectionne les documents dont le tableau contient cette valeur. Surprenant au début,
très pratique ensuite.

Le jumeau relationnel existe aussi — `findByTagsContaining` côté JPA, traduit en
`:tag member of t.tags`, c'est-à-dire une jointure vers `turtle_tag`. Comparez
`/api/sql/turtles/tagged/balise-argos` et `/api/mongo/turtles/tagged/balise-argos` :
mêmes 22 tortues, une jointure d'un côté, aucune de l'autre.

**Mais dans les deux cas, la performance vient de l'index** : index classique sur
`turtle_tag.tag`, index **multikey** sur le tableau `tags` (MongoDB indexe chaque élément
séparément). Sans index, les deux balaient tout. Le levier est le même — voir
`/api/sql/concepts/indexes` et `/api/mongo/concepts/indexes`.

### Les mots-clés utilisables

`And`, `Or`, `Between`, `LessThan`, `GreaterThan`, `Like`, `Regex`, `In`, `NotIn`,
`Near`, `Within` (géospatial), `IsNull`, `Exists`, `IgnoreCase`, `OrderBy…Desc`,
`Top`/`First`, `Distinct`, plus les préfixes `count…`, `exists…`, `delete…`.

**Quand s'arrêter** : dès que le nom devient illisible
(`findByFirstAndSecondAndThirdGreaterThanOrderByFourthDesc`), passer à `@Query` ou au
`MongoTemplate`.

## `@Query` : le filtre BSON à la main

```java
@Query("{ 'species': ?0, 'measurements.weightKg': { $gte: ?1 } }")
List<Turtle> heavyOnesOfSpecies(String species, double minWeight);
```

`?0`, `?1`… sont les paramètres, dans l'ordre. Les attributs disponibles :

| attribut | rôle |
|---|---|
| `value` | le filtre |
| `fields` | la **projection** : `"{ 'name': 1, 'species': 1, '_id': 0 }"` |
| `sort` | le tri |
| `count = true` | renvoie un nombre, sans remonter les documents |
| `delete = true` | supprime ce qui correspond |
| `collation` | comparaison de chaînes (accents, casse) |

### ⚠️ Le piège de `@Query` + `@Field`

`birthYear` est stocké sous le nom `annee_naissance` (`@Field`). Alors :

```java
List<Turtle> findByBirthYearLessThan(int year);              // ✅ nom JAVA
@Query("{ 'annee_naissance': { $lt: ?0 } }")                 // ✅ nom STOCKÉ
@Query("{ 'birthYear': { $lt: ?0 } }")                       // ❌ renvoie zéro résultat, sans erreur
```

Le JSON d'un `@Query` part **tel quel** vers le serveur : Spring Data ne traduit pas les
noms de champs à l'intérieur. La derived query, elle, passe par le mapping.

C'est la principale source d'erreurs après un renommage — et c'est silencieux : pas
d'exception, juste une liste vide.

## Les projections : ne remonter que le nécessaire

### Fermée (interface) — la plus efficace

```java
public interface TurtleNameAndSpecies {
    String getName();
    String getSpecies();
}
```

Spring Data lit l'interface et ajoute **tout seul** `"projection": { "name": 1, "species": 1 }`
à la commande. Vérifiez-le dans `/api/mongo/projections/closed`.

### Ouverte (SpEL) — confortable, pas économique

```java
public interface TurtleLabel {
    @Value("#{target.name + ' (' + target.species + ')'}")
    String getLabel();
}
```

Comme l'expression peut toucher n'importe quel champ, Spring Data **charge le document
entier** puis recompose. Regardez `/api/mongo/projections/open` : la commande n'a aucune
clause `projection`. À choisir en connaissance de cause.

### DTO (record) et projection dynamique

```java
public record TurtleCard(String name, String species, String habitatName) { }

<T> List<T> findByHabitatName(String habitatName, Class<T> type);
```

Une seule méthode, trois formes possibles : l'appelant passe `TurtleCard.class`,
`TurtleNameAndSpecies.class` ou `Turtle.class`.

### Au `MongoTemplate`

```java
Query query = new Query();
query.fields().include("name").include("species").exclude("_id");
```

### Où se fait le tri ?

| Approche | Le serveur envoie | Filtré par |
|---|---|---|
| Closed projection / `fields` / `Query.fields()` | 2 champs | **MongoDB** |
| Open projection (SpEL) | tout le document | l'application |
| `findAll()` puis `.map()` en Java | tout le document | l'application |

Sur 121 tortues, cela se voit à peine. Sur une collection réelle, c'est la différence
entre quelques kilo-octets et plusieurs méga-octets par requête.

> **Le point à retenir** : derived query tant que le nom reste lisible, `@Query` quand le
> filtre devient précis, `MongoTemplate` quand il devient dynamique. Et une projection dès
> que l'écran n'affiche que trois champs.
