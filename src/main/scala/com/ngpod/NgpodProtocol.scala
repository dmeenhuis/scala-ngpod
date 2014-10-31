package com.ngpod

object NgpodProtocol {
  case object Init
  case class ProcessDocument(url: String)
  case class StartRunner(url: String)
  case class ParseDocument(url: String)
  case class ParsedDocument(data: ParsedData)
  case class DownloadImage(imageUrl: String, date: String)
  case object ImageDownloaded
  case class ParsedData(imageUrl: String, nextUrl: String, date: String)
}
