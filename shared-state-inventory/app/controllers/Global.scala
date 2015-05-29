package controllers

import akka.actor.ActorSystem
import play.api.GlobalSettings

class Global extends GlobalSettings {

  var akkaSystem: ActorSystem = _

  override def onStart(app: play.api.Application): Unit = {
    akkaSystem = ActorSystem("shared-state-inventory")
  }
}
