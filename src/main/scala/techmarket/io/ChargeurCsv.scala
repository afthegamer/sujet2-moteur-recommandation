package techmarket.io

import techmarket.modele.*

import java.time.LocalDate
import scala.io.Source
import scala.util.{Try, Using}

object ChargeurCsv:

  def chargerCatalogue(dossier: String): Either[String, Catalogue] =
    for
      items        <- chargerItems(s"$dossier/items.csv")
      users        <- chargerUsers(s"$dossier/users.csv")
      interactions <- chargerInteractions(s"$dossier/interactions.csv")
    yield Catalogue(items, users, interactions)

  def chargerItems(chemin: String): Either[String, List[Item]] =
    parserFichier(chemin)(parserItem)

  def chargerUsers(chemin: String): Either[String, List[User]] =
    parserFichier(chemin)(parserUser)

  def chargerInteractions(chemin: String): Either[String, List[Interaction]] =
    parserFichier(chemin)(parserInteraction)

  private def lireLignes(chemin: String): Either[String, List[String]] =
    Using(Source.fromFile(chemin, "UTF-8"))(_.getLines().toList).toEither.left
      .map(erreur => s"Lecture impossible de '$chemin' : ${erreur.getMessage}")

  private def parserFichier[A](chemin: String)(
      parserLigne: Array[String] => Either[String, A]
  ): Either[String, List[A]] =
    lireLignes(chemin).flatMap: lignes =>
      val resultats = lignes.zipWithIndex
        .drop(1)
        .filter((ligne, _) => ligne.trim.nonEmpty)
        .map: (ligne, index) =>
          parserLigne(ligne.split(",", -1))
            .left
            .map(erreur => s"$chemin, ligne ${index + 1} : $erreur")

      resultats.partitionMap(identity) match
        case (Nil, valeurs)  => Right(valeurs)
        case (erreurs, _)    => Left(erreurs.mkString("\n"))

  private def parserItem(champs: Array[String]): Either[String, Item] =
    champs match
      case Array(id, nom, categorie, prix) =>
        for
          identifiant <- id.trim.toIntOption.toRight(s"item_id invalide : '$id'")
          prixEur     <- prix.trim.toDoubleOption.toRight(s"prix_eur invalide : '$prix'")
        yield Item(identifiant, nom.trim, categorie.trim, prixEur)
      case autre => Left(s"${autre.length} colonnes au lieu de 4")

  private def parserUser(champs: Array[String]): Either[String, User] =
    champs match
      case Array(id, segment, dateInscription) =>
        for
          identifiant <- id.trim.toIntOption.toRight(s"user_id invalide : '$id'")
          seg         <- Segment.depuisLibelle(segment).toRight(s"segment inconnu : '$segment'")
          date        <- parserDate(dateInscription)
        yield User(identifiant, seg, date)
      case autre => Left(s"${autre.length} colonnes au lieu de 3")

  private def parserInteraction(champs: Array[String]): Either[String, Interaction] =
    champs match
      case Array(id, userId, itemId, typeBrut, noteBrute, dateBrute) =>
        for
          identifiant <- id.trim.toIntOption.toRight(s"interaction_id invalide : '$id'")
          utilisateur <- userId.trim.toIntOption.toRight(s"user_id invalide : '$userId'")
          item        <- itemId.trim.toIntOption.toRight(s"item_id invalide : '$itemId'")
          typeInter   <- parserType(typeBrut, noteBrute)
          date        <- parserDate(dateBrute)
        yield Interaction(identifiant, utilisateur, item, typeInter, date)
      case autre => Left(s"${autre.length} colonnes au lieu de 6")

  private def parserType(typeBrut: String, noteBrute: String): Either[String, TypeInteraction] =
    typeBrut.trim.toLowerCase match
      case "vue"   => Right(TypeInteraction.Vue)
      case "achat" => Right(TypeInteraction.Achat)
      case "note" =>
        noteBrute.trim.toIntOption
          .toRight(s"note manquante ou non numérique : '$noteBrute'")
          .flatMap: valeur =>
            if valeur >= 1 && valeur <= 5 then Right(TypeInteraction.Note(valeur))
            else Left(s"note hors de l'intervalle 1-5 : $valeur")
      case autre => Left(s"type d'interaction inconnu : '$autre'")

  private def parserDate(brute: String): Either[String, LocalDate] =
    Try(LocalDate.parse(brute.trim)).toEither.left
      .map(_ => s"date invalide : '$brute'")
