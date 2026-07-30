package techmarket

import techmarket.io.ChargeurCsv
import techmarket.modele.*

import java.nio.file.Files

class ChargeurCsvTest extends munit.FunSuite:

  private def csvTemporaire(lignes: String*): String =
    val fichier = Files.createTempFile("techmarket-test", ".csv")
    Files.writeString(fichier, lignes.mkString("\n"))
    fichier.toFile.deleteOnExit()
    fichier.toString

  test("le jeu de données fourni se charge intégralement"):
    val catalogue = ChargeurCsv.chargerCatalogue("data").fold(erreur => fail(erreur), identity)
    assertEquals(catalogue.items.size, 18)
    assertEquals(catalogue.users.size, 20)
    assertEquals(catalogue.interactions.size, 82)

  test("les segments sont tous reconnus"):
    val users = ChargeurCsv.chargerUsers("data/users.csv").fold(erreur => fail(erreur), identity)
    val parSegment = users.groupBy(_.segment).view.mapValues(_.size).toMap
    assertEquals(parSegment(Segment.Nouveau), 7)
    assertEquals(parSegment(Segment.Fidele), 7)
    assertEquals(parSegment(Segment.Occasionnel), 6)

  test("les types d'interaction sont tous reconnus"):
    val interactions = ChargeurCsv.chargerInteractions("data/interactions.csv").fold(erreur => fail(erreur), identity)
    assertEquals(interactions.count(_.typeInteraction == TypeInteraction.Vue), 46)
    assertEquals(interactions.count(_.typeInteraction == TypeInteraction.Achat), 11)
    assertEquals(interactions.count(_.typeInteraction.isInstanceOf[TypeInteraction.Note]), 25)

  test("toute interaction référence un utilisateur et un item existants"):
    val catalogue = ChargeurCsv.chargerCatalogue("data").fold(erreur => fail(erreur), identity)
    val itemIds = catalogue.items.map(_.id).toSet
    val userIds = catalogue.users.map(_.id).toSet
    assert(catalogue.interactions.forall(i => itemIds.contains(i.itemId)), "item_id orphelin")
    assert(catalogue.interactions.forall(i => userIds.contains(i.userId)), "user_id orphelin")

  test("un fichier absent produit un Left, sans exception"):
    val resultat = ChargeurCsv.chargerCatalogue("dossier/qui/nexiste/pas")
    assert(resultat.isLeft)
    assert(resultat.left.exists(_.contains("Lecture impossible")))

  test("un mauvais nombre de colonnes est refusé"):
    val chemin = csvTemporaire("item_id,nom,categorie,prix_eur", "1,Clavier,Accessoires")
    assert(ChargeurCsv.chargerItems(chemin).left.exists(_.contains("colonnes au lieu de 4")))

  test("un identifiant non numérique est refusé"):
    val chemin = csvTemporaire("item_id,nom,categorie,prix_eur", "abc,Clavier,Accessoires,50.0")
    assert(ChargeurCsv.chargerItems(chemin).left.exists(_.contains("item_id invalide")))

  test("un segment inconnu est refusé"):
    val chemin = csvTemporaire("user_id,segment,date_inscription", "1,vip,2025-01-01")
    assert(ChargeurCsv.chargerUsers(chemin).left.exists(_.contains("segment inconnu")))

  test("la variante accentuée « fidèle » est acceptée"):
    val chemin = csvTemporaire("user_id,segment,date_inscription", "1,fidèle,2025-01-01")
    val users = ChargeurCsv.chargerUsers(chemin).fold(erreur => fail(erreur), identity)
    assertEquals(users.map(_.segment), List(Segment.Fidele))

  test("un type d'interaction inconnu est refusé"):
    val chemin = csvTemporaire(
      "interaction_id,user_id,item_id,type,note,date",
      "1,1,1,clic,,2025-01-01"
    )
    assert(ChargeurCsv.chargerInteractions(chemin).left.exists(_.contains("type d'interaction inconnu")))

  test("une note hors de l'intervalle 1-5 est refusée"):
    val chemin = csvTemporaire(
      "interaction_id,user_id,item_id,type,note,date",
      "1,1,1,note,6,2025-01-01"
    )
    assert(ChargeurCsv.chargerInteractions(chemin).left.exists(_.contains("hors de l'intervalle")))

  test("une interaction de type note sans valeur est refusée"):
    val chemin = csvTemporaire(
      "interaction_id,user_id,item_id,type,note,date",
      "1,1,1,note,,2025-01-01"
    )
    assert(ChargeurCsv.chargerInteractions(chemin).left.exists(_.contains("note manquante")))

  test("une date invalide est refusée"):
    val chemin = csvTemporaire(
      "interaction_id,user_id,item_id,type,note,date",
      "1,1,1,vue,,pas-une-date"
    )
    assert(ChargeurCsv.chargerInteractions(chemin).left.exists(_.contains("date invalide")))

  test("le numéro de ligne rapporté est le numéro physique, même après une ligne vide"):
    val chemin = csvTemporaire(
      "item_id,nom,categorie,prix_eur",
      "1,Clavier,Accessoires,50.0",
      "",
      "2,Souris,Accessoires,abc"
    )
    val erreur = ChargeurCsv.chargerItems(chemin).left.getOrElse("")
    assert(erreur.contains("ligne 4"), s"message obtenu : $erreur")
    assert(erreur.contains("prix_eur invalide"))

  test("toutes les erreurs d'un fichier sont rassemblées en une seule passe"):
    val chemin = csvTemporaire(
      "item_id,nom,categorie,prix_eur",
      "abc,Clavier,Accessoires,50.0",
      "2,Souris,Accessoires,xyz"
    )
    val erreur = ChargeurCsv.chargerItems(chemin).left.getOrElse("")
    assert(erreur.contains("item_id invalide"), s"message obtenu : $erreur")
    assert(erreur.contains("prix_eur invalide"), s"message obtenu : $erreur")
    assert(erreur.contains("ligne 2") && erreur.contains("ligne 3"))
