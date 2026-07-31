package techmarket.orchestration

import techmarket.calcul.{Scores, Similarite}
import techmarket.modele.*

/** Pipeline de recommandation : scoring, filtrage, tri, sélection du top-N.
  *
  * Entièrement pur : à catalogue identique, les recommandations produites sont
  * identiques. Aucune lecture de fichier, aucun affichage.
  */
object MoteurRecommandation:

  /** Recommandations pour un utilisateur.
    *
    * Renvoie `Left` si l'utilisateur est inconnu du catalogue — une erreur
    * d'appel, à distinguer d'un utilisateur connu sans recommandation.
    */
  def recommander(
      catalogue: Catalogue,
      userId: UserId,
      topN: Int
  ): Either[String, List[Recommandation]] =
    catalogue
      .user(userId)
      .toRight(s"Utilisateur inconnu : $userId")
      .map: utilisateur =>
        personnalisees(catalogue, utilisateur, topN)
          .getOrElse(populaires(catalogue, utilisateur, topN))

  /** Filtrage collaboratif basé utilisateur.
    *
    * Renvoie `None` lorsque la personnalisation n'est pas possible : segment
    * « nouveau », ou aucun voisin de goût exploitable. C'est ce `None` qui
    * déclenche le repli sur la popularité.
    *
    * Le déclenchement se fait sur le *segment* et non sur un historique vide :
    * dans le jeu de données TechMarket, tous les utilisateurs ont au moins deux
    * interactions, donc un test sur l'historique vide ne se produirait jamais.
    */
  private def personnalisees(
      catalogue: Catalogue,
      utilisateur: User,
      topN: Int
  ): Option[List[Recommandation]] =
    if utilisateur.segment == Segment.Nouveau then None
    else
      val itemIds = catalogue.items.map(_.id).toVector
      val profilCible = Scores.profil(catalogue.interactionsDe(utilisateur.id), itemIds)

      val voisins = catalogue.users
        .filter(_.id != utilisateur.id)
        .map: autre =>
          val profilAutre = Scores.profil(catalogue.interactionsDe(autre.id), itemIds)
          (autre, Similarite.cosinus(profilCible, profilAutre))
        .filter((_, similarite) => similarite > 0.0)

      val dejaConnus = catalogue.itemsConnusDe(utilisateur.id)

      /* Score d'un item : somme, sur les voisins qui s'y sont intéressés, de
         leur similarité pondérée par l'intensité de leur intérêt. L'intensité
         retenue est le signal le plus fort du voisin sur l'item (même règle que
         dans le profil) : un voisin qui a vu, acheté ET noté un même produit
         ne compte qu'une fois, pas trois. */
      val scoresParItem = voisins
        .flatMap: (voisin, similarite) =>
          Scores
            .poidsParItem(catalogue.interactionsDe(voisin.id))
            .toList
            .map((itemId, poidsMax) => (itemId, similarite * poidsMax))
        .groupMapReduce(_._1)(_._2)(_ + _)

      val nbVoisinsParItem = voisins
        .flatMap((voisin, _) => catalogue.itemsConnusDe(voisin.id).map(id => (id, voisin.id)))
        .groupMap(_._1)(_._2)
        .view
        .mapValues(_.distinct.size)
        .toMap

      val resultats = scoresParItem.toList
        .filterNot((itemId, _) => dejaConnus.contains(itemId))
        .flatMap((itemId, score) => catalogue.item(itemId).map(item => (item, score)))
        .sortBy((item, score) => (-score, item.id))
        .take(topN)
        .map: (item, score) =>
          val nbVoisins = nbVoisinsParItem.getOrElse(item.id, 0)
          Recommandation(
            item,
            score,
            s"$nbVoisins utilisateur(s) au profil proche du vôtre se sont intéressés à ce produit"
          )

      if resultats.isEmpty then None else Some(resultats)

  /** Repli du cold start : les items les plus populaires du catalogue,
    * hors produits déjà connus de l'utilisateur.
    */
  private def populaires(
      catalogue: Catalogue,
      utilisateur: User,
      topN: Int
  ): List[Recommandation] =
    val dejaConnus = catalogue.itemsConnusDe(utilisateur.id)

    Scores
      .popularite(catalogue.interactions)
      .toList
      .filterNot((itemId, _) => dejaConnus.contains(itemId))
      .flatMap((itemId, score) => catalogue.item(itemId).map(item => (item, score)))
      .sortBy((item, score) => (-score, item.id))
      .take(topN)
      .map: (item, score) =>
        Recommandation(
          item,
          score,
          "Produit parmi les plus populaires du catalogue (historique insuffisant pour personnaliser)"
        )
