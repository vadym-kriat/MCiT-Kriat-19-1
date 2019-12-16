package ua.nure.lb5_6

import akka.actor.{Actor, ActorSystem, Props}

class HelloActor extends Actor {
  def receive: PartialFunction[Any, Unit] = {
    case "hello" => println("hello back at you")
    case _       => println("huh?")
  }
}

object Main extends App {
  val system = ActorSystem("HelloSystem")
  val helloActor = system.actorOf(Props[HelloActor], name = "helloactor")
  helloActor ! "hello"
  helloActor ! "buenos dias"
  helloActor ! "213123"
}
