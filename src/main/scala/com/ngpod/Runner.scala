package com.ngpod

import java.util.concurrent.TimeUnit
import akka.actor._
import akka.routing._
import akka.util.Timeout
import com.ngpod.NgpodProtocol._
import scala.util._

class Runner extends Actor with ActorLogging {

  val parseActor = context.actorOf(Props[DocumentParseActor].withRouter(RoundRobinPool(nrOfInstances = 100)), "DocumentParser")
  val downloadActor = context.actorOf(Props[DownloadImageActor].withRouter(RoundRobinPool(nrOfInstances = 100)), "ImageDownloader")

  implicit val timeout = Timeout(30, TimeUnit.SECONDS)

  def receive: Receive = {

    case ProcessDocument(url) =>
      log.info(s"Received URL to process: $url")

      parseActor ! ParseDocument(url)

    case ParsedDocument(data) =>
      log.info(s"Received next URL to process: ${data.nextUrl}")

      parseActor ! ParseDocument(data.nextUrl)

      log.info(s"Received image to download: ${data.imageUrl}")
      downloadActor ! DownloadImage(data.imageUrl, data.date)

    case Failure(e) =>
      log.error(e, "Error occurred")
      self ! PoisonPill
  }
}
