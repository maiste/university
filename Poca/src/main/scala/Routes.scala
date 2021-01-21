/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package poca

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.{
  ContentTypes,
  HttpEntity,
  HttpResponse,
  MessageEntity,
  StatusCodes
}
import com.typesafe.scalalogging.LazyLogging
import TwirlMarshaller._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.PathMatchers.Remaining
import akka.http.scaladsl.model.HttpEntity.{ChunkStreamPart, Chunked}
import akka.stream.scaladsl._
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

import play.twirl.api.HtmlFormat
import poca.StaticPages
import com.softwaremill.session.CsrfDirectives._
import com.softwaremill.session.CsrfOptions._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.softwaremill.session._
import poca.auth.AuthSessionContext
import poca.auth.AuthUserSession

import scala.concurrent.duration.{Duration, DurationInt, MICROSECONDS, SECONDS}
import scala.runtime.Static

class Routes(users: Users, products: Products, orders: Orders)
    extends LazyLogging {
  implicit val executionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  val sessionContext = AuthSessionContext

  def getHello() = {
    logger.info("I got a request to greet.")
    HttpEntity(
      ContentTypes.`text/html(UTF-8)`,
      "<h1>Say hello to akka-http</h1>"
    )
  }

  def getSignup() = {
    logger.info("I got a request for signup.")
    html.main("Poca - Sign-up", html.signup())
  }

  def getSignin() = {
    logger.info("I got a request for signin.")
    html.main("Poca - Sign-in", html.signin())
  }

  def getChangePassword() = {
    logger.info("I got a request for changing password")
    html.main("Password change", html.password())
  }

  def emptyFields(): Future[HttpResponse] = {
    Future(
      HttpResponse(
        StatusCodes.BadRequest,
        entity = "Field 'username' or 'password' not found."
      )
    )
  }

  def updateProduct(fields: Map[String, String]) = {
    logger.info("I got a request for update product quantity.")

    (fields.get("id"), fields.get("quantity")) match {
      case (Some(id), Some(quantity)) => {
        val realquantity = if (quantity.toInt < 0) {
          0
        } else {
          quantity.toInt
        }
        products.updateProductQuantity(id, realquantity)
      }
      case _ => { () }
    }
  }

  def connection(fields: Map[String, String]): Future[Option[String]] = {
    logger.info("I got a request for connection.")

    (fields.get("username"), fields.get("password")) match {
      case (Some(username), Some(password)) => {
        val user: Future[Option[User]] = users.getUserByUsername(username)

        user.map(us =>
          us match {
            case None => {
              logger.info("Invalid user.")
              None
            } // username doesn't exist
            case Some(user) =>
              if (users.checkUserPassword(user, password)) {
                Some(user.userId)
              } else None
          }
        )
      }
      case _ => {
        Future { None }
      }
    }
  }

  def updateOrderStatus(fields: Map[String, String]) = {
    (fields.get("id"), fields.get("status")) match {
      case (Some(id), Some(status)) => orders.updateOrderStatusS(id, status)
      case _                        => ()
    }
    redirect("/sellerOrders", StatusCodes.SeeOther)
  }

  def modifyProfile(fields: Map[String, String]): Future[HttpResponse] = {
    logger.info(s"I got a request to change the profile")
    (
      fields.get("username"),
      fields.get("prev_password"),
      fields.get("new_password"),
      fields.get("email")
    ) match {
      case (
            Some(username),
            Some(previousPassword),
            Some(newPassword),
            Some(email)
          ) => {
        val user: Future[Option[User]] = users.getUserByUsername(username)
        user.map {
          case None => {
            HttpResponse(
              StatusCodes.BadRequest,
              entity = s"Username: '$username' doesn't exist."
            )
          }
          case Some(user) => {
            if (users.checkUserPassword(user, previousPassword)) {
              if (!newPassword.isEmpty()) {
                users.updatePassword(user.username, newPassword)
              }
              if (!email.isEmpty()) {
                users.updateMail(user.username, email)
              }
              HttpResponse(
                StatusCodes.OK,
                entity = s"Your profile has been correctly updated."
              )
            } else {
              HttpResponse(
                StatusCodes.BadRequest,
                entity = s"The password is incorrect."
              )
            }
          }
        }
      }
      case _ => emptyFields()
    }

  }

  def register(fields: Map[String, String]): Future[HttpResponse] = {
    logger.info("I got a request to register.")

    (
      fields.get("username"),
      fields.get("password"),
      fields.get("email")
    ) match {
      case (Some(username), Some(password), Some(email)) => {
        // Password checks (length/format) should go before next line
        val userCreation: Future[Unit] = users.createUser(
          username = username,
          unencrypted_password = password,
          email = email
        )

        userCreation
          .map(_ => {
            HttpResponse(
              StatusCodes.OK,
              entity =
                s"Welcome '$username'! You've just been registered to our great marketplace."
            )
          })
          .recover({
            case exc: UserAlreadyExistsException => {
              HttpResponse(
                StatusCodes.OK,
                entity =
                  s"The username '$username' is already taken. Please choose another username."
              )
            }
            case exc: MailException => {
              HttpResponse(
                StatusCodes.BadRequest,
                entity =
                  s"The mail '$email' is not well format. Check that you write it correctly or choose another one."
              )
            }
          })
      }
      case _ => {
        emptyFields()
      }
    }
  }

  def getUsers() = {
    logger.info("I got a request to get user list.")

    val userSeqFuture: Future[Seq[User]] = users.getAllUsers()

    userSeqFuture.map(userSeq => html.main("Poca - Users", html.users(userSeq)))
  }

  def getProfile(
      id: String,
      connected: Option[User] = None
  ): ToResponseMarshallable = {
    logger.info("I got a request to get profile: " + id)

    connected match {
      case Some(value) =>
        html.main(
          "Poca - Profile",
          html.profile(profile = value, connected = connected)
        )
      case None => {
        getNotFound(s"User ID: $id")
      }
    }
  }

  def getSeller(id: String) = {
    logger.info("I got a request for a selle page")
    val seller = new Seller(id)
    seller
      .getProducts(products)
      .map(productsSeq =>
        html.main("Poca - Products", html.seller(productsSeq))
      )
  }

  def getProducts() = {
    logger.info("I got a request for products list.")
    val productsSeqFuture: Future[Seq[Product]] = products.getAllProducts()
    productsSeqFuture.map(productsSeq =>
      html.main("Poca - Products", html.products(productsSeq))
    )
  }

  def getProduct(id: String) = {
    val product = products.getProductById(id)
    product.map[ToResponseMarshallable] {
      case Some(value) =>
        users.getUserById(value.seller).map[ToResponseMarshallable] {
          case Some(user) =>
            html.main(
              s"Poca - Product: ${value.name}",
              html.product(product = value, user = user.username)
            )
          case None =>
            html.main(
              s"Poca - Product: ${value.name}",
              html.product(product = value, user = "e-scaladur")
            )
        }
      case None => {
        getNotFound(s"Product ID: $id")
      }
    }
  }

  def basketRoute(session: AuthUserSession) = {
    onComplete(session.basket.getItems()(products)) { ctx =>
      ctx match {
        case Success(basketItems) => {
          val can_buy = basketItems.forall(p =
            basketItems => basketItems.count <= basketItems.product.quantity
          )

          complete(html.main("Your Basket", html.basket(basketItems, can_buy)))
        }
        case Failure(e) => {
          logger.error("Failure to get products from basket")
          complete(StaticPages.html(500))
        }
      }
    }
  }

  def deleteProfile(maybeString: Option[String]) = {
    if (maybeString.isEmpty) {
      logger.info("User ID is missing")
    } else {
      users.deleteUserById(maybeString.get)
    }
  }

  def getNotFound(name: String) = {
    logger.info("Invalid path requested: " + name)
    Future(
      StaticPages.html(StatusCodes.NotFound)
    )
  }

  def loginRoute =
    post {
      formFieldMap { e =>
        logger.info(s"Logging in $e")
        onSuccess(connection(e)) { b =>
          b match {
            case Some(id) =>
              sessionContext.authOptionalSession { session =>
                session match {
                  case Some(existing_body) =>
                    logger.debug("Cookie previously set")
                    val basket = existing_body.basket
                    sessionContext.authSetSession(AuthUserSession(id, basket)) {
                      logger.debug("Setting new CSRF token")
                      setNewCsrfToken(
                        checkHeader(sessionContext.sessionManager)
                      ) { ctx =>
                        logger.debug("Redirecting")
                        ctx.redirect(
                          s"/profile?userId=${id}",
                          StatusCodes.SeeOther
                        )
                      }
                    }
                  case None =>
                    logger.debug("No cookie previously set")
                    sessionContext.authSetSession(
                      AuthUserSession(id, new Basket())
                    ) {
                      logger.debug("Setting new CSRF token")
                      setNewCsrfToken(
                        checkHeader(sessionContext.sessionManager)
                      ) { ctx =>
                        logger.debug("Redirecting")
                        ctx.redirect(
                          s"/profile?userId=${id}",
                          StatusCodes.SeeOther
                        )
                      }
                    }
                }
              }

            case None => complete(StaticPages.html(403))
          }
        }
      }
    }

  def addBasket(id: String) = {
    sessionContext.authOptionalSession { session =>
      session match {
        case Some(body) => {
          var basket: Basket = body.basket
          basket.addProduct(id)
          logger.info(
            s"Added product ${id} to basket. Basket is now ${basket.toToken}"
          )
          sessionContext.authSetSession(AuthUserSession(body.id, basket)) {
            setNewCsrfToken(checkHeader(sessionContext.sessionManager)) { ctx =>
              ctx.redirect("/basket", StatusCodes.SeeOther)
            }
          }
        }
        case None => {
          var basket = new Basket()
          basket.addProduct(id)
          sessionContext.authSetSession(AuthUserSession("", basket)) {
            setNewCsrfToken(checkHeader(sessionContext.sessionManager)) { ctx =>
              ctx.redirect("/basket", StatusCodes.SeeOther)
            }
          }
        }
      }
    }
  }

  def removeFromBasket(id: String, all: Boolean) = {
    sessionContext.authOptionalSession {
      case Some(body) => {
        val basket: Basket = body.basket
        if (all) basket.deleteProduct(id) else basket.removeProduct(id)
        logger.info(
          s"Product ${id} removed from basket. Basket is now ${basket.toToken}"
        )
        sessionContext.authSetSession(AuthUserSession(body.id, basket)) {
          setNewCsrfToken(checkHeader(sessionContext.sessionManager)) { ctx =>
            ctx.redirect("/basket", StatusCodes.SeeOther)
          }
        }
      }
      case None => complete("/basket")
    }
  }

  // TODO: confirmation when user buy his basket
  def buyBasket(session: AuthUserSession) = {
    val basket: Basket = session.basket
    onComplete(session.basket.getItems()(products)) {
      _ match {
        case Success(basketItems) => {
          basketItems.map(basketItem => {
            val product  = basketItem.product
            val quantity = basketItem.count
            products.buyProduct(product.id, quantity) match {
              case None =>
                logger.info(s"Can't buy product ${product.id} of the basket.")
              case Some(real) => {
                val user = session.id
                var stat = new Requested()
                orders.createOrder(
                  product.id,
                  user,
                  product.seller,
                  quantity,
                  stat.asInstanceOf[Status]
                )
                logger.info(s"Order created for product ${product.id}")
                basket.deleteProduct(product.id)
                logger.info(
                  s"Product ${product.id} removed from basket and buy."
                )

              }
            }
          })
          sessionContext.authSetSession(AuthUserSession(session.id, basket)) {
            setNewCsrfToken(checkHeader(sessionContext.sessionManager)) { ctx =>
              ctx.redirect("/basket", StatusCodes.SeeOther)
            }
          }
        }
        case Failure(e) => {
          logger.error("Failure to get products from basket")
          complete(StaticPages.html(500))
        }
      }
    }
  }

  def addStock(fields: Map[String, String]) = {
    sessionContext.authOptionalSession {
      case Some(body) => {
        val userId_cookie = body.id
        val seller        = new Seller(userId_cookie)
        (
          fields.get("name"),
          fields.get("description"),
          fields.get("image"),
          fields.get("price"),
          fields.get("quantity")
        ) match {
          case (
                Some(name),
                Some(description),
                Some(imageUrl),
                Some(price),
                Some(quantity)
              ) => {
            seller.addProduct(
              products,
              (name, description, List(imageUrl), price, quantity)
            )
            redirect("/seller", StatusCodes.Found)
          }
          case _ => complete(StaticPages.html(500))
        }
      }
      case None =>
        logger.debug("I don't have a session.")
        redirect("/seller", StatusCodes.SeeOther)
    }
  }

  val profileRoute = {
    get {
      parameters("userId") { (userId) =>
        logger.debug(s"I got a request for profile of ${userId}.")
        sessionContext.authOptionalSession { session =>
          session match {
            case Some(body) =>
              logger.debug("I have a session.")
              val userId_cookie = body.id
              if (userId_cookie == userId) {
                onSuccess(users.getUserById(userId)) { userOption =>
                  complete(getProfile(userId, userOption))
                }
              } else {
                redirect("/signin", StatusCodes.SeeOther)
              }
            case None =>
              logger.debug("I don't have a session.")
              redirect("/signin", StatusCodes.SeeOther)
          }
        }
      }
    }
  }

  def ordersRoute(session: AuthUserSession) = {
    val userId                         = session.id
    val userOrders: Future[Seq[Order]] = orders.getAllOrdersForUserId(userId)
    val orderPlusProductSeq : Future[Seq[(OrderUser, Product)]] = userOrders.map(orders =>
      orders.map(order => { 
        val o = order.toOrderUser(users) match {
          case Some(v) => v
          case None => order.toDefaultOrderUser
        }
        val product = products.getProductById(order.productId)
        Await.result(
          product,
          Duration(10, SECONDS)
        ) match {
          case Some(prod) =>
            (o, prod)
          case None => (o, new Product)
        }
      })
    )
    val res = orderPlusProductSeq.map(tuple =>
      tuple.foldLeft(List(): List[List[(OrderUser, Product)]]) { (acc, tuple_val) =>
        try {
          val first_elem  = acc.head
          val first_tuple = first_elem.head
          if (
            first_tuple._1.date.toString
              .dropRight(4) == tuple_val._1.date.toString.dropRight(4)
          ) {
            (tuple_val :: first_elem) :: acc.tail
          } else
            (List(tuple_val)) :: acc
        } catch {
          case _: Throwable =>
            (List(tuple_val)) :: acc
        }
      }
    )
    res.map(list_of_list_of_tuple =>
      html.main("Orders", html.orders(list_of_list_of_tuple))
    )
  }

  def ordersRouteS(session: AuthUserSession) = {
    val sellerId = session.id
    val userOrders: Future[Seq[Order]] =
      orders.getAllOrdersForSellerId(sellerId)
    val orderPlusProductSeq = userOrders.map(orders =>
      orders.map(order => {
        val o = order.toOrderSeller(users) match {
          case Some(v) => v
          case None    => order.toDefaultOrderSeller
        }
        val product = products.getProductById(order.productId)
        Await.result(
          product,
          Duration(10, SECONDS)
        ) match {
          case Some(prod) =>
            (o, prod)
          case None => (o, new Product)
        }
      })
    )
    orderPlusProductSeq.map(tuple =>
      html.main("SellerOrders", html.sellerOrders(tuple))
    )
  }

  val routes: Route =
    concat(
      path("") {
        redirect("/signin", StatusCodes.PermanentRedirect)
      },
      path("hello") {
        get {
          complete(getHello)
        }
      },
      path("signup") {
        get {
          complete(getSignup)
        }
      },
      path("signin") { // TODO: Protect
        get {
          sessionContext.authOptionalSession { session =>
            session match {
              case Some(body) =>
                if (body.id == "") { complete(getSignin) }
                else {
                  redirect(s"/profile?userId=${body.id}", StatusCodes.SeeOther)
                }
              case None => complete(getSignin)
            }
          }
        }
      },
      path("password") { // TODO: Protect
        get {
          complete(getChangePassword)
        }
      },
      path("users") {
        get {
          complete(getUsers)
        }
      },
      path("profile") {
        profileRoute
      },
      path("products") {
        get {
          complete(getProducts)
        }
      },
      path("seller") {
        get {
          sessionContext.authOptionalSession { session =>
            session match {
              case Some(body) =>
                if (body.id == "") { redirect("/signin", StatusCodes.SeeOther) }
                else { complete(getSeller(body.id)) }
              case None => redirect("/signin", StatusCodes.SeeOther)
            }
          }
          //complete(getSignin)
        }
      },
      path("product") {
        get {
          parameters("productId") { (productId) =>
            complete(getProduct(productId))
          }
        }
      },
      pathPrefix("public") {
        path(Remaining) { name =>
          logger.info("Requesting resource: " + name)
          getFromResource(name)
        }
      },
      path("basket") {
        get {
          sessionContext.authOptionalSession {
            case Some(body) => basketRoute(body)
            case None       => redirect("/products", StatusCodes.SeeOther)
          }
        }
      },
      path("supply") {
        complete(html.main("Supply", html.supply()))
      },
      path("orders") {
        get {
          sessionContext.authOptionalSession {
            case Some(body) => complete(ordersRoute(body))
            case None       => redirect("/", StatusCodes.SeeOther)
          }
        }
      },
      path("sellerOrders") {
        get {
          sessionContext.authOptionalSession {
            case Some(body) => complete(ordersRouteS(body))
            case None       => redirect("/", StatusCodes.SeeOther)
          }
        }
      },
      // randomTokenCsrfProtection(checkHeader(sessionContext.sessionManager)) { // TODO: Faire fonctionner cette protection
      pathPrefix("api") {
        concat(
          path("update_product_quantity") {
            (post & formFieldMap) { fields =>
              updateProduct(fields)
              redirect("/seller", StatusCodes.SeeOther)
            }
          },
          path("account_removal") {
            (post & formFieldMap) { fields =>
              sessionContext.authSetSession(AuthUserSession("")) {
                setNewCsrfToken(checkHeader(sessionContext.sessionManager)) {
                  ctx =>
                    deleteProfile(fields.get("id"))
                    logger.info("The user was deleted")
                    ctx.redirect("/signin", StatusCodes.SeeOther)
                }
              }
            }
          },
          path("update_order_status") {
            (post & formFieldMap) { fields =>
              updateOrderStatus(fields)
            }
          },
          path("modify") { // TODO: Protect
            (post & formFieldMap) { fields =>
              complete(modifyProfile(fields))
            }
          },
          path("register") {
            (post & formFieldMap) { fields =>
              complete(register(fields))
            }
          },
          path("add_basket") {
            (post & formFieldMap) { fields =>
              fields.get("id") match {
                case Some(id) => addBasket(id)
                case None     => complete(StaticPages.html(404))
              }
            }
          },
          path("do_login") {
            loginRoute
          },
          // This should be protected and accessible only when logged in
          path("do_logout") {
            sessionContext.authSetSession(AuthUserSession("")) {
              setNewCsrfToken(checkHeader(sessionContext.sessionManager)) {
                ctx =>
                  logger.info("The user was logged out")
                  ctx.redirect("/signin", StatusCodes.SeeOther)
              }
            }
          },
          path("current_login") {
            get {
              sessionContext.authRequiredSession { session => ctx =>
                logger.info("Current session: " + session)
                ctx.complete(session.id)
              }
            }
          },
          path("remove_basket") {
            (post & formFieldMap) { fields =>
              fields.get("id") match {
                case Some(id) => removeFromBasket(id, true)
                case None     => complete(StaticPages.html(404))
              }
            }
          },
          path("buy_basket") {
            post {
              sessionContext.authOptionalSession {
                case Some(body) => buyBasket(body)
                case None       => redirect("/products", StatusCodes.SeeOther)
              }
            }
          },
          path("add_stock") {
            (post & formFieldMap) { fields =>
              addStock(fields)
            }
          },
          path("update_quantity") {
            (post & formFieldMap) { fields =>
              fields.get("update") match {
                case Some(value) => {
                  val param = value.split(",")
                  val sign  = param(0)
                  val id    = param(1)
                  sign match {
                    case "+" => addBasket(id)
                    case "-" => removeFromBasket(id, false)
                  }
                }
                case None => complete(StaticPages.html(404))
              }
            }
          }
        )
      },
      path(Remaining) { name =>
        complete(getNotFound(name))
      }
    )
}
