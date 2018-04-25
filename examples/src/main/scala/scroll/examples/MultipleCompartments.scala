package scroll.examples

import java.time.LocalDate

import scroll.internal.Compartment

/** Example demonstrating how to play multiple roles in different compartments
  *
  */
object MultipleCompartments extends App {

  case class Person(name: String)

  class Association extends Compartment {

    case class Member(id: Int, joined: LocalDate)

  }

  class Company extends Compartment {

    case class Employee(id: Int, salary: Long)

  }

  val jane = Person("Jane Doe")

  val chessClub = new Association {
    jane play Member(42, LocalDate.of(1999, 11, 22))
  }

  val hipsterStartup = new Company {
    jane play Employee(24, 2345)
  }

  val allotmentClub = new Association {
    jane play Member(999, LocalDate.now())
  }

  val janeAsInChessClub = chessClub.one((p: Person) => p.name.equals("Jane Doe"))

  /*
   * There's quite a bit of unwrapping involved here:
   *  - first `one(...)` returns an Either for the matching object (on the Right), or an ScrollError
   *    (on the Left) otherwise
   *  - second the `lift(...)` returns an Option containing the player if it could be lifted
   *  - third the dispatch on the role method returns an Either again
   */
  println("## Chess club:")
  janeAsInChessClub match {
    case Left(msg) =>
      println(s"Something went utterly wrong: $msg :-(")
    case Right(member) =>
      println(s"Jane is here! $member")
      for {jane <- chessClub.lift(member)} {
        println(s"Jane's join date: ${(+jane).joined.right.get} - that's a looong time")

      }
  }

  // we may continue using the player instance from another compartment
  println("## Hipster startup:")
  janeAsInChessClub match {
    case Right(employee) =>
      println(s"Jane is still here! $employee")
      for {jane <- employee @: hipsterStartup} {
        println(s"Jane's salary: ${(+jane).salary.right.get} (still too low)")
      }
  }

  println("## Allotment club:")
  allotmentClub.one((p: Person) => p.name.contains("Jane")) match {
    case Right(member) =>
      println(s"Found Jane yet again: $member")
      for {jane <- member @: allotmentClub} {
        println(s"Jane's join date: ${(+jane).joined.right.get} - she's pretty new")
      }
  }

}
