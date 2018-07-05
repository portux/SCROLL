package scroll.internal

import scroll.internal.errors.SCROLLErrors._
import scroll.internal.support._
import scroll.internal.util.ReflectiveHelper

import scala.collection.mutable
import scala.language.dynamics
import scala.reflect.ClassTag

/**
  * This Trait allows for implementing an objectified collaboration with a limited number of participating roles and a fixed scope.
  * In contrast to the normal Compartment, in case of ambiguities all role methods will be called in sequence.
  */
trait MultiCompartment extends Compartment {

  implicit def either2SeqTOrException[T](either: Either[_, Seq[Either[_, T]]]): Seq[T] = either.fold(
    left => {
      throw new RuntimeException(left.toString)
    },
    right => {
      var res: Seq[T] = Seq()

      right.foreach { value: Either[_, T] =>
        res = res :+ value.fold(
          l => {
            throw new RuntimeException(l.toString)
          }, r => {
            r
          }
        )
      }

      res
    }
  )

  implicit def either2SeqTHeadOrException[T](either: Either[_, Seq[Either[_, T]]]): T = either2SeqTOrException(either).head

  implicit class MultiPlayer[T <: AnyRef : ClassTag](override val wrapped: T) extends IPlayer[T](wrapped) with Dynamic with SCROLLDispatchable {

    override def unary_+ : MultiPlayer[T] = this

    override def play[R <: AnyRef : ClassTag](role: R): MultiPlayer[T] = {
      require(null != role)
      wrapped match {
        case p: MultiPlayer[_] => addPlaysRelation[T, R](p.wrapped.asInstanceOf[T], role)
        case p: AnyRef => addPlaysRelation[T, R](p.asInstanceOf[T], role)
        case p => throw new RuntimeException(s"Only instances of 'IPlayer' or 'AnyRef' are allowed to play roles! You tried it with '$p'.")
      }
      this
    }

    override def <+>[R <: AnyRef : ClassTag](role: R): MultiPlayer[T] = play(role)

    override def drop[R <: AnyRef : ClassTag](role: R): MultiPlayer[T] = {
      removePlaysRelation[T, R](wrapped, role)
      this
    }

    override def <->[R <: AnyRef : ClassTag](role: R): MultiPlayer[T] = drop(role)

    override def getRoles: Seq[AnyRef] = plays.getRoles(this)

    def applyDynamic[E, A](name: String)(args: A*)(implicit dispatchQuery: DispatchQuery = DispatchQuery.empty): Either[SCROLLError, Seq[Either[SCROLLError, E]]] = {
      val core = getCoreFor(wrapped).last
      val results = mutable.ListBuffer.empty[Either[SCROLLError, E]]
      dispatchQuery.filter(plays.getRoles(core)).foreach(r => {
        ReflectiveHelper.findMethod(r, name, args).foreach(fm => {
          args match {
            case Nil => results += dispatch(r, fm)
            case _ => results += dispatch(r, fm, args)
          }
        })
      })
      if (results.isEmpty) {
        // give up
        Left(RoleNotFound(core.toString, name, args))
      }
      else {
        Right(results)
      }
    }

    def applyDynamicNamed[E](name: String)(args: (String, Any)*)(implicit dispatchQuery: DispatchQuery = DispatchQuery.empty): Either[SCROLLError, Seq[Either[SCROLLError, E]]] =
      applyDynamic(name)(args.map(_._2): _*)(dispatchQuery)

    def selectDynamic[E](name: String)(implicit dispatchQuery: DispatchQuery = DispatchQuery.empty): Either[SCROLLError, Seq[Either[SCROLLError, E]]] = {
      val core = getCoreFor(wrapped).last
      val results = mutable.ListBuffer.empty[Either[SCROLLError, E]]
      dispatchQuery.filter(plays.getRoles(core)).filter(ReflectiveHelper.hasMember(_, name)).foreach(r => {
        results += ReflectiveHelper.propertyOf(r, name)
      })
      if (results.isEmpty) {
        // give up
        Left(RoleNotFound(core.toString, name, Seq.empty))
      }
      else {
        Right(results)
      }
    }

    def updateDynamic(name: String)(value: Any)(implicit dispatchQuery: DispatchQuery = DispatchQuery.empty): Unit = {
      val core = getCoreFor(wrapped).last
      dispatchQuery.filter(plays.getRoles(core)).filter(ReflectiveHelper.hasMember(_, name)).foreach(ReflectiveHelper.setPropertyOf(_, name, value))
    }


    override def equals(o: Any): Boolean = o match {
      case other: MultiPlayer[_] => getCoreFor(wrapped) == getCoreFor(other.wrapped)
      case other: Any => getCoreFor(wrapped) match {
        case Nil => false
        case p :: Nil => p == other
        case _ => false
      }
      case _ => false // default case
    }

    override def hashCode(): Int = wrapped.hashCode()
  }

}
