/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package poca

import akka.http.scaladsl.model.{
  ContentTypes,
  HttpEntity,
  HttpResponse,
  MessageEntity,
  StatusCodes
}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl._
import java.nio.file.Paths
import akka.http.scaladsl.model.HttpEntity.{Chunked, ChunkStreamPart}
import akka.http.scaladsl.model.StatusCode

object StaticPages {
  private def createStaticSource(fileName: String) =
    FileIO
      .fromPath(Paths get fileName)
      .map(ChunkStreamPart.apply)

  private def createChunkedSource(fileName: String) =
    Chunked(ContentTypes.`text/html(UTF-8)`, createStaticSource(fileName))

  /**
    * Fetches an error static page.
    *
    * @param name
    * @return HttpResponse with a status code static chunked entity
    */
  def html(code: StatusCode): HttpResponse =
    HttpResponse(
      status = code,
      entity = createChunkedSource(s"static/html/${code.intValue}.html")
    )

  /**
    * Fetches a valid static page.
    *
    * @param name
    * @return HttpResponse with a valid static chunked entity
    */
  def html(name: String): HttpResponse =
    HttpResponse(
      status = 200,
      entity = createChunkedSource(s"static/html/${name}.html")
    )
}
