# Moteur de recommandation TechMarket

Projet tutoré Scala — mini moteur de recommandation en programmation fonctionnelle.

## Problématique reformulée

TechMarket vend des produits high-tech en ligne. Son catalogue compte plusieurs centaines de références et s'agrandit chaque trimestre, si bien que **les visiteurs s'y perdent** : le taux de rebond sur les fiches produit est élevé, le panier moyen stagne, et un visiteur sans historique ne reçoit aucune suggestion.

L'équipe Data veut un **prototype** pour mesurer ce qu'apporterait une fonctionnalité « Vous pourriez aussi aimer », avant d'investir dans une solution industrielle.

Traduit en objectifs techniques :

| Objectif métier | Traduction technique |
|---|---|
| Augmenter le taux de clic sur les suggestions | Trier les candidats par score de pertinence décroissant, ne garder que le top-N |
| Augmenter le panier moyen | Recommander des produits proches de ceux déjà consultés ou achetés |
| Servir les nouveaux visiteurs (*cold start*) | Repli sur les items les plus populaires quand l'historique est insuffisant |
| Recommandations explicables | Chaque `Recommandation` porte une `justification` lisible par un non-technicien |

Trois contraintes encadrent le prototype : il tourne **hors-ligne** sur un extrait anonymisé, ses résultats sont **reproductibles et testables** (aucun service externe), et il doit rester **maintenable** — la lisibilité prime sur la sophistication algorithmique.

## Jeu de données

Fourni avec le sujet, dans `data/` :

| Fichier | Contenu | Volume |
|---|---|---|
| `items.csv` | `item_id, nom, categorie, prix_eur` | 18 produits, 6 catégories |
| `users.csv` | `user_id, segment, date_inscription` | 20 utilisateurs (7 fidèles, 7 nouveaux, 6 occasionnels) |
| `interactions.csv` | `interaction_id, user_id, item_id, type, note, date` | 82 interactions : 46 vues, 25 notes, 11 achats |

**Point d'attention relevé à l'analyse** : l'énoncé décrit le cold start comme le cas d'un « utilisateur sans historique », mais dans les données **aucun utilisateur n'a zéro interaction** — les 7 utilisateurs du segment `nouveau` en ont exactement 2 chacun. Le repli est donc déclenché par le **segment**, et non par un historique vide : autrement, le cas ne serait jamais atteint sur le jeu fourni.

## Architecture

Quatre couches, du cœur pur vers la périphérie (*functional core, imperative shell*) :

```
techmarket/
├── modele/         entités immuables : User, Item, Interaction, Recommandation
├── calcul/         fonctions pures de scoring et de similarité
├── orchestration/  pipeline scoring → filtrage → tri → top-N
└── io/             chargement des CSV et affichage, seule couche impure
```

La couche `calcul` ne connaît ni le format CSV ni les fichiers : elle opère sur des `Vector[Double]` et des entités du domaine, ce qui la rend testable sans données réelles.

## Lancer le projet

```bash
sbt compile
sbt run
sbt test
```

`sbt run` affiche les recommandations et écrit un fichier `recommandations.json`
à la racine du projet.

## Prérequis

| Outil | Version |
|---|---|
| JDK | 21 |
| SBT | 1.12.4 |
| Scala | 3.8.1 |

## Organisation

Projet réalisé en équipe. Développement incrémental en quatre séances : cadrage et modélisation, cœur fonctionnel, intégration du pipeline, finalisation.
