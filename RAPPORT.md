# Rapport de projet — Moteur de recommandation TechMarket

Projet tutoré Scala — programmation fonctionnelle
Dépôt : <https://github.com/afthegamer/sujet2-moteur-recommandation>

---

## 1. La problématique et notre réponse

TechMarket vend des produits high-tech en ligne. Son catalogue est grand et il
grossit chaque trimestre. Les visiteurs s'y perdent : ils quittent les fiches
produit sans rien faire, le panier moyen n'augmente pas, et les nouveaux
visiteurs ne voient aucune suggestion puisqu'on ne sait rien d'eux.

L'équipe Data voulait un prototype pour mesurer ce que rapporterait un bloc
« Vous pourriez aussi aimer », avant d'investir dans une vraie solution.

Voici comment chaque objectif se traduit dans notre code :

| Objectif de TechMarket | Ce que fait le moteur |
|---|---|
| Faire cliquer sur les suggestions | On cherche les utilisateurs qui ont les mêmes goûts, et on propose ce qu'ils ont aimé |
| Augmenter le panier moyen | On ne propose que des produits que l'utilisateur n'a jamais vus ni achetés |
| Servir les nouveaux visiteurs | Repli automatique sur les produits les plus populaires |
| Rester explicable | Chaque suggestion est accompagnée d'une phrase qui dit pourquoi elle est là |

Les trois contraintes sont respectées. Le moteur tourne hors-ligne sur les CSV
fournis, sans aucun service externe. Il donne les mêmes résultats à chaque
exécution. Et le code reste court : chaque couche tient dans un ou deux fichiers.

## 2. Le jeu de données

Trois fichiers CSV fournis avec le sujet, rangés dans `data/` :

- `items.csv` : 18 produits répartis en 6 catégories
- `users.csv` : 20 utilisateurs, dont 7 fidèles, 7 nouveaux et 6 occasionnels
- `interactions.csv` : 82 interactions, soit 46 vues, 25 notes et 11 achats

**Un point important trouvé en lisant les données.** Le sujet parle du cold start
comme du cas d'un « utilisateur sans historique ». Mais dans les fichiers, aucun
utilisateur n'a zéro interaction : les 7 utilisateurs « nouveau » en ont
exactement 2 chacun. Si on avait déclenché le repli sur un historique vide, il
ne se serait jamais produit. On le déclenche donc sur le **segment**.

## 3. L'architecture

Le projet est découpé en quatre couches. Le cœur est pur, les entrées-sorties
sont mises à part.

```
src/main/scala/techmarket/
├── modele/          Item, User, Interaction, Recommandation, Catalogue
├── calcul/          Similarite (cosinus), Scores (poids, profils, popularité)
├── orchestration/   MoteurRecommandation (le pipeline)
├── io/              ChargeurCsv (lecture des CSV), Affichage (sortie console)
└── Main.scala       point d'entrée
```

Deux règles ont guidé ce découpage :

1. **La couche `calcul` ne sait pas d'où viennent les données.** Elle ne
   manipule que des vecteurs de nombres et des entités. On peut donc la tester
   sans lire un seul fichier.
2. **Seule la couche `io` touche au disque.** Le `Main` se contente de charger,
   d'appeler le moteur et d'afficher.

## 4. Comment marche le moteur

### La mesure de similarité

On compare deux utilisateurs avec la **similarité cosinus** :

```
cos(u, v) = (u · v) / (‖u‖ × ‖v‖)
```

C'est une fonction pure, de signature `(Vector[Double], Vector[Double]) => Double`.
Elle vaut 0 quand l'angle n'a pas de sens : vecteurs vides, de tailles
différentes, ou nuls.

