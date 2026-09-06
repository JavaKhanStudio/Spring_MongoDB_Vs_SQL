# 0 · Le schéma, des deux côtés

> Version illustrée : la [page de synthèse](synthese.html), section « Les deux schémas ».
> Pour l'explorer en direct : `/api/sql/concepts/schema` et `/api/mongo/concepts/document/{id}`.

Le domaine est le même des deux côtés : **une tortue**, ses *mensurations*, ses
*observations* de terrain, ses *tags*, et son *habitat*. Ce qui change, c'est la façon de
le ranger.

---

## Côté relationnel — 6 tables

```
                     habitat_id                turtle_id
       HABITAT  1 ─────────── N  TURTLE  1 ─────────── N  TURTLE_TAG   (valeurs)
                                    │
                                    │ 1 ─────────── N  OBSERVATION     (table fille)
                                    │
                                    └ N ─────────── N  PROGRAM         (entité partagée)
                                              via TURTLE_PROGRAM       ← LA table de jointure


┌───────────────────────────────────────────┐
│ HABITAT   ·  12 lignes                    │
├───────────────────────────────────────────┤
│ id              BIGINT   PK               │
│ name            VARCHAR  NOT NULL, UNIQUE │
│ ocean           VARCHAR                   │
│ water_temp_c    DOUBLE                    │
│ protected_area  BOOLEAN                   │
└───────────────────────────────────────────┘

┌───────────────────────────────────────────────────────┐
│ TURTLE   ·  121 lignes                                │
├───────────────────────────────────────────────────────┤
│ id               BIGINT   PK                          │
│ name             VARCHAR  NOT NULL                    │
│ species          VARCHAR  NOT NULL                    │
│ sex              VARCHAR                              │
│ birth_year       INTEGER                              │
│ shell_length_cm  DOUBLE   ┐ aplati depuis @Embeddable │
│ weight_kg        DOUBLE   ┘ Measurements              │
│ habitat_id       BIGINT   FK → habitat.id             │
└───────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────┐
│ TURTLE_TAG   ·  265 lignes   —  @ElementCollection │
├────────────────────────────────────────────────────┤
│ turtle_id  BIGINT   FK → turtle.id                 │
│ tag        VARCHAR  la VALEUR elle-même, + index   │
│                     aucune clé primaire            │
└────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│ OBSERVATION   ·  696 lignes   —  @OneToMany │
├─────────────────────────────────────────────┤
│ id            BIGINT   PK                   │
│ turtle_id     BIGINT   FK → turtle.id       │
│ observed_on   DATE     NOT NULL             │
│ site          VARCHAR  NOT NULL             │
│ observer      VARCHAR                       │
│ health_score  INTEGER                       │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ PROGRAM   ·  8 lignes   —  entité partagée, 15 colonnes │
├─────────────────────────────────────────────────────────┤
│ id                    BIGINT   PK                       │
│ name                  VARCHAR  NOT NULL, UNIQUE         │
│ acronym               VARCHAR  NOT NULL                 │
│ organisation          VARCHAR  NOT NULL                 │
│ principal_investig..  VARCHAR                           │
│ contact_email         VARCHAR                           │
│ website               VARCHAR                           │
│ description           VARCHAR(1000)                     │
│ funding_euros         BIGINT                            │
│ start_year            INTEGER  NOT NULL                 │
│ end_year              INTEGER                           │
│ status                VARCHAR  NOT NULL  (enum)         │
│ focus_species         VARCHAR                           │
│ coordination_country  VARCHAR                           │
│ sampling_interval_d.  INTEGER  ┐ aplati depuis          │
│ tagging_method        VARCHAR  │ @Embeddable            │
│ min_shell_length_cm   DOUBLE   │ Protocol               │
│ satellite_tracking    BOOLEAN  ┘                        │
└─────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│ TURTLE_PROGRAM   ·  194 lignes   —  @ManyToMany          │
├──────────────────────────────────────────────────────────┤
│ turtle_id   BIGINT   FK → turtle.id     ┐ PRIMARY KEY    │
│ program_id  BIGINT   FK → program.id    ┘ composite      │
│                      DEUX clés étrangères, aucune donnée │
└──────────────────────────────────────────────────────────┘
```

