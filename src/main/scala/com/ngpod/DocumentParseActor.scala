package com.ngpod

import akka.actor._
import com.ngpod.NgpodProtocol._
import spray.client.pipelining._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util._

class DocumentParseActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case ParseDocument(url) =>
      val originalSender = sender()

      log.info(url)

      val future = for{
        html <- readDocument(url)
        data <- parseDocument(html)
      } yield data

      future onComplete {
        case Success(opt: Option[ParsedData]) =>

          opt match {
            case Some(x) =>
              originalSender ! ParsedDocument(x)
            case None =>
              originalSender ! Status.Failure(new Exception("Could not process URL"))
          }

        case Failure(error) =>
          log.error(error, "Could not process URL")
          originalSender ! Status.Failure(new Exception("Could not process URL"))
      }
  }

  def readDocument(url: String): Future[String] = {
    val pipeline = sendReceive ~> unmarshal[String]

    pipeline {
      Get(url)
    }
  }

  def parseDocument(html: String): Future[Option[ParsedData]] = Future {
    val imagePattern = """<div\s+class=\"primary_photo\">\s*<a\s+href=\"([^\"]+)\"(.*)\s*<img src=\"([^\"]+)\"\s+width""".r
    val date = """<p class=\"publication_time\">([a-zA-Z]+) ([\d]{1,2}), ([\d]{4})</p>""".r findFirstMatchIn html
      match {
        case Some(m) =>
          m.group(3) + toMonthNumber(m.group(1)) + "%02d".format(m.group(2).toInt)
        case None =>
          ""
    }

    imagePattern.findFirstMatchIn(html) match {
      case Some(m) =>
        val result = ParsedData(
          "http:" + m.group(3),
          "http://photography.nationalgeographic.com" + m.group(1),
          date)

        log.info(result.toString)

        Some(result)
      case None =>
        None
    }
  }

  def toMonthNumber(monthName: String) = {
    val months = Array("january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december")

    "%02d".format(months.indexWhere(x => x == monthName.toLowerCase) + 1)
  }
}
