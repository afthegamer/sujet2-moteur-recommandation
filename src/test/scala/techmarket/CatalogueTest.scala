package techmarket

import techmarket.modele.*

class CatalogueTest extends munit.FunSuite:

  test("ajouter une interaction ne modifie pas le catalogue d'origine"):
    val avant = Fixtures.catalogue
    val taille = Fixtures.interactions.size
    val nouvelle = Interaction(99, Fixtures.alice.id, Fixtures.souris.id, TypeInteraction.Vue, Fixtures.date)

    val apres = avant.avecInteraction(nouvelle)

    assertEquals(avant.interactions.size, taille, "le catalogue initial doit être intact")
    assertEquals(apres.interactions.size, taille + 1)
    assert(apres.interactions.contains(nouvelle))
    assert(!avant.interactions.contains(nouvelle))

  test("ajouter un item ou un utilisateur renvoie un nouveau catalogue"):
    val item = Item(99, "Casque", "Audio", 120.0)
    val user = User(99, Segment.Occasionnel, Fixtures.date)

    assertEquals(Fixtures.catalogue.avecItem(item).items.size, Fixtures.catalogue.items.size + 1)
    assertEquals(Fixtures.catalogue.avecUser(user).users.size, Fixtures.catalogue.users.size + 1)
    assertEquals(Fixtures.catalogue.items.size, 3, "le catalogue initial doit être intact")

  test("les recherches par identifiant renvoient Option"):
    assertEquals(Fixtures.catalogue.item(Fixtures.clavier.id), Some(Fixtures.clavier))
    assertEquals(Fixtures.catalogue.item(999), None)
    assertEquals(Fixtures.catalogue.user(Fixtures.alice.id), Some(Fixtures.alice))
    assertEquals(Fixtures.catalogue.user(999), None)

  test("l'historique d'un utilisateur inconnu est vide, sans erreur"):
    assertEquals(Fixtures.catalogue.interactionsDe(999), Nil)
    assertEquals(Fixtures.catalogue.itemsConnusDe(999), Set.empty[ItemId])

  test("itemsConnusDe rassemble tous les items touchés par l'utilisateur"):
    assertEquals(Fixtures.catalogue.itemsConnusDe(Fixtures.bob.id), Set(Fixtures.clavier.id, Fixtures.ecran.id))
