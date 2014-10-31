package com.ngpod

import java.io._
import akka.actor._
import com.ngpod.NgpodProtocol._
import spray.client.pipelining._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._

class DownloadImageActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case DownloadImage(imageUrl, date) =>
      val originalSender = sender()

      log.info(s"Downloading image published on $date from $imageUrl")

      val pipeline = sendReceive ~> unmarshal[Array[Byte]]

      val future = pipeline {
        Get(imageUrl)
      }

      val filename = date + "-" + imageUrl.substring(imageUrl.lastIndexOf("/") + 1)

      future onComplete {
        case Success(bytes) =>
          log.info("Success: " + filename)

          val path = new File("./images")
          path.mkdir()

          var stream: FileOutputStream = null

          try {
            stream = new FileOutputStream("./images/" + filename)
            stream.write(bytes)
          } finally {
            stream.close()
          }

          originalSender ! ImageDownloaded

        case Failure(e) =>
          log.error("Fail: " + e.getMessage)
          originalSender ! Status.Failure(e)
      }

      sender ! ImageDownloaded
  }
}