**Une notion métier, six tables, 1 296 lignes** (121 + 12 + 265 + 696 + 8 + 194). Trois de ces
tables n'existent que parce que le modèle relationnel ne sait pas stocker une liste dans
une colonne :

| Table | Pourquoi elle existe | Annotation qui la crée |
|---|---|---|
| `turtle` | la tortue elle-même | `@Entity` |
| `turtle_tag` | une liste de **valeurs** | `@ElementCollection` + `@CollectionTable` |
| `observation` | une liste d'**objets** | `@OneToMany(mappedBy = "turtle")` |
| `habitat` | une entité partagée, côté « un » | `@Entity` + `@ManyToOne` côté tortue |
| `program` | une entité partagée, côté « plusieurs » | `@Entity` |
| `turtle_program` | **la** table de jointure | `@ManyToMany` + `@JoinTable` |

### ⚠️ `turtle_tag` n'est **pas** une table de jointure

Elle n'en a que l'allure. Le projet contient les deux motifs côte à côte, ce qui rend la
comparaison immédiate (`/api/sql/concepts/constraints`) :

```
TURTLE_TAG      FOREIGN KEY  (TURTLE_ID)                    ← UNE FK, aucune PK
TURTLE_PROGRAM  FOREIGN KEY  (TURTLE_ID)
                FOREIGN KEY  (PROGRAM_ID)
                PRIMARY KEY  (PROGRAM_ID, TURTLE_ID)        ← DEUX FK + PK composite
OBSERVATION     FOREIGN KEY  (TURTLE_ID)   +  PRIMARY KEY (ID)
TURTLE          FOREIGN KEY  (HABITAT_ID)  +  PRIMARY KEY (ID)
HABITAT         PRIMARY KEY  (ID)          +  UNIQUE (NAME)
PROGRAM         PRIMARY KEY  (ID)          +  UNIQUE (NAME)
```

`turtle_program` est une vraie table de jointure : **deux** clés étrangères, une clé
primaire composite, aucune colonne de données. `turtle_tag` n'a rien de tout ça — sa
colonne `tag` contient la **valeur** elle-même (`"balise-argos"`), pas une référence.

C'est le motif `@ElementCollection` : une collection de *valeurs* qui appartient à la
tortue. Relation **one-to-many vers des valeurs**, donc — et la chaîne `"balise-argos"`
est stockée en autant de lignes qu'il y a de tortues qui la portent, exactement comme
dans le tableau `tags` du document MongoDB. C'est pour cette raison qu'elle est modélisée
ainsi ici : c'est l'analogue le plus fidèle du tableau BSON.

À noter : Hibernate n'a créé **aucune clé primaire** sur cette table. Le `Set<String>`
côté Java empêche les doublons en mémoire ; la base, elle, ne les empêcherait pas.

**Quand passer à un vrai `@ManyToMany` ?** Le jour où l'étiquette devient une *entité* :
elle a ses propres attributs, elle existe même si personne ne la porte, et on veut la
renommer d'un seul UPDATE. C'est exactement le cas des **programmes de recherche** du
projet — d'où le `@ManyToMany` sur `Turtle.programs`, à comparer ligne à ligne avec les
tags. Le tableau ci-dessous résume le choix :

| | `turtle_tag` (valeurs) | `turtle_program` (jointure) |
|---|---|---|
| Clés étrangères | 1 | **2** |
| Clé primaire | aucune | composite `(program_id, turtle_id)` |
| Ce que porte la colonne | la valeur | une référence |
| L'étiquette a des attributs | non | oui (organisme, année) |
| Existe sans porteur | non | **oui** |
| Renommer | `update turtle_tag set tag = …` (N lignes) | 1 ligne dans `program` |
| Fautes de frappe possibles | oui | non — la FK ferme le vocabulaire |
| Équivalent document | tableau de valeurs `tags` | tableau de références `programIds` |

Les mensurations, elles, **n'ont pas de table** : `@Embeddable` les aplatit en deux
colonnes de `turtle` (`shell_length_cm`, `weight_kg`). C'est l'équivalent le plus proche
de l'embedding MongoDB — mais limité à un seul exemplaire, sans imbrication.

