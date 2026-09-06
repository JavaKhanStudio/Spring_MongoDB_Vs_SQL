# 5 · Introduction au pipeline d'agrégation

> À jouer en parallèle : `bruno/05-agregation` — `mongo/service/TurtleAggregationService.java`

## L'idée : une chaîne de montage

En SQL, on **décrit le résultat** et le moteur choisit son plan. En MongoDB, on décrit une
**suite d'étapes** ; chacune reçoit un flux de documents et en produit un autre.

C'est exactement le pipeline de `/api/mongo/aggregation/sites?top=5` :

```
 [121 documents tortue]
      │  $unwind     déplier le tableau d'observations     (aucun équivalent SQL)
      ▼
 [696 documents observation]
      │  $group      agréger par site                      (≈ GROUP BY)
      ▼
 [ 28 documents site]
      │  $sort       par nombre d'observations             (≈ ORDER BY)
      ▼
      │  $limit 5                                          (≈ LIMIT)
      ▼
      │  $project    renommer _id en site                  (≈ SELECT)
      ▼
 [ 5 lignes de résultat]
```

Un `$match` en tête filtrerait les tortues **avant** tout ce travail (≈ `WHERE`).

## Le dictionnaire

| Étape | Équivalent SQL | Rôle |
|---|---|---|
| `$match` | `WHERE` | filtrer |
| `$project` | `SELECT` | choisir, renommer, calculer |
| `$group` | `GROUP BY` | agréger (`$sum`, `$avg`, `$min`, `$max`, `$push`, `$first`) |
| `$sort` | `ORDER BY` | trier |
| `$limit` / `$skip` | `LIMIT` / `OFFSET` | paginer |
| `$lookup` | `LEFT JOIN` | joindre une autre collection |
| `$unwind` | *(aucun)* | déplier un tableau en N documents |
| `$bucket` | `CASE WHEN` + `GROUP BY` | histogramme |
| `$addFields` / `$set` | colonne calculée | enrichir |
| `$out` / `$merge` | `CREATE TABLE AS SELECT` | écrire le résultat |

Disponible aussi via `/api/mongo/aggregation/cheatsheet`.

## Le même calcul, des deux côtés

**SQL** (`/api/sql/stats/species`) :

```sql
select species, count(*), avg(shell_length_cm), max(weight_kg)
from turtle group by species order by count(*) desc
```

**MongoDB** (`/api/mongo/aggregation/species`) :

```javascript
[
  { $group: { _id: "$species",
              count: { $sum: 1 },
              avgShellLengthCm: { $avg: "$measurements.shellLengthCm" },
              maxWeightKg:      { $max: "$measurements.weightKg" } } },
  { $sort:    { count: -1 } },
  { $project: { _id: 0, key: "$_id", count: 1, avgShellLengthCm: 1, maxWeightKg: 1 } }
]
```

**Résultats identiques, au chiffre près.** À faire constater aux étudiants.

Deux détails :

* `$group` range **toujours** la clé de regroupement dans `_id` — d'où le `$project`
  final, qui la renomme pour coller au record Java ;
* `"$species"` avec un `$` désigne **la valeur du champ** ; sans `$`, c'est une constante.

En Java, le DSL suit le pipeline étape pour étape :

```java
newAggregation(
    group("species").count().as("count")
                    .avg("measurements.shellLengthCm").as("avgShellLengthCm"),
    sort(Sort.Direction.DESC, "count"),
    project("count", "avgShellLengthCm").and("_id").as("key").andExclude("_id"));
```

Chaque réponse REST expose le pipeline généré dans `result.explain` : prêt à coller dans
`mongosh` ou Compass.

## `$unwind` : l'étape qui n'existe pas en SQL

Les observations sont **dans** la tortue. Pour raisonner « une ligne par observation », il
faut d'abord les déplier : une tortue à 3 observations devient 3 documents.

```javascript
{ $unwind: "$observations" }
```

Si cette étape n'a pas d'équivalent SQL, c'est parce que le modèle relationnel a **déjà**
fait ce travail : la table `observation` est, littéralement, la version dépliée du tableau.

Comparez `/api/sql/stats/sites` et `/api/mongo/aggregation/sites` : mêmes résultats, un
`join … group by` d'un côté, un `$unwind` + `$group` de l'autre.

## `$lookup` : la vraie jointure

```javascript
{ $lookup: { from: "habitats", localField: "habitatId", foreignField: "_id", as: "habitat" } }
```

Exécuté **côté serveur** — c'est exactement ce que `@DBRef` ne sait pas faire.

Trois points à souligner (`/api/mongo/aggregation/habitats`) :

1. le résultat est un **tableau** (0, 1 ou n éléments) : on enchaîne presque toujours sur
   un `$unwind` ;
2. `$lookup` + `$unwind` se comporte comme un **INNER JOIN** — Goliath, qui n'a pas
   d'habitat connu, disparaît du résultat. Pour un LEFT JOIN, il faut
   `$unwind` avec `preserveNullAndEmptyArrays: true` ;
3. il faut que **les types correspondent** : `habitatId` est stocké en `ObjectId` grâce à
   `@Field(targetType = OBJECT_ID)`. Stocké en chaîne, il ne matcherait jamais — sans
   erreur, juste zéro résultat.

`$lookup` reste plus coûteux qu'un `JOIN` relationnel : c'est un outil de rapport et
d'analyse, pas la manière normale de lire une entité. Si vous en avez besoin sur le chemin
critique, c'est souvent le **modèle** qu'il faut revoir (embedding, ou dénormalisation).

## Performance : deux règles

1. **`$match` le plus tôt possible** — en tête de pipeline il peut utiliser les index et
   réduit le flux avant le `$group` ; placé après, il travaille sur tout le volume.
   Même logique qu'un `WHERE` évalué avant un `HAVING`.
2. **`$project` tôt** pour jeter les champs inutiles quand les documents sont gros.

Pour vérifier : `db.turtles.aggregate([…]).explain()` dans `mongosh`, ou l'onglet
*Explain* de Compass.

## Où écrire le pipeline ?

| Approche | Quand |
|---|---|
| `@Aggregation` sur le repository | pipeline **figé** — voir `TurtleMongoRepository.statsBySpecies()` |
| `MongoTemplate.aggregate(…)` | pipeline **construit à l'exécution** (filtres optionnels, périodes variables) |

Même arbitrage qu'au chapitre 3 : figé → repository, dynamique → template.

> **Le point à retenir** : le pipeline n'est pas « le SQL de MongoDB », c'est une chaîne de
> transformations. Un `GROUP BY` s'y traduit sans effort ; en échange, on obtient `$unwind`,
> `$bucket`, `$facet`, `$graphLookup` et `$merge` — des choses que le SQL standard ne fait
> pas, ou très mal.
