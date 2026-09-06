# 1 · Concepts MongoDB et leurs équivalents Spring Data

> Le schéma complet des deux bases : [00-schema.md](00-schema.md)

> À jouer en parallèle : `bruno/01-concepts` — ou `./mvnw spring-boot:run -Dspring-boot.run.arguments=--demo=1`

## Le vocabulaire, terme à terme

| Monde relationnel | MongoDB | Côté Spring Data |
|---|---|---|
| base de données | base de données | l'URI de connexion |
| **table** | **collection** | `@Document(collection = "turtles")` |
| **ligne** | **document** (BSON) | une instance de la classe |
| **colonne** | **champ** | un attribut Java, renommable avec `@Field` |
| clé primaire (`BIGINT AUTO_INCREMENT`) | `_id` (`ObjectId`) | `@Id` (celui de `org.springframework.data.annotation`) |
| clé étrangère + jointure | référence, ou **embedding** | un `String habitatId`, ou l'objet lui-même |
| schéma imposé par le moteur | schéma souple (validation optionnelle) | rien à déclarer |
| `JOIN` | `$lookup` (dans un pipeline) | `LookupOperation` |
| `GROUP BY` | `$group` (dans un pipeline) | `Aggregation` |
| `JpaRepository` | `MongoRepository` | même API, même famille |
| `EntityManager` + API Criteria | `MongoTemplate` + `Criteria` | même rôle |
| transaction | transaction (depuis 4.0, sur replica set) | `@Transactional` |

## BSON, pas JSON

Le stockage n'est pas du JSON mais du **BSON** : un format binaire, typé et ordonné.
Ce que le JSON ne sait pas faire et que BSON fait :

* de vrais types numériques (`int32`, `int64`, `double`, `decimal128`) ;
* des dates (`$date`, en UTC), des données binaires, des `ObjectId` ;
* un parcours rapide, sans reparser tout le document.

C'est visible dans `/api/mongo/concepts/document/{id}` :

```jsonc
{
  "_id":  { "$oid": "6a95afcd80116a8f92f32aaf" },     // ObjectId, pas une chaîne
  "annee_naissance": 1998,                             // int32
  "observations": [
    { "date": { "$date": "2023-03-13T23:00:00Z" }, … } // instant UTC
  ],
  "_class": "com.formation.turtles.mongo.model.Turtle" // ajouté par Spring Data
}
```

Deux détails à commenter en cours :

* **`_class`** — Spring Data y range le type Java pour savoir quoi reconstruire à la
  lecture (utile en cas d'héritage). On peut le supprimer en configurant un
  `MappingMongoConverter` sans `DefaultTypeMapper`.
* **la date décalée** — un `LocalDate` (2023-03-14) devient un instant UTC
  (2023-03-13T23:00:00Z) : BSON n'a pas de type « date sans heure ». Pour éviter toute
  surprise, stocker des `Instant`, ou une chaîne `yyyy-MM-dd` si seule la date compte.

## L'ObjectId

12 octets : **4** d'horodatage · **5** aléatoires (propres au processus) · **3** de compteur.

```
6a95afcd  80116a8f92  f32aaf
└─ date ┘ └─ hasard ┘ └ cpt ┘
```

Trois conséquences pratiques (endpoint `/api/mongo/concepts/objectid/{id}`) :

1. **il porte sa date de création** — trier par `_id`, c'est presque trier par date ;
2. **il est généré par le client**, sans aller-retour, là où un `AUTO_INCREMENT` doit
   être attribué par le serveur : on peut donc préparer des références avant d'écrire ;
3. **il est unique globalement**, pas seulement dans une table.

Le type Java reste `String` : la conversion `String ↔ ObjectId` est automatique. Un `@Id`
non nul est repris tel quel — c'est ce qui permet d'utiliser une clé métier (un code
d'identification bagué, par exemple) comme `_id`.

## Schéma souple ≠ absence de schéma

Rien n'empêche deux documents d'une même collection d'avoir des champs différents
(`/api/mongo/concepts/sample`). Mais dans un projet Spring Data, **la classe Java joue le
rôle du schéma** : elle décide ce qu'on écrit et ce qu'on relit.

Ce que ça change au quotidien :

* ajouter un champ = ajouter un attribut, sans migration ni `ALTER TABLE` ;
* les anciens documents n'ont pas le champ → il arrive à `null` : c'est **à
  l'application** de gérer les deux formes de données pendant la transition ;
* pour verrouiller, MongoDB propose la **validation JSON Schema** au niveau de la
  collection (`$jsonSchema`), qui refuse les documents non conformes.

> **Le point à retenir** : le schéma n'a pas disparu, il a changé de place. Il est passé
> du moteur vers le code — plus souple, mais entièrement sous votre responsabilité.
