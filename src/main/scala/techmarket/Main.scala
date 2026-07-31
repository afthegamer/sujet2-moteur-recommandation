package techmarket

import techmarket.io.{Affichage, ChargeurCsv, ExportJson}
import techmarket.modele.*
import techmarket.orchestration.MoteurRecommandation

@main def main(): Unit =
  val topN = 3
  val fichierExport = "recommandations.json"

  ChargeurCsv.chargerCatalogue("data") match
    case Left(erreur) =>
      println("Chargement du jeu de données impossible :")
      println(erreur)

    case Right(catalogue) =>
      println("=== Recommandations ===")

      val echantillon = Segment.values.toList.flatMap: segment =>
        catalogue.users.filter(_.segment == segment).sortBy(_.id).headOption

      val resultats = echantillon.map: utilisateur =>
        (utilisateur, MoteurRecommandation.recommander(catalogue, utilisateur.id, topN).getOrElse(Nil))

      resultats.foreach: (utilisateur, recommandations) =>
        println()
        println(Affichage.formaterRecommandations(utilisateur, recommandations))

      println()
      ExportJson.ecrire(fichierExport, resultats) match
        case Left(erreur)  => println(erreur)
        case Right(chemin) => println(s"=== Export JSON écrit dans $chemin ===")
