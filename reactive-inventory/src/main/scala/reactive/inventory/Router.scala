package reactive.inventory

import akka.actor.{OneForOneStrategy, SupervisorStrategy}

trait Router {
  //restart member of pool on error
  val oneForOneSupervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case e ⇒ SupervisorStrategy.Restart
  }
}