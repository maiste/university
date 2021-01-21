/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

// From https://github.com/vmunier/akka-http-scalajs.g8
// MPL v2.0 is compatible with Apache License v2 of the above project.

package poca

import akka.http.scaladsl.marshalling.{Marshaller, _}
import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.MediaTypes._
import play.twirl.api.{Html, Txt, Xml}

object TwirlMarshaller {

  /** Twirl marshallers for Xml, Html and Txt mediatypes */
  implicit val twirlHtmlMarshaller = twirlMarshaller[Html](`text/html`)
  implicit val twirlTxtMarshaller = twirlMarshaller[Txt](`text/plain`)
  implicit val twirlXmlMarshaller = twirlMarshaller[Xml](`text/xml`)

  def twirlMarshaller[A](contentType: MediaType): ToEntityMarshaller[A] = {
    Marshaller.StringMarshaller.wrap(contentType)(_.toString)
  }
}