**Pourquoi le cosinus.** Il ne regarde pas *combien* un utilisateur est actif,
mais *dans quelle direction* vont ses goûts. Un client fidèle très actif et un
occasionnel discret qui aiment les mêmes produits sont donc considérés comme
proches. C'est ce qu'il faut pour l'objectif « taux de clic » : on cherche des
gens qui ont les mêmes goûts, pas des gros acheteurs. La similarité de Jaccard
aurait ignoré la force du signal (une vue aurait compté autant qu'un achat), et
une distance euclidienne aurait séparé deux personnes de même goût simplement
parce que l'une est plus active.

### Du clic au nombre

Chaque interaction reçoit un poids : une vue vaut 1, une note vaut sa valeur
(de 1 à 5), un achat vaut 5. Le **profil** d'un utilisateur est un vecteur avec
une case par produit.

Quand quelqu'un a plusieurs interactions sur le même produit, on garde **le
signal le plus fort**, pas la somme. Sinon, une personne qui consulte dix fois
la même fiche pèserait plus lourd qu'un acheteur.

La **popularité** d'un produit, elle, additionne les signaux de tout le monde.
Elle ne dépend d'aucun utilisateur en particulier, ce qui la rend utilisable
pour un visiteur qu'on ne connaît pas.

### Le pipeline

1. **Scoring** — on calcule la similarité entre l'utilisateur et tous les
   autres, et on garde ceux dont la similarité est positive. Le score d'un
   produit est la somme, sur ces voisins, de leur similarité multipliée par leur
   intérêt pour ce produit.
2. **Filtrage** — on retire les produits que l'utilisateur connaît déjà.
3. **Tri** — score décroissant. En cas d'égalité, on départage par
   identifiant : c'est ce qui garantit le même résultat à chaque exécution.
4. **Top-N** — on garde les N premiers et on construit les recommandations avec
   leur justification.

**Le cold start** est porté par le type `Option`. La fonction de personnalisation
renvoie `None` dans deux cas : l'utilisateur est du segment « nouveau », ou
aucun voisin n'apporte de produit inédit. Un `getOrElse` bascule alors sur les
produits populaires. L'absence de personnalisation est donc une valeur normale
du programme, pas une erreur.

## 5. Les principes fonctionnels appliqués

| Principe demandé | Où on le voit |
|---|---|
| Immutabilité | Que des `val` et des `case class`. Les méthodes `avecItem`, `avecUser`, `avecInteraction` renvoient un nouveau `Catalogue` au lieu de modifier l'ancien |
| Fonctions pures | Tout `modele`, `calcul` et `orchestration` : mêmes entrées, mêmes sorties, aucun effet de bord |
| Fonctions d'ordre supérieur | `map`, `filter`, `filterNot`, `flatMap`, `sortBy`, `take`, `groupMapReduce`, `partitionMap`. Aucun `var` ni `while` dans tout le projet |
| ADT et pattern matching | `enum Segment` et `enum TypeInteraction`. `Note` porte sa valeur, donc une note sans valeur est impossible à écrire |
| Erreurs sans exception | `Option` pour les recherches, `Either` pour le parsing et l'utilisateur inconnu, `Try` pour la lecture de fichier |
| Cœur pur, bords impurs | `Main.scala` fait moins de 30 lignes : charger, appeler, afficher |

Sur le dernier point, le chargement des CSV mérite un mot. Aucune exception ne
remonte : si un fichier est illisible ou une ligne mal formée, on obtient un
`Left` qui dit quel fichier et quelle ligne posent problème. Et toutes les
erreurs d'un fichier sont rassemblées d'un coup, pas seulement la première.

## 6. Les résultats

Sortie de `sbt run`, avec un utilisateur de chaque segment et un top-3 :

```
=== Recommandations ===

Utilisateur 2 — segment nouveau
  1. Laptop UltraBook 14            1099.00 EUR   score 28.00
     Produit parmi les plus populaires du catalogue (historique insuffisant pour personnaliser)
  2. Webcam StreamCam HD              79.00 EUR   score 18.00
     Produit parmi les plus populaires du catalogue (historique insuffisant pour personnaliser)
  3. Batterie externe 20000mAh        39.90 EUR   score 16.00
     Produit parmi les plus populaires du catalogue (historique insuffisant pour personnaliser)

Utilisateur 1 — segment occasionnel
  1. Batterie externe 20000mAh        39.90 EUR   score 1.91
     2 utilisateur(s) au profil proche du vôtre se sont intéressés à ce produit
  2. Webcam StreamCam HD              79.00 EUR   score 1.37
     1 utilisateur(s) au profil proche du vôtre se sont intéressés à ce produit
  3. Chargeur rapide USB-C 65W        29.90 EUR   score 0.86
     2 utilisateur(s) au profil proche du vôtre se sont intéressés à ce produit

Utilisateur 4 — segment fidèle
  1. Batterie externe 20000mAh        39.90 EUR   score 1.25
     2 utilisateur(s) au profil proche du vôtre se sont intéressés à ce produit
  2. Webcam StreamCam HD              79.00 EUR   score 0.88
     1 utilisateur(s) au profil proche du vôtre se sont intéressés à ce produit
  3. Chargeur rapide USB-C 65W        29.90 EUR   score 0.65
     2 utilisateur(s) au profil proche du vôtre se sont intéressés à ce produit
```

Ce qu'on lit : l'utilisateur 2, qui est nouveau, reçoit les valeurs sûres du
catalogue. Les deux autres reçoivent des accessoires complémentaires, portés par
des gens qui ont les mêmes goûts qu'eux. Chaque ligne dit pourquoi elle est là.

**Tests : 46, tous verts** (`sbt test`).

## 7. Les difficultés rencontrées

**Le cold start ne pouvait pas marcher comme prévu.** On avait d'abord compris
qu'il fallait tester si l'historique était vide, comme le suggère l'énoncé. En
regardant les données, on a vu qu'aucun utilisateur n'était dans ce cas. Il a
fallu relire le jeu de données avant de coder et changer le déclencheur pour le
segment.

**Un produit pouvait compter double dans le score.** Au début, on additionnait
les contributions interaction par interaction. Un voisin qui avait acheté puis
noté le même produit comptait donc deux fois, alors que la règle du profil dit
de garder seulement le signal le plus fort. Le bug ne se voyait pas : les tests
passaient, parce qu'aucun cas de test n'avait deux interactions sur un même
produit. On l'a trouvé en recalculant les scores à la main sur les vraies
données. Il changeait l'ordre du top-3 pour 6 utilisateurs sur 13.

**Des tests verts qui ne prouvaient rien.** On a vérifié en cassant le code
exprès : on pouvait supprimer la gestion du segment ou la pondération par
similarité sans qu'aucun test échoue. On a donc ajouté un cas de contraste, un
utilisateur « nouveau » qui *a* des voisins, et des vérifications sur les valeurs
exactes des scores.

**Les versions de Scala et sbt.** Le projet a démarré en Scala 3.3.1 avec sbt
1.9.9, puis a été passé en 3.8.1 et 1.12.4. Il a fallu vérifier que tout
compilait et que les tests passaient toujours après ce changement.

**Le travail à plusieurs sur le même dépôt.** Le moteur a été ajouté par un
membre pendant que d'autres travaillaient sur l'affichage. Il est arrivé sans
tests, ce qui a été rattrapé ensuite. C'est ce qui nous a appris à regarder ce
qui est déjà poussé avant de pousser à notre tour.

## 8. L'organisation de l'équipe

Quatre personnes ont travaillé sur le projet. La répartition se lit directement
dans l'historique Git.

| Membre | Ce qu'il a fait | Commits |
|---|---|---|
| **Brad Dos Santos Patatas** | Séance 1 : structure sbt, prise en main des données, modèle du domaine, README de cadrage. Puis les tests du moteur en séance 3 | `720a694`, `8a2f9c4` |
| **Robin** | Séance 2 : le cœur fonctionnel — similarité cosinus, poids et profils, chargement des CSV, et les tests de ces trois modules | `f6f598b` |
| **Mathis** | Séance 3 : le point d'entrée `Main` et la couche d'affichage, puis la mise à jour du README | `1bdad81`, `f8ec836`, `b3686b3`, `b87ad00` |
| **diallosidymohamed** | Séance 3 : le moteur de recommandation, c'est-à-dire le pipeline complet | `43c1229` |

Le découpage en couches a rendu ce travail en parallèle possible. Comme la
couche `calcul` ne dépend de rien d'autre que du modèle, elle a pu être écrite
pendant que le modèle était déjà figé. De même, l'affichage et le moteur ont été
développés en même temps par deux personnes différentes, chacun de son côté.

## 9. Limites et suites possibles

**Les notes basses ne pénalisent pas.** Une note de 1 ou 2 sur 5 compte comme un
intérêt faible, pas comme un rejet. Le signal « je n'aime pas » n'est donc pas
utilisé. Sur les données fournies, cela ne change aucune recommandation
affichée : il n'y a que deux notes en dessous de 3. Mais c'est la première chose
à corriger pour un vrai usage, par exemple en centrant les notes autour de 3.

**Le calcul est quadratique.** On compare chaque utilisateur à tous les autres.
Pour 20 profils hors-ligne, c'est sans importance. Au-delà, il faudrait
pré-calculer les voisinages.

**Les extensions optionnelles n'ont pas été faites.** Le sujet propose un
scoring hybride, une mesure de précision, un export JSON et un indicateur
d'impact. Nous avons préféré livrer exactement ce qui était demandé, avec des
tests solides, plutôt que d'ajouter des fonctionnalités moins maîtrisées.
