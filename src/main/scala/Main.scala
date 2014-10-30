package com.ngpod

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import com.ngpod.NgpodProtocol._

import spray.client._
import spray.client.pipelining._

import scala.util._
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends App {

  implicit val system = ActorSystem("NGPOD")

  val runner = system.actorOf(Props[Runner])
  runner ! Init

  Thread.sleep(5000)
  system.shutdown()

}

class Runner extends Actor with ActorLogging {
  val parseActor = context.actorOf(Props[DocumentParseActor])
  val downloadActor = context.actorOf(Props[DownloadImageActor])

  implicit val timeout = Timeout(5, TimeUnit.SECONDS)


  override def receive: Receive = {
    case Init =>
      val future = parseActor ? ParseDocument("http://photography.nationalgeographic.com/photography/photo-of-the-day/")

      future onComplete {
        case Success(msg) =>
        case Failure(error) =>
          self ! PoisonPill
      }
  }


}

class DocumentParseActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case ParseDocument(url) =>
      log.info(url)

      val pipeline = sendReceive ~> unmarshal[String]

      val responseFuture = pipeline {
        Get(url)
      }

      responseFuture onComplete {
        case Success(response: String) =>
          val parsedData = parseDocument(response)


          sender ! ParsedDocument("")
        case Failure(error) =>
          log.error(error, "Could not download from URL")
          sender ! Status.Failure(error)
      }
  }

  def parseDocument(html: String): ParsedData = {
    //val photoIndex = html.indexOf("<div class=\"primary_photo\">")

    val regex = """<div\s+class=\"primary_photo\">\s*<a\s+href=\"([^\"]+)\"(.*)\s*<img src=\"([^\"]+)\"\s+width""".r

    regex.findFirstMatchIn(html) match {
      case Some(m) =>
        log.info(m.group(1))
      case None =>
    }



    ParsedData("", "", "")
  }

  case class ParsedData(imageUrl: String, nextUrl: String, date: String)
}

class DownloadImageActor extends Actor {
  override def receive: Receive = {
    case "" =>
  }
}

object NgpodProtocol {
  case object Init
  case class ParseDocument(url: String)
  case class ParsedDocument(nextUrl: String)
}



