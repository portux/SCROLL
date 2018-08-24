package scroll.internal.graph

import scala.reflect.ClassTag

/**
  * Trait defining an generic interface for all kind of role graphs.
  */
trait RoleGraph {
  /**
    * Merges this with another RoleGraph given as other.
    *
    * @param other the RoleGraph to merge with.
    */
  //def merge(other: RoleGraph): Unit
  
  /**
    * Combines this with another RoleGraph given as other.
    *
    * @param other the RoleGraph to combine with.
    */
  //def combine(other: RoleGraph): Unit
  
  /**
    * RoleGraph given as other would get part of this.
    *
    * @param other the RoleGraph for integration in this one.
    */
  def addPart(other: RoleGraph): Boolean
  
  /**
    * RoleGraph given as other would get part of this and set both to this.
    *
    * @param other the RoleGraph for integration in this one and set as this.
    */
  //def addPartAndCombine(other: RoleGraph): Unit

  /**
    * Removes all players and plays-relationships specified in other from this RoleGraph.
    *
    * @param other the RoleGraph all players and plays-relationships specified in should removed from this
    */
  def detach(other: RoleGraph): Unit

  /**
    * Adds a plays relationship between core and role.
    *
    * @tparam P type of the player
    * @tparam R type of the role
    * @param player the player instance to add the given role
    * @param role   the role instance to add
    */
  def addBinding[P <: AnyRef : ClassTag, R <: AnyRef : ClassTag](player: P, role: R): Unit

  /**
    * Removes a plays relationship between core and role.
    *
    * @param player the player instance to remove the given role from
    * @param role   the role instance to remove
    */
  def removeBinding[P <: AnyRef : ClassTag, R <: AnyRef : ClassTag](player: P, role: R): Unit

  /**
    * Removes the given player from the graph.
    * This should remove its binding too!
    *
    * @param player the player to remove
    */
  def removePlayer[P <: AnyRef : ClassTag](player: P): Unit

  /**
    * Returns a Seq of all players
    *
    * @return a Seq of all players
    */
  def allPlayers: Seq[AnyRef]

  /**
    * Returns a Seq of all roles attached to the given player (core object).
    *
    * @param player the player instance to get the roles for
    * @return a Seq of all roles of core player including the player object itself. Returns an empty Seq if the given player is not in the role-playing graph.
    */
  def getRoles(player: AnyRef): Seq[AnyRef]

  /**
    * Returns a Seq of all facets attached to the given player (core object).
    *
    * @param player the player instance to get the facets for
    * @return a Seq of all facets of core player including the player object itself. Returns an empty Seq if the given player is not in the role-playing graph.
    */
  def getFacets(player: AnyRef): Seq[Enumeration#Value]

  /**
    * Checks if the role graph contains the given player.
    *
    * @param player the player instance to check
    * @return true if the role graph contains the given player, false otherwise
    */
  def containsPlayer(player: AnyRef): Boolean

  /**
    * Returns a list of all predecessors of the given player, i.e. a transitive closure
    * of its cores (deep roles).
    *
    * @param player the player instance to calculate the cores of
    * @return a list of all predecessors of the given player
    */
  def getPredecessors(player: AnyRef): Seq[AnyRef]
}