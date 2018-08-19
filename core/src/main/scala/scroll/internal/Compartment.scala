package scroll.internal

import scroll.internal.errors.SCROLLErrors._
import scroll.internal.graph.ScalaRoleGraph
//import scroll.internal.graph.{CachedScalaRoleGraph, ScalaRoleGraph}
import scroll.internal.support.UnionTypes.RoleUnionTypes
import scroll.internal.support._
import scroll.internal.util.ReflectiveHelper

import scala.annotation.tailrec
import scala.reflect.{ ClassTag, classTag }

/** This Trait allows for implementing an objectified collaboration with a limited number of participating roles and a fixed scope.
  *
  * ==Overview==
  * Roles are dependent on some sort of context. We call them compartments. A typical example of a compartment is a university,
  * which contains the roles Student and Teacher collaborating in Courses. Everything in SCROLL happens inside of Compartments
  * but roles (implemented as standard Scala classes) can be defined or imported from everywhere. Just mix in this Trait
  * into your own specific compartment class or create an anonymous instance.
  *
  * ==Example==
  * {{{
  * val player = new Player()
  * new Compartment {
  *   class RoleA
  *   class RoleB
  *
  *   player play new RoleA()
  *   player play new RoleB()
  *
  *   // call some behaviour
  * }
  * }}}
  */
