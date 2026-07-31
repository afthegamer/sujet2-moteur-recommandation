package techmarket

import techmarket.modele.*
import techmarket.orchestration.MoteurRecommandation

class MoteurRecommandationTest extends munit.FunSuite:

  private val tolerance = 1e-9

  private def recommander(userId: UserId, topN: Int = 10) =
    MoteurRecommandation.recommander(Fixtures.catalogue, userId, topN)

  test("un utilisateur inconnu produit une erreur"):
    assert(recommander(999).isLeft)

  test("segment « nouveau » sans voisin de goût : repli populaire (Chloé)"):
    val recommandations = recommander(Fixtures.chloe.id).getOrElse(Nil)
    assert(recommandations.nonEmpty)
    assert(recommandations.forall(_.justification.contains("populaires")))

  test("segment « nouveau » AVEC voisins de goût : repli quand même (David)"):
    val recommandations = recommander(Fixtures.david.id).getOrElse(Nil)
    assert(recommandations.nonEmpty)
    assert(
      recommandations.forall(_.justification.contains("populaires")),
      "le segment « nouveau » doit forcer le repli même quand des voisins existent"
    )

  test("le repli suit l'ordre de popularité"):
    assertEquals(
      recommander(Fixtures.chloe.id).getOrElse(Nil).map(_.item.id),
      List(Fixtures.clavier.id, Fixtures.ecran.id)
    )

  test("un non-nouveau sans candidat inédit bascule aussi sur le repli (Bob)"):
    val recommandations = recommander(Fixtures.bob.id).getOrElse(Nil)
    assertEquals(recommandations.map(_.item.id), List(Fixtures.souris.id))
    assert(recommandations.forall(_.justification.contains("populaires")))

  test("Alice reçoit l'écran via Bob, avec le score exact cos x poids"):
    val recommandations = recommander(Fixtures.alice.id).getOrElse(Nil)
    assertEquals(recommandations.map(_.item.id), List(Fixtures.ecran.id))
    assertEqualsDouble(recommandations.head.score, 5.0 / math.sqrt(2.0), tolerance)

  test("la justification personnalisée porte le nombre exact de voisins"):
    val recommandations = recommander(Fixtures.alice.id).getOrElse(Nil)
    assert(
      recommandations.head.justification.startsWith("1 utilisateur(s)"),
      s"justification obtenue : ${recommandations.head.justification}"
    )

  test("un voisin qui a acheté PUIS noté le même item ne compte qu'une fois"):
    val c1 = Item(1, "c1", "cat", 10.0)
    val c2 = Item(2, "c2", "cat", 10.0)
    val a  = User(1, Segment.Fidele, Fixtures.date)
    val b  = User(2, Segment.Occasionnel, Fixtures.date)
    val catalogue = Catalogue(
      List(c1, c2),
      List(a, b),
      List(
        Interaction(1, a.id, c1.id, TypeInteraction.Achat, Fixtures.date),
        Interaction(2, b.id, c1.id, TypeInteraction.Achat, Fixtures.date),
        Interaction(3, b.id, c2.id, TypeInteraction.Achat, Fixtures.date),
        Interaction(4, b.id, c2.id, TypeInteraction.Note(4), Fixtures.date)
      )
    )
    val recommandations = MoteurRecommandation.recommander(catalogue, a.id, 10).getOrElse(Nil)
    assertEquals(recommandations.map(_.item.id), List(c2.id))
    assertEqualsDouble(recommandations.head.score, 5.0 / math.sqrt(2.0), tolerance)

  test("plusieurs candidats personnalisés : tri décroissant et top-N respectés"):
    val c1 = Item(1, "c1", "cat", 10.0)
    val c2 = Item(2, "c2", "cat", 10.0)
    val c3 = Item(3, "c3", "cat", 10.0)
    val a  = User(1, Segment.Fidele, Fixtures.date)
    val b  = User(2, Segment.Occasionnel, Fixtures.date)
    val catalogue = Catalogue(
      List(c1, c2, c3),
      List(a, b),
      List(
        Interaction(1, a.id, c1.id, TypeInteraction.Achat, Fixtures.date),
        Interaction(2, b.id, c1.id, TypeInteraction.Achat, Fixtures.date),
        Interaction(3, b.id, c2.id, TypeInteraction.Note(5), Fixtures.date),
        Interaction(4, b.id, c3.id, TypeInteraction.Vue, Fixtures.date)
      )
    )
    val tous = MoteurRecommandation.recommander(catalogue, a.id, 10).getOrElse(Nil)
    assertEquals(tous.map(_.item.id), List(c2.id, c3.id))
    assert(tous.head.score > tous(1).score)
    assertEquals(
      MoteurRecommandation.recommander(catalogue, a.id, 1).getOrElse(Nil).map(_.item.id),
      List(c2.id)
    )

  test("scores ex aequo : départage déterministe par identifiant croissant"):
    val iA = Item(1, "iA", "cat", 10.0)
    val iB = Item(2, "iB", "cat", 10.0)
    val nouveau = User(1, Segment.Nouveau, Fixtures.date)
    val u2      = User(2, Segment.Occasionnel, Fixtures.date)
    val u3      = User(3, Segment.Occasionnel, Fixtures.date)
    val catalogue = Catalogue(
      List(iA, iB),
      List(nouveau, u2, u3),
      List(
        Interaction(1, u2.id, iA.id, TypeInteraction.Vue, Fixtures.date),
        Interaction(2, u3.id, iB.id, TypeInteraction.Vue, Fixtures.date)
      )
    )
    assertEquals(
      MoteurRecommandation.recommander(catalogue, nouveau.id, 10).getOrElse(Nil).map(_.item.id),
      List(iA.id, iB.id)
    )

  test("les items déjà connus de l'utilisateur sont exclus"):
    Fixtures.users.map(_.id).foreach: userId =>
      val dejaConnus = Fixtures.catalogue.itemsConnusDe(userId)
      val proposes = recommander(userId).getOrElse(Nil).map(_.item.id).toSet
      assertEquals(proposes.intersect(dejaConnus), Set.empty[ItemId], s"user $userId")

  test("chaque recommandation porte une justification non vide"):
    val toutes = Fixtures.users.flatMap(u => recommander(u.id).getOrElse(Nil))
    assert(toutes.nonEmpty)
    assert(toutes.forall(_.justification.trim.nonEmpty))

  test("le moteur est déterministe : deux appels donnent le même résultat"):
    assertEquals(recommander(Fixtures.alice.id), recommander(Fixtures.alice.id))
