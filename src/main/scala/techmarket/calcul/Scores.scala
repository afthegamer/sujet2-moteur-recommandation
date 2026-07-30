package techmarket.calcul

import techmarket.modele.*

object Scores:

  def poids(typeInteraction: TypeInteraction): Double =
    typeInteraction match
      case TypeInteraction.Vue          => 1.0
      case TypeInteraction.Achat        => 5.0
      case TypeInteraction.Note(valeur) => valeur.toDouble

  def poidsParItem(interactions: List[Interaction]): Map[ItemId, Double] =
    interactions.groupMapReduce(_.itemId)(i => poids(i.typeInteraction))(_ max _)

  def profil(interactions: List[Interaction], itemIds: Vector[ItemId]): Vector[Double] =
    val parItem = poidsParItem(interactions)
    itemIds.map(id => parItem.getOrElse(id, 0.0))

  def popularite(interactions: List[Interaction]): Map[ItemId, Double] =
    interactions.groupMapReduce(_.itemId)(i => poids(i.typeInteraction))(_ + _)
