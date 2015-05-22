package reactive.inventory

import akka.actor.{OneForOneStrategy, SupervisorStrategy}

trait Router {
  //resume member of pool on error
  val oneForOneSupervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case e ⇒ SupervisorStrategy.Resume
  }
}