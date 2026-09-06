# 3 · MongoRepository vs MongoTemplate

> À jouer en parallèle : `bruno/03-repository-vs-template` — `mongo/service/TurtleTemplateService.java`

## Deux outils, une seule connexion

`MongoRepository` est **construit au-dessus** de `MongoTemplate` : même connexion, même
convertisseur, mêmes conversions de types. Les mélanger dans un même service n'a rien
d'un compromis — c'est l'usage normal.

```
  MongoRepository  ──┐
                     ├──> MongoTemplate ──> pilote MongoDB ──> serveur
  MongoTemplate    ──┘
```

## Le tableau de décision

| Besoin | Repository | Template |
|---|---|---|
| CRUD (`save`, `findById`, `delete`, `count`) | ✅ | ✅ |
| Requête connue à la compilation | ✅ derived query / `@Query` | ✅ mais verbeux |
| Pagination, tri | ✅ `Pageable`, `Sort` | ✅ |
| **Critères assemblés à l'exécution** | ❌ | ✅ `Criteria` |
| **Modifier un seul champ** (`$set`, `$inc`, `$push`) | ❌ | ✅ |
| **Upsert** | ❌ | ✅ |
| **findAndModify / findAndReplace** (atomique) | ❌ | ✅ |
| Écriture en masse (`updateMulti`, `bulkOps`) | ❌ | ✅ |
| Pipeline d'agrégation | `@Aggregation` (figé) | ✅ (construit à l'exécution) |
| Lire du BSON brut (`org.bson.Document`) | ❌ | ✅ |

**Règle courte** : le repository pour tout ce qui est connu d'avance ; le template dès
qu'il faut composer la requête, ou ne toucher qu'une partie du document.

## Pourquoi `save()` ne suffit pas

C'est le point le plus important du chapitre. Comparez les deux requêtes Bruno :

**`repository.save(tortue)`** → 1 lecture + **une réécriture complète** du document :

```jsonc
{ "update": "turtles",
  "updates": [ { "q": { "_id": … },
                 "u": { "_id": …, "name": "Crush", "species": …, "tags": [ … ],
                        "measurements": { … }, "observations": [ … ], … } } ] }
```

**`mongoTemplate.updateFirst(…, new Update().set("measurements.weightKg", 140))`** :

```jsonc
{ "update": "turtles",
  "updates": [ { "q": { "_id": … },
                 "u": { "$set": { "measurements.weightKg": 140.0 } } } ] }
```

Trois différences, pas une :

1. **le volume** — un champ contre tout le document ;
2. **la lecture évitée** — le `$set` ne lit rien avant d'écrire ;
3. **la concurrence** — `save()` écrase silencieusement ce qu'un autre client aurait
   modifié entre la lecture et l'écriture ; le `$set` ne touche que son champ.

Le même piège existe en JPA, mais Hibernate le masque : le *dirty checking* n'écrit que
les colonnes modifiées. Spring Data Mongo, lui, ne suit pas les objets — d'où
l'importance de connaître le `MongoTemplate`.

## Les opérateurs de mise à jour utiles

| Opérateur | Méthode `Update` | Ce qu'il fait |
|---|---|---|
| `$set` | `.set("champ", v)` | remplace une valeur |
| `$unset` | `.unset("champ")` | supprime le champ |
| `$inc` | `.inc("champ", 2.5)` | incrémente **côté serveur**, atomiquement |
| `$push` | `.push("observations", o)` | ajoute dans un tableau |
| `$addToSet` | `.addToSet("tags", t)` | ajoute si absent |
| `$pull` | `.pull("tags", t)` | retire d'un tableau |
| `$setOnInsert` | `.setOnInsert(…)` | ne s'applique qu'en cas de création (upsert) |

Et les trois portées : `updateFirst` (le premier trouvé), `updateMulti` (tous),
`upsert` (crée s'il n'existe pas).

## Le pendant côté JPA

Le `MongoTemplate` n'est pas une bizarrerie de MongoDB. Ouvrez côte à côte :

* `sql/service/TurtleSearchService.java` — `CriteriaBuilder`, `Root`, `Predicate` ;
* `mongo/service/TurtleTemplateService.java` — `Criteria`, `Query`.

Même structure, même raison d'être : construire une requête à l'exécution. L'équivalent
JPA du `$set` ciblé, c'est le `@Modifying @Query("update …")` — que l'on écrit exactement
pour les mêmes raisons.

> **Le point à retenir** : le repository couvre 80 % des besoins et ne coûte rien à écrire.
> Les 20 % restants — recherche dynamique, mise à jour partielle, atomicité — sont
> précisément ceux où une mauvaise solution fait mal en production.