trait Compartment
  extends RoleConstraints
  with RoleRestrictions
  with RoleGroups
  with Relationships
  with QueryStrategies
  with RoleUnionTypes {

  protected var plays: ScalaRoleGraph = new ScalaRoleGraph()

  implicit def either2TorException[T](either: Either[_, T]): T = either.fold(
    l => {
      throw new RuntimeException(l.toString)
    }, r => {
      r
    })

  /** Declaring a is-part-of relation between compartments.
    */
  def partOf(other: Compartment): Unit = {
    require(null != other)
    plays.merge(other.plays)
  }

  /** Declaring a bidirectional is-part-of relation between compartment.
    */
  def union(other: Compartment): Compartment = {
    require(null != other)
    other.partOf(this)
    this.partOf(other)
    this
  }

  /** Merge role graphs to this and set other role graph to this one.
    */
  def combine(other: Compartment): Compartment = {
    require(null != other)
    plays.addPartAndCombine(other.plays)
    //println("*************************************************************Combine0: G1: " + this.plays + " G2: " + other.plays + " O1: " + this + " O2: " + other)
    other.plays = this.plays
    //println("*************************************************************Combine1: G1: " + this.plays + " G2: " + other.plays + " O1: " + this + " O2: " + other)
    this
  }

  /** Removing is-part-of relation between compartments.
    */
  def notPartOf(other: Compartment): Unit = {
    require(null != other)
    plays.detach(other.plays)
  }

  /** Query the role playing graph for all player instances that do conform to the given matcher.
    *
    * @param matcher the matcher that should match the queried player instance in the role playing graph
    * @tparam T the type of the player instance to query for
    * @return all player instances as Seq, that do conform to the given matcher
    */
  def all[T <: AnyRef: ClassTag](matcher: RoleQueryStrategy = MatchAny()): Seq[T] = {
    plays.allPlayers.filter(ReflectiveHelper.is[T]).map(_.asInstanceOf[T]).filter(a => {
      getCoreFor(a) match {
        case p :: Nil => matcher.matches(p)
        case Nil => false
        case l => l.forall(matcher.matches)
      }
    })
  }

  /** Query the role playing graph for all player instances that do conform to the given function.
    *
    * @param matcher the matching function that should match the queried player instance in the role playing graph
    * @tparam T the type of the player instance to query for
    * @return all player instances as Seq, that do conform to the given matcher
    */
  def all[T <: AnyRef: ClassTag](matcher: T => Boolean): Seq[T] =
    plays.allPlayers.filter(ReflectiveHelper.is[T]).map(_.asInstanceOf[T]).filter(a => {
      getCoreFor(a) match {
        case p :: Nil => matcher(p.asInstanceOf[T])
        case Nil => false
        case l: Seq[AnyRef] => l.forall(i => matcher(i.asInstanceOf[T]))
      }
    })

  private def safeReturn[T](seq: Seq[T], typeName: String): Either[TypeError, Seq[T]] = seq match {
    case Nil => Left(TypeNotFound(typeName))
    case s => Right(s)
  }

  /** Query the role playing graph for all player instances that do conform to the given matcher and return the first found.
    *
    * @param matcher the matcher that should match the queried player instance in the role playing graph
    * @tparam T the type of the player instance to query for
    * @return the first player instance, that does conform to the given matcher or an appropriate error
    */
  def one[T <: AnyRef: ClassTag](matcher: RoleQueryStrategy = MatchAny()): Either[TypeError, T] = safeReturn(all[T](matcher), classTag[T].toString).fold(
    l => {
      Left(l)
    }, r => {
      Right(r.head)
    })

  /** Query the role playing graph for all player instances that do conform to the given function and return the first found.
    *
    * @param matcher the matching function that should match the queried player instance in the role playing graph
    * @tparam T the type of the player instance to query for
    * @return the first player instances, that do conform to the given matcher or an appropriate error
    */
  def one[T <: AnyRef: ClassTag](matcher: T => Boolean): Either[TypeError, T] = safeReturn(all[T](matcher), classTag[T].toString).fold(
    l => {
      Left(l)
    }, r => {
      Right(r.head)
    })

  /** Adds a play relation between core and role.
    *
    * @tparam C type of core
    * @tparam R type of role
    * @param core the core to add the given role at
    * @param role the role that should added to the given core
    */
  def addPlaysRelation[C <: AnyRef: ClassTag, R <: AnyRef: ClassTag](core: C, role: R): Unit = {
    require(null != core)
    require(null != role)
    validate(core, role)
    plays.addBinding(core, role)
  }

  /** Removes the play relation between core and role.
    *
    * @tparam C type of core
    * @tparam R type of role
    * @param core the core the given role should removed from
    * @param role the role that should removed from the given core
    */
  def removePlaysRelation[C <: AnyRef: ClassTag, R <: AnyRef: ClassTag](core: C, role: R): Unit = {
    require(null != core)
    require(null != role)
    plays.removeBinding(core, role)
  }

  /** Transfers a role from one core to another.
    *
    * @tparam F type of core the given role should be removed from
    * @tparam T type of core the given role should be attached to
    * @tparam R type of role
    * @param coreFrom the core the given role should be removed from
    * @param coreTo   the core the given role should be attached to
    * @param role     the role that should be transferred
    */
  def transferRole[F <: AnyRef: ClassTag, T <: AnyRef: ClassTag, R <: AnyRef: ClassTag](coreFrom: F, coreTo: T, role: R): Unit = {
    require(null != coreFrom)
    require(null != coreTo)
    require(null != role)
    require(coreFrom != coreTo, "You can not transfer a role from itself.")
    removePlaysRelation(coreFrom, role)
    addPlaysRelation(coreTo, role)
  }

  /** Wraps an object playing some role in the current compartment into a player instance to enable access to its roles.
    *
    * This is particularly useful if the object plays multiple roles in different compartments and
    * the scopes where the play relation is defined and actually used differ.
    *
    * If an instance which is already lifted is passed it will just be returned.
    *
    * @param player the object. It has to play some role in the compartment
    * @tparam T the type of the object
    * @return the player
    */
  def lift[T <: AnyRef: ClassTag](player: T): Option[Player[T]] = {
    player match {
      case p: Player[_] if plays.containsPlayer(p.wrapped) => Some(p)
      case Player => None
      case wrapped if plays.containsPlayer(wrapped) => new Player(wrapped)
      case _ => None
    }

    if (plays.containsPlayer(player)) Some(new Player[T](player)) else None
  }

  /** Alias for [[Compartment.lift]]
    *
    * @param player the object. It has to play some role in the compartment
    * @tparam T the type of the object
    * @return the player
    */
  def @:[T <: AnyRef: ClassTag](player: T): Option[Player[T]] = lift(player)

  @tailrec
  protected final def getCoreFor(role: AnyRef): Seq[AnyRef] = {
    require(null != role)
    role match {
      case cur: IPlayer[_] => getCoreFor(cur.wrapped) // the role graph stores the player as unwrapped objects
      case cur: AnyRef if plays.containsPlayer(cur) =>
        val r = plays.getPredecessors(cur)
        if (r.isEmpty) {
          Seq(cur)
        } else {
          if (r.lengthCompare(1) == 0) {
            getCoreFor(r.head)
          } else {
            r
          }
        }
      case _ => Seq(role)
    }
  }

  /** Explicit helper factory method for creating a new Player instance
    * without the need to relying on the implicit mechanics of Scala.
    *
    * @param obj the player or role that is wrapped into this dynamic player type
    * @return a new Player instance wrapping the given object
    */
  def newPlayer(obj: Object): Player[Object] = {
    require(null != obj)
    new Player(obj)
  }

  implicit class Player[T <: AnyRef: ClassTag](override val wrapped: T) extends IPlayer[T](wrapped) with SCROLLDynamic with SCROLLDispatchable {

    override def unary_+ : Player[T] = this

    /** Returns the player of this player instance if this is a role, or this itself.
      *
      * @param dispatchQuery provide this to sort the resulting instances if a role instance is played by multiple core objects
      * @return the player of this player instance if this is a role, or this itself or an appropriate error
      */
    def player(implicit dispatchQuery: DispatchQuery = DispatchQuery.empty): Either[TypeError, AnyRef] = dispatchQuery.filter(getCoreFor(this)) match {
      case elem :: Nil => Right(elem)
      case l: Seq[T] => Right(l.head)
      case _ => Left(TypeNotFound(this.getClass.toString))
    }

    override def play[R <: AnyRef: ClassTag](role: R): Player[T] = {
      require(null != role)
      wrapped match {
        case p: Player[_] => addPlaysRelation[T, R](p.wrapped.asInstanceOf[T], role)
        case p: AnyRef => addPlaysRelation[T, R](p.asInstanceOf[T], role)
        case p => throw new RuntimeException(s"Only instances of 'IPlayer' or 'AnyRef' are allowed to play roles! You tried it with '$p'.")
      }
      this
    }

    override def <+>[R <: AnyRef: ClassTag](role: R): Player[T] = play(role)

    /** Adds a play relation between core and role but always returns the player instance.
      *
      * @tparam R type of role
      * @param role the role that should played
      * @return the player instance
      */
    def playing[R <: AnyRef: ClassTag](role: R): T = play(role).wrapped

    /** Alias for [[Player.playing]].
      *
      * @tparam R type of role
      * @param role the role that should played
      * @return the player instance
      */
    def <=>[R <: AnyRef: ClassTag](role: R): T = playing(role)

    override def drop[R <: AnyRef: ClassTag](role: R): Player[T] = {
      removePlaysRelation[T, R](wrapped, role)
      this
    }

    override def <->[R <: AnyRef: ClassTag](role: R): Player[T] = drop(role)

    override def getRoles: Seq[AnyRef] = plays.getRoles(this)

    /** Transfers a role to another player.
      *
      * @tparam R type of role
      * @param role the role to transfer
      */
    def transfer[R <: AnyRef: ClassTag](role: R) = new {
      def to[P <: AnyRef: ClassTag](player: P): Unit = {
        transferRole[T, P, R](wrapped, player, role)
      }
    }

    /** Checks of this Player is playing a role of the given type R.
      *
      * @tparam R type of role
      * @return true if this player is playing a role of type R, false otherwise. Returns false also, if
      *         the player is not available in the role-playing graph.
      */
    def isPlaying[R <: AnyRef: ClassTag]: Boolean = plays.getRoles(wrapped).exists(ReflectiveHelper.is[R])

    /** Checks if this Player has all of the given facet(s) attached.
      *
      * @param f the facet(s)
      * @return true if this player has all of the given facets attached, false otherwise.
      */
    def hasFacets(f: Enumeration#Value*): Boolean = f.forall(plays.getFacets(wrapped).contains)

    /** Checks if this Player has at least one of the given facets attached.
      *
      * @param f the facets
      * @return true if this player has at least one of the given facets attached, false otherwise.
      */
    def hasSomeFacet(f: Enumeration#Value*): Boolean = f.exists(plays.getFacets(wrapped).contains)

    /** Checks of this Player has an extension of the given type.
      * Alias for [[Player.isPlaying]].
      */
    def hasExtension[E <: AnyRef: ClassTag]: Boolean = isPlaying[E]

    override def applyDynamic[E, A](name: String)(args: A*)(implicit dispatchQuery: DispatchQuery = DispatchQuery.empty): Either[SCROLLError, E] = {
      val core = getCoreFor(wrapped).last
      dispatchQuery.filter(plays.getRoles(core)).foreach(r => {
        ReflectiveHelper.findMethod(r, name, args).foreach(fm => {
          args match {
            case Nil => return dispatch(r, fm)
            case _ => return dispatch(r, fm, args)
          }
        })
      })
      // otherwise give up
      Left(RoleNotFound(core.toString, name, args))
    }

    override def applyDynamicNamed[E](name: String)(args: (String, Any)*)(implicit dispatchQuery: DispatchQuery = DispatchQuery.empty): Either[SCROLLError, E] =
      applyDynamic(name)(args.map(_._2): _*)(dispatchQuery)

    override def selectDynamic[E](name: String)(implicit dispatchQuery: DispatchQuery = DispatchQuery.empty): Either[SCROLLError, E] = {
      val core = getCoreFor(wrapped).last
      val roles = dispatchQuery.filter(plays.getRoles(core))
      val member = roles.find(ReflectiveHelper.hasMember(_, name))
      member match {
        case Some(r) => Right(ReflectiveHelper.propertyOf(r, name))
        case None => Left(RoleNotFound(core.toString, name, Seq.empty))
      }
    }

    override def updateDynamic(name: String)(value: Any)(implicit dispatchQuery: DispatchQuery = DispatchQuery.empty): Unit = {
      val core = getCoreFor(wrapped).last
      dispatchQuery.filter(plays.getRoles(core)).find(ReflectiveHelper.hasMember(_, name)) match {
        case Some(r) => ReflectiveHelper.setPropertyOf(r, name, value)
        case None => // do nothing
      }
    }

    override def equals(o: Any): Boolean = o match {
      case other: Player[_] =>
        val ownCore = getCoreFor(wrapped)
        val otherCore = getCoreFor(other.wrapped)
        ownCore.reverse.headOption == otherCore.reverse.headOption
      case other: Any => getCoreFor(wrapped).contains(other)
      case _ => false // default case
    }

    override def hashCode(): Int = wrapped.hashCode()

    override def toString: String = s"${this.getClass.getName}: ${wrapped}"

  }

  object Player {
    def unapply[T <: AnyRef: ClassTag](arg: Player[T]): Option[T] = Some(arg.wrapped)
  }

}
