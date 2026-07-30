package techmarket

import techmarket.modele.*

import java.time.LocalDate

object Fixtures:

  val date: LocalDate = LocalDate.of(2025, 1, 1)

  val clavier = Item(1, "Clavier", "Accessoires", 50.0)
  val souris  = Item(2, "Souris", "Accessoires", 30.0)
  val ecran   = Item(3, "Écran", "Périphériques", 200.0)

  val items: List[Item] = List(clavier, souris, ecran)

  val alice = User(1, Segment.Fidele, date)
  val bob   = User(2, Segment.Occasionnel, date)
  val chloe = User(3, Segment.Nouveau, date)
  val david = User(4, Segment.Nouveau, date)

  val users: List[User] = List(alice, bob, chloe, david)

  val interactions: List[Interaction] = List(
    Interaction(1, alice.id, clavier.id, TypeInteraction.Achat, date),
    Interaction(2, bob.id, clavier.id, TypeInteraction.Achat, date),
    Interaction(3, bob.id, ecran.id, TypeInteraction.Note(5), date),
    Interaction(4, chloe.id, souris.id, TypeInteraction.Vue, date),
    Interaction(5, david.id, clavier.id, TypeInteraction.Vue, date)
  )

  val catalogue: Catalogue = Catalogue(items, users, interactions)
