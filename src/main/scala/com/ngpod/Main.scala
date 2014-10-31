package com.ngpod

import akka.actor._
import com.ngpod.NgpodProtocol._

object Main extends App {

  implicit val system = ActorSystem("NGPOD")
  val runner = system.actorOf(Props[Runner], "Runner")

  runner ! ProcessDocument("http://photography.nationalgeographic.com/photography/photo-of-the-day/")
}











