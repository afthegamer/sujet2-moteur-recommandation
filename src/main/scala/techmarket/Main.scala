package techmarket

import techmarket.io.{Affichage, ChargeurCsv}
import techmarket.modele.*
import techmarket.orchestration.MoteurRecommandation

@main def main(): Unit =
  val topN = 3

  ChargeurCsv.chargerCatalogue("data") match
    case Left(erreur) =>
      println("Chargement du jeu de données impossible :")
      println(erreur)

    case Right(catalogue) =>
      println("=== Recommandations ===")

      val echantillon = Segment.values.toList.flatMap: segment =>
        catalogue.users.filter(_.segment == segment).sortBy(_.id).headOption

      echantillon.foreach: utilisateur =>
        println()
        MoteurRecommandation.recommander(catalogue, utilisateur.id, topN) match
          case Left(erreur) => println(s"  $erreur")
          case Right(recommandations) =>
            println(Affichage.formaterRecommandations(utilisateur, recommandations))
