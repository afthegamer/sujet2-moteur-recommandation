package techmarket

import techmarket.io.ExportJson
import techmarket.modele.*

import java.nio.charset.StandardCharsets
import java.nio.file.Files

class ExportJsonTest extends munit.FunSuite:

  private val reco = Recommandation(Fixtures.clavier, 12.5, "test de justification")

  test("le JSON contient les champs attendus pour une suggestion"):
    val json = ExportJson.serialiser(List((Fixtures.alice, List(reco))))
    assert(json.contains("\"userId\": 1"), json)
    assert(json.contains("\"segment\": \"fidèle\""), json)
    assert(json.contains("\"itemId\": 1"), json)
    assert(json.contains("\"nom\": \"Clavier\""), json)
    assert(json.contains("\"categorie\": \"Accessoires\""), json)
    assert(json.contains("\"justification\": \"test de justification\""), json)

  test("les nombres utilisent le point décimal, jamais la virgule"):
    val json = ExportJson.serialiser(List((Fixtures.alice, List(reco))))
    assert(json.contains("\"prixEur\": 50.00"), json)
    assert(json.contains("\"score\": 12.50"), json)
    assert(!json.contains("50,00") && !json.contains("12,50"), "virgule décimale : JSON invalide")

  test("un utilisateur sans recommandation donne un tableau vide"):
    val json = ExportJson.serialiser(List((Fixtures.chloe, Nil)))
    assert(json.contains("\"suggestions\": []"), json)

  test("les guillemets et antislashs sont échappés"):
    val item = Item(9, "Écran 27\" \\ pro", "Périphériques", 200.0)
    val json = ExportJson.serialiser(List((Fixtures.alice, List(Recommandation(item, 1.0, "ok")))))
    assert(json.contains("""\"""), json)
    assert(json.contains("""\\"""), json)

  test("plusieurs utilisateurs produisent plusieurs blocs"):
    val json = ExportJson.serialiser(
      List((Fixtures.alice, List(reco)), (Fixtures.bob, List(reco)))
    )
    assert(json.contains("\"userId\": 1"), json)
    assert(json.contains("\"userId\": 2"), json)

  test("ecrire crée le fichier et rend son chemin"):
    val fichier = Files.createTempFile("techmarket-export", ".json")
    fichier.toFile.deleteOnExit()
    val resultat = ExportJson.ecrire(fichier.toString, List((Fixtures.alice, List(reco))))
    assertEquals(resultat, Right(fichier.toString))
    val contenu = Files.readString(fichier, StandardCharsets.UTF_8)
    assert(contenu.contains("\"recommandations\""), contenu)
    assert(contenu.contains("\"nom\": \"Clavier\""), contenu)

  test("un chemin invalide produit un Left, sans exception"):
    val resultat = ExportJson.ecrire("dossier/qui/nexiste/pas/sortie.json", List((Fixtures.alice, Nil)))
    assert(resultat.isLeft)
    assert(resultat.left.exists(_.contains("Écriture impossible")))
