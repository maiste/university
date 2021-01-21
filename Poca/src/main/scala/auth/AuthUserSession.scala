/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package poca.auth

import com.softwaremill.session.{
  SessionSerializer,
  SingleValueSessionSerializer
}
import poca.auth.AuthSessionSerializer
import poca.Basket
import poca.BasketToken

import scala.util.Try

case class AuthUserSession(id: String, basket: BasketToken) {}

object AuthUserSession {
  implicit def serializer: SessionSerializer[AuthUserSession, String] =
    new AuthSessionSerializer()

  def apply(id: String): AuthUserSession = {
    AuthUserSession(id, "")
  }
  def apply: AuthUserSession = {
    AuthUserSession("", "")
  }
}
