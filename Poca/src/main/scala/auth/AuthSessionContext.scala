/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package poca.auth

import com.softwaremill.session.{
  SessionConfig,
  SessionManager,
  SessionSerializer,
  BasicSessionEncoder
}
import com.softwaremill.session.CsrfDirectives._
import com.softwaremill.session.CsrfOptions._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.softwaremill.session._
import com.typesafe.scalalogging.LazyLogging
import poca.auth.AuthUserSession

object AuthSessionContext extends LazyLogging {
  implicit val executionContext =
    scala.concurrent.ExecutionContext.Implicits.global
  implicit val sessionConfig = SessionConfig.fromConfig()
  implicit val sessionManager =
    new SessionManager[AuthUserSession](sessionConfig)

  implicit val refreshTokenStorage =
    new InMemoryRefreshTokenStorage[AuthUserSession] {
      def log(msg: String) = logger.info(msg)
    }

  def authSetSession(v: AuthUserSession) =
    setSession(refreshable, usingCookies, v)

  val authRequiredSession   = requiredSession(refreshable, usingCookies)
  val authInvalidateSession = invalidateSession(refreshable, usingCookies)
  val authOptionalSession   = optionalSession(refreshable, usingCookies)

}