Le DDL est généré par Hibernate au démarrage (`ddl-auto: create-drop`) : le schéma existe
**avant** la moindre donnée.

---

## Côté document — 2 collections (+ 1 pour la démo)

Les mêmes données tiennent en **141 documents** : 121 tortues + 12 habitats + 8 programmes.
Une tortue = un document, avec tout ce qui lui appartient.

```jsonc
// collection « turtles »
{
  "_id":             ObjectId("6a95f57b151c755bc64b5f4f"),   // clé primaire
  "name":            "Crush",
  "species":         "Chelonia mydas",
  "sex":             "M",
  "annee_naissance": 1998,                        // @Field renomme birthYear

  "tags": [ "balise-argos", "adulte" ],           // ← remplace la table turtle_tag

  "measurements": {                               // ← remplace @Embeddable
    "shellLengthCm": 98.5,
    "weightKg":      132.0
  },

  "observations": [                               // ← remplace la table observation
    { "date": ISODate("2023-03-13T23:00:00Z"), "site": "Lady Elliot",
      "observer": "Aline Roy", "healthScore": 9 },
    { "date": ISODate("2024-01-07T23:00:00Z"), "site": "Cairns",
      "observer": "Aline Roy", "healthScore": 8 }
  ],

  "habitatId":   ObjectId("6a95f57b151c755bc64b5f4e"),  // ← remplace la FK habitat_id
  "habitatName": "Grande Barrière de corail",           // copie dénormalisée

  "programs": [                                   // ← remplace la table de jointure
    { "programId":    ObjectId("6a95ff7b3e21…"),  //    la référence : elle fait foi
      "name":         "Argos Océan Indien",       //    copie dénormalisée
      "organisation": "CNRS" },                   //    copie dénormalisée
    { "programId":    ObjectId("6a95ff7b3e21…"),
      "name":         "Reef Watch Queensland",
      "organisation": "AIMS" }
  ],

  "_class": "com.formation.turtles.mongo.model.Turtle"  // ajouté par Spring Data
}
```

```jsonc
// collection « habitats » — référencée, jamais embedded
{
  "_id":           ObjectId("6a95f51ef7da7c0042c2590d"),
  "name":          "Récif de Toliara",     // index unique
  "ocean":         "Océan Indien",
  "waterTempC":    27.5,
  "protectedArea": true,
  "_class":        "com.formation.turtles.mongo.model.Habitat"
}
```

```jsonc
// collection « programs » — entité partagée, jamais embedded
{
  "_id":          ObjectId("6a95ff7b3e21e6a633563fff"),
  "name":         "Argos Océan Indien",    // index unique
  "organisation": "CNRS",
  "startYear":    2015
}
```

```jsonc
// collection « turtles_dbref » — n'existe que pour la démonstration du chapitre 2
{
  "_id":     ObjectId("6a95f51ef7da7c0042c25912"),
  "name":    "Crush",
  "species": "Chelonia mydas",
  "habitat": DBRef("habitats", ObjectId("6a95f51ef7da7c0042c25910"))
}
```

### Index réellement créés

`auto-index-creation: true` fait créer au démarrage les index déclarés par `@Indexed` :

| Collection | Index | Origine |
|---|---|---|
| `turtles` | `_id_`, `name`, `species`, **`tags`**, **`programs.programId`**, **`programs.organisation`** | `@Id`, trois `@Indexed`, deux `@CompoundIndex` |
| `habitats` | `_id_`, `name` **unique** | `@Id`, `@Indexed(unique = true)` |
| `programs` | `_id_`, `name` **unique** | `@Id`, `@Indexed(unique = true)` |
| `turtles_dbref` | `_id_` | `@Id` |

Les trois index en gras portent sur des **tableaux** (y compris sur un champ *à
l'intérieur* d'un tableau de sous-documents) : ils sont donc automatiquement
**multikey** — MongoDB indexe chaque élément séparément. C'est ce qui rend
`{ tags: "balise-argos" }` et `{ "programs.organisation": "CNRS" }` aussi rapides qu'une
recherche sur un champ scalaire, sans aucune table annexe. Leur pendant relationnel est l'index posé
sur `turtle_tag.tag` et ceux des deux colonnes de `turtle_program`.
À comparer en direct : `/api/sql/concepts/indexes` et `/api/mongo/concepts/indexes`.

