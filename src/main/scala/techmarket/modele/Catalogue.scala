package techmarket.modele

final case class Catalogue(
    items: List[Item],
    users: List[User],
    interactions: List[Interaction]
):

  lazy val itemsParId: Map[ItemId, Item] = items.map(i => i.id -> i).toMap
  lazy val usersParId: Map[UserId, User] = users.map(u => u.id -> u).toMap
  lazy val interactionsParUser: Map[UserId, List[Interaction]] =
    interactions.groupBy(_.userId)

  def item(id: ItemId): Option[Item] = itemsParId.get(id)
  def user(id: UserId): Option[User] = usersParId.get(id)

  def interactionsDe(userId: UserId): List[Interaction] =
    interactionsParUser.getOrElse(userId, Nil)

  def itemsConnusDe(userId: UserId): Set[ItemId] =
    interactionsDe(userId).map(_.itemId).toSet

  def avecItem(nouvel: Item): Catalogue = copy(items = items :+ nouvel)
  def avecUser(nouvel: User): Catalogue = copy(users = users :+ nouvel)
  def avecInteraction(nouvelle: Interaction): Catalogue =
    copy(interactions = interactions :+ nouvelle)
