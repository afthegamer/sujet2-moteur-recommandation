package techmarket.io

import techmarket.modele.*

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util.Locale
import scala.util.Try

object ExportJson:

  private def nombre(valeur: Double): String =
    String.format(Locale.ROOT, "%.2f", valeur)

  private def echapper(texte: String): String =
    texte.flatMap:
      case '"'  => "\\\""
      case '\\' => "\\\\"
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\t' => "\\t"
      case c    => c.toString

  private def chaine(valeur: String): String = s""""${echapper(valeur)}""""

  private def suggestion(recommandation: Recommandation): String =
    val item = recommandation.item
    List(
      s""""itemId": ${item.id}""",
      s""""nom": ${chaine(item.nom)}""",
      s""""categorie": ${chaine(item.categorie)}""",
      s""""prixEur": ${nombre(item.prixEur)}""",
      s""""score": ${nombre(recommandation.score)}""",
      s""""justification": ${chaine(recommandation.justification)}"""
    ).mkString("      { ", ", ", " }")

  private def bloc(utilisateur: User, recommandations: List[Recommandation]): String =
    val suggestions =
      if recommandations.isEmpty then "[]"
      else recommandations.map(suggestion).mkString("[\n", ",\n", "\n    ]")

    s"""  {
       |    "userId": ${utilisateur.id},
       |    "segment": ${chaine(Affichage.libelleSegment(utilisateur.segment))},
       |    "suggestions": $suggestions
       |  }""".stripMargin

  def serialiser(resultats: List[(User, List[Recommandation])]): String =
    val blocs = resultats.map((utilisateur, recommandations) => bloc(utilisateur, recommandations)).mkString(",\n")
    s"""{
       |  "recommandations": [
       |$blocs
       |  ]
       |}
       |""".stripMargin

  def ecrire(chemin: String, resultats: List[(User, List[Recommandation])]): Either[String, String] =
    Try(Files.writeString(Paths.get(chemin), serialiser(resultats), StandardCharsets.UTF_8)).toEither.left
      .map(erreur => s"Écriture impossible de '$chemin' : ${erreur.getMessage}")
      .map(_ => chemin)
