package ua.nure.lb5_6

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

class Speleologist {

  private var navRef: ActorRef[Navigator.ActionRequest] = _
  private var envRef: ActorRef[Environment.Request] = _

  private var environmentBehaviorRef: ActorRef[Environment.Response] = _
  private var navigatorBehaviorRef: ActorRef[Navigator.ActionResponse] = _

  private var gameState: ActionResult = KeepGoing

  def setupActor(navRef: ActorRef[Navigator.ActionRequest], envRef: ActorRef[Environment.Request]): Behavior[Any] =
    Behaviors.setup(context => {
      this.navRef = navRef
      this.envRef = envRef

      if (environmentBehaviorRef == null) {
        environmentBehaviorRef = context.spawn(environmentBehavior, "speleologist-behavior")
        navigatorBehaviorRef = context.spawn(navigatorBehavior, "speleologist-navigator")
      }

      envRef ! Environment.EnvironmentRequest(environmentBehaviorRef)

      Behaviors.stopped
    })

  private def environmentBehavior: Behavior[Environment.Response] = Behaviors.receive[Environment.Response]((context, message) => {
    message match {
      case Environment.EnvironmentResponse(percept) =>
        navRef ! Navigator.ActionRequest(percept, "", navigatorBehaviorRef)

        Behaviors.same

      case Environment.ActionResponse(actionResult: ActionResult) =>
        this.gameState = actionResult
        Behaviors.same
    }
  })

  private def navigatorBehavior: Behavior[Navigator.ActionResponse] = Behaviors.receive[Navigator.ActionResponse]((context, message) => {
    envRef ! Environment.PerformAction(message.action, environmentBehaviorRef)

    Behaviors.same
  })

}
