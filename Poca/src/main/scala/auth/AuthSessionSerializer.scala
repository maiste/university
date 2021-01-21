package poca.auth

import poca.auth.AuthUserSession
import poca.Basket
import poca.BasketToken
import com.softwaremill.session.MultiValueSessionSerializer

import scala.util.Try

class AuthSessionSerializer
    extends MultiValueSessionSerializer[AuthUserSession](
      (aus: AuthUserSession) => Map("id" -> aus.id, "basket" -> aus.basket),
      (m: Map[String, String]) => Try(new AuthUserSession(m("id"), m("basket")))
    )
