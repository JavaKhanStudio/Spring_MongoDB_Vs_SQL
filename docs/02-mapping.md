# 2 · Le mapping objet-document

> Le schéma complet des deux bases : [00-schema.md](00-schema.md)

> À jouer en parallèle : `bruno/02-mapping` — fichiers `mongo/model/*.java` vs `sql/model/*.java`

## Les annotations, comparées

| Intention | JPA | Spring Data MongoDB |
|---|---|---|
| Cette classe est persistante | `@Entity` + `@Table(name=…)` | `@Document(collection = "turtles")` |
| Ceci est la clé | `@Id` + `@GeneratedValue` | `@Id` (paquet `org.springframework.data.annotation`) |
| Renommer le champ stocké | `@Column(name = "birth_year")` | `@Field("annee_naissance")` |
| Objet sans identité propre | `@Embeddable` + `@Embedded` | *(rien à écrire)* |
| Collection d'objets | `@OneToMany` + table fille | *(un `List<…>`, embedded)* |
| Collection de valeurs | `@ElementCollection` + table | *(un `Set<String>`)* |
| Lien vers une autre entité | `@ManyToOne` + `@JoinColumn` | un identifiant, ou `@DBRef` |
| Ne pas persister | `@Transient` (JPA) | `@Transient` (Spring Data) |
| Index | `@Index` dans `@Table` | `@Indexed`, `@CompoundIndex` |

⚠️ **Deux annotations portent le même nom dans les deux mondes** (`@Id`, `@Transient`) mais
viennent de paquets différents. L'import qui se trompe est l'erreur numéro un des débuts :
côté Mongo, c'est toujours `org.springframework.data.annotation.*`.

Le plus frappant reste ce qu'on **n'écrit pas** : dans `mongo/model/Measurements.java` et
`Observation.java`, il n'y a **aucune annotation**. Tout objet devient naturellement un
sous-document.

## Embedding ou reference ?

C'est **la** décision de modélisation. Elle se prend champ par champ, et elle n'est pas
réversible sans migration.

```
                    La donnée est-elle lue avec son parent,
                    presque à chaque fois ?
                              │
                 ┌── oui ─────┴───── non ──┐
                 │                          │
    Appartient-elle vraiment              REFERENCE
    au parent (elle n'a pas de              (habitatId)
    sens seule) ?
          │
   ┌─ oui ┴─ non ─┐
   │               │
Le tableau reste  REFERENCE
borné (dizaines,
pas millions) ?
   │
┌ oui ┴ non ┐
│            │
EMBEDDING   REFERENCE  
            (ou collection dédiée)
```

Dans le projet :

| Donnée | Choix | Pourquoi |
|---|---|---|
| `measurements` | embedded | un seul exemplaire, toujours lu avec la tortue |
| `tags` | embedded (tableau) | quelques valeurs, propres à la tortue |
| `observations` | embedded (tableau) | appartiennent à la tortue, quelques dizaines au plus |
| `habitat` | **référencé** (`habitatId`) | partagé par plusieurs tortues, évolue seul |
| `habitatName` | **dupliqué** | pour afficher une liste sans aucune jointure |
| `programs` | **référencé + dénormalisé** (tableau de `{ programId, name, organisation }`) | relation N-N, avec les libellés copiés pour filtrer sans jointure |

Les garde-fous à énoncer :

