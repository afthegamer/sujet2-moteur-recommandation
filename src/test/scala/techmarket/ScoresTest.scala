package techmarket

import techmarket.calcul.Scores
import techmarket.modele.*

class ScoresTest extends munit.FunSuite:

  private val tolerance = 1e-9

  test("le poids reflète l'intensité : vue=1, note=sa valeur, achat=5"):
    assertEqualsDouble(Scores.poids(TypeInteraction.Vue), 1.0, tolerance)
    assertEqualsDouble(Scores.poids(TypeInteraction.Achat), 5.0, tolerance)
    assertEqualsDouble(Scores.poids(TypeInteraction.Note(4)), 4.0, tolerance)
    assertEqualsDouble(Scores.poids(TypeInteraction.Note(5)), Scores.poids(TypeInteraction.Achat), tolerance)
    assertEqualsDouble(Scores.poids(TypeInteraction.Note(1)), Scores.poids(TypeInteraction.Vue), tolerance)

  test("le profil place chaque item à sa coordonnée, 0 si aucune interaction"):
    val itemIds = Vector(1, 2, 3)
    val profil = Scores.profil(Fixtures.interactions.filter(_.userId == Fixtures.bob.id), itemIds)
    assertEquals(profil, Vector(5.0, 0.0, 5.0))

  test("le profil d'un utilisateur sans interaction est le vecteur nul"):
    assertEquals(Scores.profil(Nil, Vector(1, 2, 3)), Vector(0.0, 0.0, 0.0))

  test("le profil retient le signal le plus fort, pas leur somme"):
    val interactions = List(
      Interaction(1, 1, 1, TypeInteraction.Vue, Fixtures.date),
      Interaction(2, 1, 1, TypeInteraction.Achat, Fixtures.date)
    )
    assertEquals(Scores.profil(interactions, Vector(1)), Vector(5.0))

  test("la popularité cumule les signaux de tous les utilisateurs"):
    val popularite = Scores.popularite(Fixtures.interactions)
    assertEqualsDouble(popularite(Fixtures.clavier.id), 11.0, tolerance)
    assertEqualsDouble(popularite(Fixtures.ecran.id), 5.0, tolerance)
    assertEqualsDouble(popularite(Fixtures.souris.id), 1.0, tolerance)