⚠️ `auto-index-creation` est pratique en formation, **déconseillé en production** :
la création d'index devient un effet de bord du démarrage. On préfère des migrations
explicites (Mongock, script de déploiement).

---

## La correspondance, ligne à ligne

| Côté relationnel | Côté document | Ce que ça change |
|---|---|---|
| table `turtle` | collection `turtles` | — |
| colonnes `shell_length_cm`, `weight_kg` | sous-document `measurements` | l'imbrication devient possible |
| table `turtle_tag` (+ FK) | tableau `tags` | une table en moins |
| table `observation` (+ FK, + PK) | tableau `observations` | une table et une jointure en moins |
| FK `habitat_id` | champ `habitatId` (`ObjectId`) | plus d'intégrité référentielle garantie |
| table `program` | collection `programs` | — |
| **table de jointure `turtle_program`** | **tableau `programs`** | la notion de table de jointure disparaît : il faut choisir un côté |
| `program.name`, `program.organisation` (une seule fois) | **copiés dans chaque tortue** | une lecture en moins, une propagation en plus |
| *(rien)* | champ `habitatName` | dénormalisé : lire vite, écrire deux fois |
| `BIGINT AUTO_INCREMENT` | `ObjectId` | généré côté client, porte sa date |
| DDL, contraintes `NOT NULL` | *(rien)* | la classe Java tient lieu de schéma |
| `JOIN` | `$lookup` (ou rien, si embedded) | la jointure devient un choix |

**Ce qui disparaît côté document** : trois tables, deux clés étrangères, deux clés
primaires techniques, et toutes les jointures pour lire une tortue.

**Ce qui apparaît** : la responsabilité de la cohérence. Rien n'empêche un `habitatId` de
pointer vers un habitat supprimé — c'est à l'application de s'en occuper.

### La duplication, à deux endroits et pour deux raisons

Le projet dénormalise volontairement à deux endroits, et ce n'est ni un oubli ni une
faute contre les formes normales :

| Copie | Ce qu'elle achète | Ce qu'elle coûte |
|---|---|---|
| `habitatName` dans chaque tortue | afficher la liste sans ouvrir `habitats` | propager si un habitat est renommé |
| `programs[].name` et `programs[].organisation` | les 52 tortues du CNRS en **une** requête, sans `$lookup` | 2 écritures et **52 copies** à propager quand un organisme change |

Notez la proportion : un programme a **quinze champs** (budget, responsable, description,
protocole imbriqué…), la copie n'en emporte que **deux**. On ne duplique pas l'entité, on
duplique l'étiquette qu'on affiche — les treize autres restent dans `programs`.

Le critère est toujours le même : **on ne duplique que ce qui est stable et lu souvent**.
Un nom d'organisme change une fois par décennie et s'affiche partout — bon candidat. Un
poids qui change à chaque pesée — mauvais candidat.

Et la référence (`programId`, `habitatId`) reste toujours là, à côté de la copie : c'est
elle la source de vérité. En cas de doute, on repasse par elle.

---

## L'explorer soi-même

| Outil | Adresse |
|---|---|
| Console H2 (le schéma relationnel en vrai) | <http://localhost:8080/h2-console> — `jdbc:h2:mem:turtles`, `sa`, sans mot de passe |
| mongo-express (les documents en vrai) | <http://localhost:8081> — base `turtles_demo` |
| Le schéma SQL en JSON | `/api/sql/concepts/schema` |
| Les contraintes (PK, FK, UNIQUE) | `/api/sql/concepts/constraints` |
| Les index, des deux côtés | `/api/sql/concepts/indexes` · `/api/mongo/concepts/indexes` |
| Les 4 lignes d'une tortue | `/api/sql/concepts/rows/1` |
| Le document brut d'une tortue | `/api/mongo/concepts/document/{id}` |

L'endpoint `/api/sql/concepts/constraints` liste les clés primaires, étrangères et
d'unicité — la partie du modèle relationnel qui n'a **aucun équivalent** côté MongoDB :
le moteur y garantit l'unicité d'un index, jamais l'intégrité référentielle entre deux
collections.
