# Moteur de recommandation TechMarket

Projet Scala 3. Un moteur de recommandation écrit en programmation fonctionnelle.

## Le problème

TechMarket vend des produits high-tech en ligne. Le catalogue est grand et il
grossit chaque trimestre. Du coup, les visiteurs s'y perdent.

Trois conséquences :

- ils quittent les fiches produit sans rien faire
- le panier moyen n'augmente pas
- les nouveaux visiteurs ne voient aucune suggestion, car on ne sait rien d'eux

L'équipe Data veut un prototype pour mesurer ce que rapporterait un bloc
« Vous pourriez aussi aimer ». Le moteur doit :

- faire cliquer davantage sur les suggestions
- augmenter le panier moyen
- proposer quand même quelque chose aux nouveaux visiteurs
- expliquer chaque recommandation, pour qu'une équipe métier la comprenne

Trois contraintes : le prototype tourne hors-ligne sur des données anonymisées,
il donne les mêmes résultats à chaque exécution, et il reste simple à lire.

## Les données

Fournies avec le sujet, dans le dossier `data/` :

- `items.csv` : 18 produits (id, nom, catégorie, prix)
- `users.csv` : 20 utilisateurs (id, segment, date d'inscription)
- `interactions.csv` : 82 interactions (vues, achats, notes)

Le segment d'un utilisateur vaut « nouveau », « occasionnel » ou « fidele ».

## Lancer le projet

```bash
sbt compile
sbt test
```

## Versions utilisées

| Outil | Version |
|---|---|
| JDK | 21 |
| SBT | 1.12.4 |
| Scala | 3.8.1 |
