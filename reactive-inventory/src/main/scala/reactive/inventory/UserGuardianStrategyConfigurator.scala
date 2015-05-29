package reactive.inventory

import akka.actor.SupervisorStrategy
import akka.actor.SupervisorStrategy._
import akka.actor.SupervisorStrategyConfigurator
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy
//This is the strategy used by the top level Actor to handle failures
class UserGuardianStrategyConfigurator extends SupervisorStrategyConfigurator {

  def create(): SupervisorStrategy = {
    OneForOneStrategy() {
      case _ => Restart
    }
  }
}
