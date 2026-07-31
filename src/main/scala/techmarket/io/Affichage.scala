package techmarket.io

import techmarket.modele.*

import java.util.Locale

object Affichage:

  private def nombre(valeur: Double): String =
    String.format(Locale.ROOT, "%.2f", valeur)

  def libelleSegment(segment: Segment): String =
    segment match
      case Segment.Nouveau     => "nouveau"
      case Segment.Occasionnel => "occasionnel"
      case Segment.Fidele      => "fidèle"

  def formaterRecommandations(
      utilisateur: User,
      recommandations: List[Recommandation]
  ): String =
    val entete =
      s"Utilisateur ${utilisateur.id} — segment ${libelleSegment(utilisateur.segment)}"

    val corps =
      if recommandations.isEmpty then List("  Aucune recommandation disponible.")
      else
        recommandations.zipWithIndex.map: (reco, index) =>
          val ligne =
            f"  ${index + 1}. ${reco.item.nom}%-28s ${nombre(reco.item.prixEur)}%9s EUR   score ${nombre(reco.score)}"
          s"$ligne\n     ${reco.justification}"

    (entete :: corps).mkString("\n")