* un document ne peut pas dépasser **16 Mo** ;
* un tableau qui grandit sans limite (journal d'événements, messages…) doit devenir une
  **collection à part** — c'est le motif *bucket* / *outlier* ;
* dupliquer une donnée (`habitatName`), c'est **choisir** de la mettre à jour soi-même
  quand la source change : c'est un compromis lecture rapide / écriture plus coûteuse,
  pas un oubli.

## La règle, et le fait qu'elle s'applique champ par champ

On privilégie l'**embedding** quand les données :

* appartiennent au même **agrégat métier** ;
* sont généralement **lues ensemble** ;
* ont un **cycle de vie commun**.

> *« Data that is accessed together should be stored together. »*

On l'évite quand la duplication devient coûteuse à maintenir. Si 100 000 utilisateurs
partagent la même entreprise, embarquer ses informations complètes revient à les dupliquer
100 000 fois — et à les maintenir autant de fois si le nom ou l'adresse changent. Comme
l'entreprise a par ailleurs une existence et un cycle de vie propres, une **référence vers
son identifiant** s'impose.

### Le piège : croire que la décision se prend par entité

Elle se prend **par champ**. L'habitat le montre à lui seul — il tranche dans les trois
sens à la fois :

| Champ | Traitement | Pourquoi |
|---|---|---|
| `name` | **copié** dans chaque tortue (`habitatName`) | change une fois par décennie, affiché partout |
| `waterTempC` | **non copié** | c'est une mesure : elle change tout le temps |
| l'objet entier | **jamais embarqué** | 16 tortues le partagent, il a sa vie propre |

Le critère opérationnel est un produit : **fréquence de mise à jour × nombre de copies à
propager**. Un nom d'habitat, c'est « rare × 16 » — on copie. Une température, c'est
« permanente × 16 » — on ne copie pas. À 100 000 copies, le calcul penche bien plus vite.

### Les deux sens du compromis, mesurés

> À jouer : `bruno/02-mapping`, requêtes 8 à 11.

| Opération | SQL | MongoDB |
|---|---|---|
| Lire les tortues d'un habitat **avec sa température** | **1** requête (`join fetch`) | **2** requêtes — la température n'est pas copiée |
| Changer la température | 1 update, 1 ligne | 1 `$set`, **1 document, 0 tortue** |
| Renommer un organisme de programme *(champ copié, ch. 6)* | 1 update, 2 lignes | 2 updates + **52 copies** |

Les deux premières lignes se lisent ensemble : côté document, on a **refusé de payer à
l'écriture** (aucune propagation quand la température bouge), donc on **paie à la lecture**
(une requête de plus pour l'afficher). La troisième ligne montre le choix inverse, pris sur
un autre champ de la même base.

Aucun des deux modèles ne gagne : ils déplacent le coût. Modéliser, c'est **choisir de quel
côté payer**, en connaissant ses lectures.

## Le cas `@DBRef`

`@DBRef` stocke `{ "$ref": "habitats", "$id": ObjectId(…) }` et Spring Data résout la
référence **à la lecture** — une requête de plus par document.

Jouez `/api/mongo/links/dbref` et comptez `roundTrips` : **20** pour vingt tortues.
Passez `?limit=60`, il en fait **60**. Le bon vieux « 1 + N », réimporté dans le monde
document.

Regardez surtout les `_id` demandés : le **même habitat est rechargé autant de fois qu'il
est référencé**. Aucun cache, aucun regroupement.

Pire : `@DBRef` **n'existe pas côté serveur**. Aucun `$lookup`, aucun filtre, aucun tri ne
peut porter sur l'objet lié.

**L'alternative recommandée** — la manual reference :

```java
@Field(targetType = FieldType.OBJECT_ID)   // ← indispensable
private String habitatId;
```

`targetType = OBJECT_ID` fait stocker un vrai `ObjectId` et non une chaîne. Sans lui, un
`$lookup` sur `habitats._id` ne trouverait jamais rien : les types ne correspondent pas.
C'est un piège classique, et silencieux — le pipeline renvoie simplement des tableaux vides.

On choisit alors explicitement :

* une deuxième requête (`/api/mongo/links/manual/{id}`) — 2 allers-retours **maîtrisés** ;
* ou un `$lookup` dans un pipeline (`/api/mongo/aggregation/habitats`) — 1 seul, côté serveur ;
* ou rien du tout, quand le nom dénormalisé suffit (`/api/mongo/turtles`) — 1 seul.

## Le cas plusieurs-à-plusieurs

> À jouer : `bruno/06-many-to-many`

Une tortue suit plusieurs **programmes de recherche**, un programme suit plusieurs
tortues. C'est la seule relation du projet qui, côté relationnel, produit une vraie
**table de jointure** :

```
turtle_program   turtle_id  BIGINT  FK → turtle.id     ┐ PRIMARY KEY
                 program_id BIGINT  FK → program.id    ┘ composite
```

Deux clés étrangères, une clé primaire composite, **aucune colonne de données**. C'est la
signature du motif — et le contre-exemple qui permet de comprendre `turtle_tag`, qui n'a
qu'une FK et pas de PK (voir [00-schema.md](00-schema.md)).

### Pourquoi les programmes, et pas les tags

Parce qu'un programme est une **entité** : il a ses propres attributs (organisme, année de
lancement), il existe même sans tortue inscrite, et on veut pouvoir le renommer d'un seul
UPDATE. Un tag n'est qu'une étiquette : il naît et meurt avec ses porteurs.

C'est le critère de décision, et il vaut aussi côté document.

### Côté MongoDB : la table de jointure n'existe pas

C'est le fait marquant du chapitre. MongoDB n'a pas la notion de table de jointure. Il
faut donc **choisir un côté** :

| Stratégie | Ce qu'on stocke | Quand |
|---|---|---|
| Référence nue | `programIds: [ObjectId]` | la relation suffit, on ne lit jamais les libellés sans aller chercher le programme |
| **Extended reference** *(retenue ici)* | `programs: [{ programId, name, organisation }]` | on affiche et on filtre sur les libellés en permanence |
| Référence des deux côtés | + `programs.turtleIds: [ObjectId]` | les deux sens sont critiques — au prix de deux écritures à garder cohérentes |

### L'extended reference, ou pourquoi dupliquer a du sens ici

Le tableau ne contient pas des identifiants nus, mais l'identifiant **plus une copie** des
deux champs qu'on lit tout le temps :

```javascript
"programs": [
  { "programId": ObjectId("…"), "name": "Argos Océan Indien",  "organisation": "CNRS" },
  { "programId": ObjectId("…"), "name": "Reef Watch Queensland", "organisation": "AIMS" }
]
```

Un programme a **quinze champs** : acronyme, responsable scientifique, budget, site web,
description de plusieurs lignes, protocole de terrain imbriqué, statut, années de début et
de fin… La copie n'en emporte que **deux**. C'est ce qui sépare la dénormalisation de la
duplication naïve : on ne recopie pas l'entité, on recopie **l'étiquette qu'on affiche**.

Pour le voir : `/api/mongo/programs/of-turtle/{id}` renvoie la vue complète d'un programme,
et **seuls trois champs sur quinze apparaissent** — les autres n'ont pas été recopiés, ils
sont restés dans la collection `programs`. Le jumeau SQL, lui, les rapporte tous, au prix d'une jointure de plus.

**Ce que la copie achète** — mesuré sur les endpoints :

| Question | Avec la copie | Sans (identifiants nus) |
|---|---|---|
| Les programmes d'une tortue | **1** requête | 2 (un `$in` pour les libellés) |
| Les tortues d'un programme (35) | **1** requête (`programs.name`) | 2 (résoudre le nom, puis filtrer) |
| **Les tortues du CNRS (52)** | **1** requête (`programs.organisation`) | 2, ou un `$lookup` |

Le cas du CNRS est le plus parlant : il **finance deux programmes** (ARGOI et STGP), donc
la question « toutes les tortues du CNRS » traverse deux programmes d'un coup. Un
identifiant de programme ne suffirait pas ; il faudrait les chercher tous les deux.

**Ce qu'elle coûte** — renommer un organisme :

```javascript
// 1. la source de vérité
db.programs.updateMany({ organisation: "CNRS" }, { $set: { organisation: "CNRS / IRD" } })

// 2. toutes les copies — et rien ne l'impose
db.turtles.updateMany(
  { "programs.organisation": "CNRS" },
  { $set: { "programs.$[e].organisation": "CNRS / IRD" } },
  { arrayFilters: [ { "e.organisation": "CNRS" } ] })
```

Deux écritures, **52 documents touchés**, et une fenêtre d'incohérence entre les deux.
Côté SQL, c'est `update program set organisation = ? where organisation = ?` : **deux
lignes**, et tout le monde voit le nouveau nom instantanément.

**Le critère** : on ne duplique que ce qui est **stable** et **lu souvent**. Un nom
d'organisme change une fois par décennie et s'affiche à chaque écran — bon candidat. Et on
garde toujours la référence à côté de la copie : `programId` reste la source de vérité.

Les chemins `programs.programId` et `programs.organisation` sont indexés : un index sur un
champ *à l'intérieur* d'un tableau est lui aussi **multikey**.

### Ce que ça change, mesuré

| Question | SQL | MongoDB |
|---|---|---|
| Les programmes d'une tortue | 2 requêtes (jointure `turtle_program` → `program`) | **1 requête** : le document les contient déjà |
| Les tortues d'un programme (35) | **1 requête**, 2 jointures — on filtre sur `program.name` | **1 requête** grâce au nom dupliqué (2 par la référence seule) |
| Les tortues du CNRS (52) | **1 requête**, 2 jointures, rien de dupliqué | **1 requête**, 0 jointure — parce que l'organisme est dupliqué |
| Renommer un organisme | **1 UPDATE**, 2 lignes | 2 updates : 2 documents source, puis **52 copies** |
| Inscrire une tortue | 1 `INSERT` dans la table de jointure | 1 `$addToSet` sur la tortue |
| Combien de tortues par programme | `GROUP BY` sur la table de jointure | `$lookup` depuis `programs`, dont le `foreignField` est un tableau |

Deux enseignements :

1. **La table de jointure sert les deux sens à égalité** — la relation est stockée *entre*
   les deux entités. Le modèle document, lui, oblige à privilégier un sens. Ici « les
   programmes d'une tortue » est gratuit ; si le besoin dominant était l'inverse, on
   stockerait `turtleIds` côté programme.
2. **La jointure sait filtrer sur n'importe quel attribut de l'autre table**
   (`where program.organisation = ?`) sans que rien n'ait été prévu à la modélisation. Le
   modèle document ne peut le faire en une requête que sur les champs qu'il a **choisi de
   dupliquer** — d'où l'importance de connaître ses lectures avant de modéliser.

### Ce que la contrainte garantissait, et qui devient votre travail

* La clé primaire composite empêchait la double inscription → côté document, c'est
  `$addToSet` qui joue ce rôle.
* La clé étrangère empêchait de référencer un programme inexistant → **rien** ne
  l'empêche côté document.
* La suppression d'un programme nettoyait la table de jointure (`ON DELETE CASCADE`) →
  côté document, il faut un `$pull` sur toutes les tortues concernées, à écrire soi-même.

## Le prix des relations, chiffré

| Appel | `roundTrips` |
|---|---|
| `/api/sql/turtles?limit=20` (lazy loading) | **49** |
| `/api/sql/turtles?limit=50` | **113** |
| `/api/sql/turtles?limit=121` (toute la table) | **255** |
| `/api/mongo/turtles?limit=20` · `50` | **1** · **1** |
| `/api/mongo/turtles?limit=121` | **2** |
| `/api/sql/turtles/{id}` | 4 |
| `/api/sql/turtles/{id}/fetch-join` | 2 |
| `/api/mongo/links/embedded/{id}` | 1 |
| `/api/mongo/links/manual/{id}` | 2 |

Le coût SQL est **linéaire** : doublez `?limit`, le compteur double. Côté document il ne
bouge pas — sauf à 121, où il passe à 2 : le curseur MongoDB renvoie **101 documents par
lot**, et il faut un `getMore` pour le reste. Une belle occasion de parler de curseurs.

> **Le point à retenir** : JPA vous laisse modéliser d'abord et découvrir le coût des
> jointures ensuite. MongoDB vous force à trancher au moment de la modélisation — en
> partant des **lectures** que fera l'application, pas de la théorie des formes normales.
