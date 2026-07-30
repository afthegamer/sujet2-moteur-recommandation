package techmarket.modele

import java.time.LocalDate

type UserId = Int
type ItemId = Int

enum Segment:
  case Nouveau, Occasionnel, Fidele

object Segment:
  def depuisLibelle(libelle: String): Option[Segment] =
    libelle.trim.toLowerCase match
      case "nouveau"           => Some(Segment.Nouveau)
      case "occasionnel"       => Some(Segment.Occasionnel)
      case "fidele" | "fidèle" => Some(Segment.Fidele)
      case _                   => None

enum TypeInteraction:
  case Vue
  case Achat
  case Note(valeur: Int)

final case class Item(
    id: ItemId,
    nom: String,
    categorie: String,
    prixEur: Double
)

final case class User(
    id: UserId,
    segment: Segment,
    dateInscription: LocalDate
)

final case class Interaction(
    id: Int,
    userId: UserId,
    itemId: ItemId,
    typeInteraction: TypeInteraction,
    date: LocalDate
)

final case class Recommandation(
    item: Item,
    score: Double,
    justification: String
)
