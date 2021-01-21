/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

import scala.concurrent.Future
import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.RouteTestTimeout
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.model.{
  ContentTypes,
  FormData,
  HttpMethods,
  HttpRequest,
  StatusCodes
}
import akka.http.scaladsl.model.headers.Cookie
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.Matchers
import org.scalatest.funsuite.AnyFunSuite
import org.scalamock.scalatest.MockFactory

import concurrent.duration._
import poca.{
  Basket,
  MailException,
  MyDatabase,
  Order,
  Orders,
  Product,
  Products,
  Routes,
  User,
  UserAlreadyExistsException,
  Users,
  Requested,
}
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.StatusCode

import java.util.Date
import java.sql.Timestamp

class RoutesTest
    extends AnyFunSuite
    with Matchers
    with MockFactory
    with ScalatestRouteTest {

  // the Akka HTTP route testkit does not yet support a typed actor system (https://github.com/akka/akka-http/issues/2036)
  // so we have to adapt for now
  lazy val testKit = ActorTestKit()
  implicit def default(implicit system: ActorSystem) =
    RouteTestTimeout(10.second)
  implicit def typedSystem = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  test("Route GET /hello should say hello") {
    var mockUsers       = mock[Users]
    var mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    val request = HttpRequest(uri = "/hello")
    request ~> routesUnderTest ~> check {
      status should ===(StatusCodes.OK)

      contentType should ===(ContentTypes.`text/html(UTF-8)`)

      entityAs[String] should ===("<h1>Say hello to akka-http</h1>")
    }
  }

  test("Route GET /signup should returns the signup page") {
    var mockUsers       = mock[Users]
    var mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    val request = HttpRequest(uri = "/signup")
    request ~> routesUnderTest ~> check {
      status should ===(StatusCodes.OK)

      contentType should ===(ContentTypes.`text/html(UTF-8)`)
    }
  }

  test("Route POST /api/register should create a new user") {
    var mockUsers    = mock[Users]
    var mockProducts = mock[Products]
    var mockOrders   = mock[Orders]

    (mockUsers.createUser _)
      .expects("toto", "password", "test@test.fr", false)
      .returning(Future(()))
      .once()

    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = "/api/register",
      entity = FormData(
        ("username", "toto"),
        ("password", "password"),
        ("email", "test@test.fr")
      ).toEntity
    )
    request ~> routesUnderTest ~> check {
      status should ===(StatusCodes.OK)

      contentType should ===(ContentTypes.`text/plain(UTF-8)`)

      entityAs[String] should ===(
        "Welcome 'toto'! You've just been registered to our great marketplace."
      )
    }
  }

  test(
    "Route POST /api/register should warn the user when username is already taken"
  ) {
    var mockUsers    = mock[Users]
    var mockProducts = mock[Products]
    var mockOrders   = mock[Orders]

    (mockUsers.createUser _)
      .expects("toto", "password", "test@test.fr", false)
      .returns(Future({
        throw new UserAlreadyExistsException("")
      }))
      .once()

    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = "/api/register",
      entity = FormData(
        ("username", "toto"),
        ("password", "password"),
        ("email", "test@test.fr")
      ).toEntity
    )
    request ~> routesUnderTest ~> check {
      status should ===(StatusCodes.OK)

      contentType should ===(ContentTypes.`text/plain(UTF-8)`)

      entityAs[String] should ===(
        "The username 'toto' is already taken. Please choose another username."
      )
    }
  }

  test(
    "Route POST /api/register should raise exception if mail format isn't good"
  ) {
    var mockUsers    = mock[Users]
    var mockProducts = mock[Products]
    var mockOrders   = mock[Orders]

    (mockUsers.createUser _)
      .expects("toto", "password", "test.fr", false)
      .returns(Future({
        throw new MailException()
      }))
      .once()

    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = "/api/register",
      entity = FormData(
        ("username", "toto"),
        ("password", "password"),
        ("email", "test.fr")
      ).toEntity
    )
    request ~> routesUnderTest ~> check {
      status should ===(StatusCodes.BadRequest)
      contentType should ===(ContentTypes.`text/plain(UTF-8)`)
      entityAs[String] should ===(
        s"The mail 'test.fr' is not well format. Check that you write it correctly or choose another one."
      )
    }
  }

  test(
    "Route POST /api/register should return empty fields if fields are missing"
  ) {
    var mockUsers    = mock[Users]
    var mockProducts = mock[Products]
    var mockOrders   = mock[Orders]

    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = "/api/register",
      entity = FormData(
        ("username", "toto"),
        ("email", "test.fr")
      ).toEntity
    )
    request ~> routesUnderTest ~> check {
      status should ===(StatusCodes.BadRequest)
      contentType should ===(ContentTypes.`text/plain(UTF-8)`)
      entityAs[String] should ===(
        "Field 'username' or 'password' not found."
      )
    }
  }

  test("Route GET /users should display the list of users") {
    var mockUsers    = mock[Users]
    var mockProducts = mock[Products]
    var mockOrders   = mock[Orders]
    val userList = List(
      User(
        username = "riri",
        userId = "id1",
        password = "",
        email = "test@test.fr",
        isSeller = false
      ),
      User(
        username = "fifi",
        userId = "id2",
        password = "",
        email = "test@test.fr",
        isSeller = false
      ),
      User(
        username = "lulu",
        userId = "id2",
        password = "",
        email = "test@test.fr",
        isSeller = false
      )
    )
    (mockUsers.getAllUsers _).expects().returns(Future(userList)).once()

    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    val request = HttpRequest(uri = "/users")
    request ~> routesUnderTest ~> check {
      status should ===(StatusCodes.OK)

      contentType should ===(ContentTypes.`text/html(UTF-8)`)

      // entityAs[String].length should be(203)
    }
  }

  test("Route GET /aa should returns the error404 page") {
    var mockUsers       = mock[Users]
    var mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    val request = HttpRequest(uri = "/aa")
    request ~> routesUnderTest ~> check {
      status should ===(StatusCodes.NotFound)
      contentType should ===(ContentTypes.`text/html(UTF-8)`)
    }
  }

  test("Route GET /products should display the list of products") {
    var mockUsers    = mock[Users]
    var mockProducts = mock[Products]
    var mockOrders   = mock[Orders]

    val productList = List(
      Product(
        id = "1",
        name = "obj 1",
        description = " desc 1",
        imageLinks = "img1.jpg"
      ),
      Product(
        id = "2",
        name = "obj 2",
        description = "desc 2",
        imageLinks = "img2.jpg;img3.jpg"
      ),
      Product(id = "3", name = "obj 3", description = "desc 3", imageLinks = "")
    )
    (mockProducts.getAllProducts _)
      .expects()
      .returns(Future(productList))
      .once()

    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    val request = HttpRequest(uri = "/products")
    request ~> routesUnderTest ~> check {
      status should ===(StatusCodes.OK)
      contentType should ===(ContentTypes.`text/html(UTF-8)`)
    }
  }

  test("Route GET /product should display the product") {
    val mockUsers    = mock[Users]
    val mockProducts = mock[Products]
    var mockOrders   = mock[Orders]

    val product = Product(
      id = "0",
      name = "obj 1",
      description = " desc 1",
      imageLinks = "img1.jpg"
    )
    val user = User(
      username = "riri",
      userId = "0",
      password = "",
      email = "test@test.fr",
      isSeller = false
    )

    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes
    (mockProducts.getProductById _)
      .expects("0")
      .returns(Future(Some(product)))
      .anyNumberOfTimes()
    (mockProducts.getProductById _)
      .expects("38")
      .returns(Future(Option.empty[Product]))
      .once()
    (mockUsers.getUserById _).expects("0").returns(Future(Some(user))).once()

    // A product that does not exist
    Get("/product?productId=38") ~> routesUnderTest ~> check {
      contentType should ===(ContentTypes.`text/html(UTF-8)`)
      status should ===(StatusCodes.NotFound)
    }
    Get("/product?productId=0") ~> routesUnderTest ~> check {
      status should ===(StatusCodes.OK)
      contentType should ===(ContentTypes.`text/html(UTF-8)`)
      entityAs[String] should include("Selled by : riri")
    }

    (mockUsers.getUserById _).expects("0").returns(Future(None)).once()
    Get("/product?productId=0") ~> routesUnderTest ~> check {
      status should ===(StatusCodes.OK)
      contentType should ===(ContentTypes.`text/html(UTF-8)`)
      entityAs[String] should include("Selled by : e-scaladur")
    }
  }

  test("Route GET /profile?userId=hello shouldn't display the user hello") {
    val mockUsers       = mock[Users]
    val mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    // A profile that does not exist
    Get("/profile?userId=hello") ~> routesUnderTest ~> check {
      status should ===(StatusCodes.SeeOther)
    }

  }

  test("Route GET /profile?userId=0 should display user 0") {
    var mockUsers    = mock[Users]
    var mockProducts = mock[Products]
    var mockOrders   = mock[Orders]
    val user = User(
      userId = "0",
      username = "test",
      password = "test",
      email = "test@test.fr",
      isSeller = false
    )
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    Get("/profile?userId=0") ~> routesUnderTest ~> check {
      status should ===(StatusCodes.SeeOther)
      contentType should ===(ContentTypes.`text/html(UTF-8)`)
    }
  }

  test("Route GET / should redirect to /signin ") {
    var mockUsers       = mock[Users]
    var mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    Get("/") ~> routesUnderTest ~> check {
      status shouldEqual StatusCodes.PermanentRedirect
      responseAs[String] shouldEqual """The request, and all future requests should be repeated using <a href="/signin">this URI</a>."""
    }
  }

  test("Route GET /signin should return signin page if not signed in") {
    var mockUsers       = mock[Users]
    var mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    Get("/signin") ~> routesUnderTest ~> check {
      status shouldEqual StatusCodes.OK
      contentType should ===(ContentTypes.`text/html(UTF-8)`)
    }
  }

  def checkSetCookies(cookies: Array[String], true_status: StatusCode) = {
    check {
      status should ===(true_status)
      contentType should ===(ContentTypes.`text/html(UTF-8)`)

      for (i <- 0 to 2) {
        headers(i).name should ===("Set-Cookie")
        cookies(i) = headers(i).value().split(";")(0)
      }
    }
  }

  def requestCookies(request: HttpRequest, cookies: Array[String]) = {
    var r = request
    for (c <- cookies) {
      var s = c.split("=");
      r = r ~> Cookie(s(0) -> s(1))
    }
    r
  }

  def createSession(
      user: User,
      mockUsers: Users,
      mockProducts: Products,
      routesUnderTest: akka.http.scaladsl.server.Route,
      cookies: Array[String] = new Array[String](3)
  ) = {
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = "/api/do_login",
      entity = FormData(
        ("username", user.username),
        ("password", user.password)
      ).toEntity
    )

    (mockUsers.getUserByUsername _)
      .expects(user.username)
      .returns(Future(Some(user)))
      .anyNumberOfTimes()
    (mockUsers.checkUserPassword _)
      .expects(user, user.username)
      .returns(true)
      .once()

    val res = {
      if (cookies(0) == null) {
        request
      } else {
        requestCookies(request, cookies)
      }
    } ~> routesUnderTest

    res ~> checkSetCookies(cookies, StatusCodes.SeeOther)
    res ~> check {
      headers(3).name should ===("Location")
      headers(3).value should ===(s"/profile?userId=${user.userId}")
    }

    cookies
  }

  def idFromCookie(cookie: String): String = {
    cookie.split("~")(1).split("&")(0)
  }

  def idFromCookie(cookies: Array[String]): String = {
    var out = ""
    for (c <- cookies) {
      if (c.split("=")(0) == "_sessiondata") {
        out = idFromCookie(c)
      }
    }
    out
  }

  def basketFromCookie(cookie: String): Basket = {
    val s = cookie.split("~")
    if (s.length < 3) {
      new Basket()
    } else {
      new Basket(s(2).replaceAll("%3A", ":"))
    }
  }

  def basketFromCookie(cookies: Array[String]): Basket = {
    var out = new Basket
    for (c <- cookies) {
      if (c.split("=")(0) == "_sessiondata") {
        out = basketFromCookie(c)
      }
    }
    out
  }

  def createEmptySession(
      routesUnderTest: akka.http.scaladsl.server.Route,
      cookies: Array[String] = new Array[String](3)
  ): Array[String] = {
    val res = Post(
      "/api/add_basket",
      FormData(
        ("id", "0")
      )
    ) ~> routesUnderTest
    res ~> checkSetCookies(cookies, StatusCodes.SeeOther)
    res ~> check {
      headers(3).name should ===("Location")
      headers(3).value should ===("/basket")

      basketFromCookie(cookies).size should ===(1)
      idFromCookie(cookies) should ===("")
    }

    cookies
  }

  // def testCookies(body)

  test("Route POST /api/do_login should return cookies if user exists") {
    var mockUsers       = mock[Users]
    var mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    val user = User(
      userId = "0",
      username = "test",
      password = "test",
      email = "test@test.fr",
      isSeller = false
    )

    createSession(user, mockUsers, mockProducts, routesUnderTest)
  }

  test("Route POST /api/do_login should not login if user doesn't exist") {
    var mockUsers    = mock[Users]
    var mockProducts = mock[Products]
    var mockOrders   = mock[Orders]

    val user = User(
      userId = "0",
      username = "test",
      password = "test",
      email = "test@test.fr",
      isSeller = false
    )
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    (mockUsers.getUserByUsername _)
      .expects("test")
      .returns(Future(Some(user)))
      .anyNumberOfTimes()
    (mockUsers.checkUserPassword _).expects(user, "test").returns(false).once()

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = "/api/do_login",
      entity = FormData(("username", "test"), ("password", "test")).toEntity
    )

    request ~> routesUnderTest ~> check {
      status should ===(StatusCode.int2StatusCode(403))
    }
  }

  test(
    "Route POST /api/add_basket should add product to current basket whether a session is present or not"
  ) {
    var mockUsers       = mock[Users]
    var mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    val user = User(
      userId = "0",
      username = "test",
      password = "test",
      email = "test@test.fr",
      isSeller = false
    )

    var cookies = createSession(user, mockUsers, mockProducts, routesUnderTest)

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = "/api/add_basket",
      entity = FormData(
        ("id", "0")
      ).toEntity
    )

    val res = requestCookies(request, cookies) ~> routesUnderTest

    res ~> checkSetCookies(cookies, StatusCodes.SeeOther)
    res ~> check {
      headers(3).name should ===("Location")
      headers(3).value should ===("/basket")

      basketFromCookie(cookies).size should ===(1)
      idFromCookie(cookies) should ===("0")
    }

    val res2 = request ~> routesUnderTest
    res2 ~> checkSetCookies(cookies, StatusCodes.SeeOther)
    res2 ~> check {
      headers(3).name should ===("Location")
      headers(3).value should ===("/basket")

      basketFromCookie(cookies).size should ===(1)
      idFromCookie(cookies) should ===("")
    }

    Post("/api/add_basket") ~> routesUnderTest ~> check {
      status should ===(StatusCodes.NotFound)
    }
  }

  test(
    "Route POST /api/do_login should keep basket of empty session"
  ) {
    var mockUsers       = mock[Users]
    var mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    val user = User(
      userId = "0",
      username = "test",
      password = "test",
      email = "test@test.fr",
      isSeller = false
    )

    var cookies = createEmptySession(routesUnderTest)

    cookies =
      createSession(user, mockUsers, mockProducts, routesUnderTest, cookies)
    basketFromCookie(cookies).size should ===(1)
  }

  test(
    "Route POST /api/remove_basket tests"
  ) {
    var mockUsers       = mock[Users]
    var mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    var cookies = createEmptySession(routesUnderTest)

    val request2 = HttpRequest(
      method = HttpMethods.POST,
      uri = "/api/remove_basket",
      entity = FormData(
        ("id", "0")
      ).toEntity
    )

    val res2 = requestCookies(request2, cookies) ~> routesUnderTest
    res2 ~> checkSetCookies(cookies, StatusCodes.SeeOther)
    res2 ~> check {
      headers(3).name should ===("Location")
      headers(3).value should ===("/basket")

      basketFromCookie(cookies).size should ===(0)
      idFromCookie(cookies) should ===("")
    }

    request2 ~> routesUnderTest ~> check {
      status should ===(StatusCodes.OK)
    }

    val request3 = HttpRequest(
      method = HttpMethods.POST,
      uri = "/api/remove_basket"
    )

    request3 ~> routesUnderTest ~> check {
      status should ===(StatusCodes.NotFound)
    }
  }

  test("Route GET /api/current_login test") {
    var mockUsers       = mock[Users]
    var mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    val user = User(
      userId = "0",
      username = "test",
      password = "test",
      email = "test@test.fr",
      isSeller = false
    )

    var cookies = createSession(user, mockUsers, mockProducts, routesUnderTest)

    requestCookies(
      Get("/api/current_login"),
      cookies
    ) ~> routesUnderTest ~> check {
      status should ===(StatusCodes.OK)
      responseAs[String] shouldEqual (s"${user.userId}")
    }
  }

  test("Route GET /password test") {
    var mockUsers       = mock[Users]
    var mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    Get("/password") ~> routesUnderTest ~> check(
      status should ===(StatusCodes.OK)
    )
  }

  test("Route POST /api/update_product_quantity tests") {
    var mockUsers    = mock[Users]
    var mockProducts = mock[Products]
    var mockOrders   = mock[Orders]
    val user = User(
      userId = "0",
      username = "test",
      password = "test",
      email = "test@test.fr",
      isSeller = true
    )
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes
    val form = FormData(
      ("id", "0"),
      ("quantity", "0")
    )
    try {
      Post("/api/update_product_quantity", form) ~> routesUnderTest ~> check {
        status should ===(StatusCodes.SeeOther)
      }
    } catch {
      case _: Exception => ()
    }
  }

  test("Route POST /api/modify tests") {
    var mockUsers       = mock[Users]
    var mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    Post("/api/modify") ~> routesUnderTest ~> check {
      status should ===(StatusCodes.BadRequest)
      entityAs[String] should ===("Field 'username' or 'password' not found.")
    }

    val user = User(
      userId = "0",
      username = "test",
      password = "test",
      email = "test@test.fr",
      isSeller = false
    )
    val new_password = "test2"
    val new_mail     = "test2@test.fr"
    val form = FormData(
      ("username", user.username),
      ("prev_password", user.password),
      ("new_password", new_password),
      ("email", new_mail)
    )

    (mockUsers.getUserByUsername _)
      .expects(user.username)
      .returns(Future(None))
      .once()

    Post("/api/modify", form) ~> routesUnderTest ~> check {
      status should ===(StatusCodes.BadRequest)
      entityAs[String] should ===(
        s"Username: '${user.username}' doesn't exist."
      )
    }

    (mockUsers.getUserByUsername _)
      .expects(user.username)
      .returns(Future(Some(user)))
      .anyNumberOfTimes()
    (mockUsers.checkUserPassword _)
      .expects(user, user.username)
      .returns(false)
      .once()

    Post("/api/modify", form) ~> routesUnderTest ~> check {
      status should ===(StatusCodes.BadRequest)
      entityAs[String] should ===(s"The password is incorrect.")
    }

    (mockUsers.checkUserPassword _)
      .expects(user, user.username)
      .returns(true)
      .once()
    (mockUsers.updatePassword _)
      .expects(user.username, new_password)
      .anyNumberOfTimes()
    (mockUsers.updateMail _).expects(user.username, new_mail).anyNumberOfTimes()

    Post("/api/modify", form) ~> routesUnderTest ~> check {
      status should ===(StatusCodes.OK)
      entityAs[String] should ===(s"Your profile has been correctly updated.")
    }
  }

  test("Route GET /basket test") {
    var mockUsers       = mock[Users]
    var mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    val u = User("test", "test", "test", "test@test.com", isSeller = false)

    var cookies = createSession(u, mockUsers, mockProducts, routesUnderTest)

    Get("/basket") ~> routesUnderTest ~> check(
      status should ===(StatusCodes.SeeOther)
    )

    requestCookies(Get("/basket"), cookies) ~> routesUnderTest ~> check {
      status should ===(StatusCodes.OK)
    }
  }

  test("Route POST /api/account_removal test") {
    var mockUsers       = mock[Users]
    var mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    val u = User("test", "test", "test", "test@test.com", isSeller = false)

    var cookies = createSession(u, mockUsers, mockProducts, routesUnderTest)

    Get("/basket") ~> routesUnderTest ~> check(
      status should ===(StatusCodes.SeeOther)
    )

    val res =
      requestCookies(Post("/api/account_removal"), cookies) ~> routesUnderTest
    res ~> checkSetCookies(cookies, StatusCodes.SeeOther)
    res ~> check {
      headers(3).name should ===("Location")
      headers(3).value should ===("/signin")

      idFromCookie(cookies) should ===("")
    }

    (mockUsers.deleteUserById _).expects("test").once()

    val res2 =
      requestCookies(
        Post("/api/account_removal", FormData(("id", "test"))),
        cookies
      ) ~> routesUnderTest
    res2 ~> checkSetCookies(cookies, StatusCodes.SeeOther)
    res2 ~> check {
      headers(3).name should ===("Location")
      headers(3).value should ===("/signin")

      idFromCookie(cookies) should ===("")
    }
  }

  test("Route POST /api/update_quantity test") {
    var mockUsers       = mock[Users]
    var mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    val u = User("test", "test", "test", "test@test.com", isSeller = false)

    var cookies = new Array[String](3)

    Post("/api/update_quantity") ~> routesUnderTest ~> check(
      status should ===(StatusCodes.NotFound)
    )

    val res =
      Post(
        "/api/update_quantity",
        FormData(("update", "+,0"))
      ) ~> routesUnderTest
    res ~> checkSetCookies(cookies, StatusCodes.SeeOther)
    res ~> check {
      headers(3).name should ===("Location")
      headers(3).value should ===("/basket")

      idFromCookie(cookies) should ===("")
      basketFromCookie(cookies).size should ===(1)
    }
    val res2 =
      requestCookies(
        Post("/api/update_quantity", FormData(("update", "-,0"))),
        cookies
      ) ~> routesUnderTest
    res2 ~> checkSetCookies(cookies, StatusCodes.SeeOther)
    res2 ~> check {
      headers(3).name should ===("Location")
      headers(3).value should ===("/basket")

      idFromCookie(cookies) should ===("")
      basketFromCookie(cookies).size should ===(0)
    }
  }

  test("Route GET /seller test") {
    var mockUsers       = mock[Users]
    var mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes
    val u               = User("test", "test", "test", "test@test.com", isSeller = false)
    val p               = Product("", "", "", "", 0, 0, "test")
    val ps              = new Array[Product](1)
    ps(0) = p

    var cookies = createEmptySession(routesUnderTest)

    Get("/seller") ~> routesUnderTest ~> check(
      status should ===(StatusCodes.SeeOther)
    )
    requestCookies(Get("/seller"), cookies) ~> routesUnderTest ~> check {
      status should ===(StatusCodes.SeeOther)
    }
    cookies = createSession(u, mockUsers, mockProducts, routesUnderTest)

    (mockProducts.filterProductsBySeller _)
      .expects(u.userId)
      .returns(Future(ps))
      .once()

    requestCookies(Get("/seller"), cookies) ~> routesUnderTest ~> check {
      status should ===(StatusCodes.OK)
    }
  }

  test("Route GET /signin other tests") {
    var mockUsers       = mock[Users]
    var mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    val u = User("test", "test", "test", "test@test.com", isSeller = false)

    var cookies = createSession(u, mockUsers, mockProducts, routesUnderTest)

    requestCookies(Get("/signin"), cookies) ~> routesUnderTest ~> check(
      status should ===(StatusCodes.SeeOther)
    )

    cookies = createEmptySession(routesUnderTest)

    requestCookies(Get("/signin"), cookies) ~> routesUnderTest ~> check(
      status should ===(StatusCodes.OK)
    )
  }

  test("Route POST /api/buy_basket tests") {
    var mockUsers       = mock[Users]
    var mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    val u  = User("test", "test", "test", "test@test.com", isSeller = false)
    val ep = new Product()
    val p  = new Product("0", "name", "desc", "link", 10, 1, "0")

    var cookies = createEmptySession(routesUnderTest)
    cookies =
      createSession(u, mockUsers, mockProducts, routesUnderTest, cookies)

    var cookies2 = cookies.clone()

    Post("/api/buy_basket") ~> routesUnderTest ~> check {
      status should ===(StatusCodes.SeeOther)
    }

    (mockProducts.getProductById _)
      .expects("0")
      .returns(Future(Some(p)))
      .anyNumberOfTimes()
    (mockProducts.buyProduct _)
      .expects(p.id, 1)
      .returns(Some(1))
      .once()
    (mockOrders.createOrder _ )
      .expects(p.id, u.userId, p.seller, new Integer(1), Requested())
      .returns(Future())
      .once()

    val res =
      requestCookies(Post("/api/buy_basket"), cookies) ~> routesUnderTest
    res ~> checkSetCookies(cookies, StatusCodes.SeeOther)

    res ~> check {
      headers(3).name should ===("Location")
      headers(3).value should ===("/basket")

      basketFromCookie(cookies).size should ===(0)
    }

    (mockProducts.buyProduct _).expects(p.id, 1).returns(None).once()

    val res2 =
      requestCookies(Post("/api/buy_basket"), cookies2) ~> routesUnderTest
    res2 ~> checkSetCookies(cookies2, StatusCodes.SeeOther)
    res2 ~> check {
      headers(3).name should ===("Location")
      headers(3).value should ===("/basket")

      basketFromCookie(cookies2).size should ===(1)
    }
  }

  test("Route POST /api/do_logout tests") {
    var mockUsers       = mock[Users]
    var mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    val u       = User("test", "test", "test", "test@test.com", isSeller = false)
    var cookies = createSession(u, mockUsers, mockProducts, routesUnderTest)

    val res = requestCookies(Post("/api/do_logout"), cookies) ~> routesUnderTest
    res ~> checkSetCookies(cookies, StatusCodes.SeeOther)
    res ~> check {
      headers(3).name should ===("Location")
      headers(3).value should ===("/signin")
      idFromCookie(cookies) should ===("")
    }
  }

  test("Route POST /api/update_order_status tests") {
    var mockUsers       = mock[Users]
    var mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    val u       = User("test", "test", "test", "test@test.com", isSeller = true)
    var cookies = createSession(u, mockUsers, mockProducts, routesUnderTest)

    val currentDate : Date = new Date
    val order = Order("0", "0", "test", "test", new Timestamp(currentDate.getTime()), 0, "requested")

    val form = FormData(
      ("id", "0"),
      ("status", "ongoing")
    )

    try {
      Post("/api/update_order_status", form) ~> routesUnderTest ~> check {
        status should ===(StatusCodes.SeeOther)
      }
    } catch {
      case _: Exception => ()
    }
  }

  test("Route GET /profile session tests") {
    var mockUsers       = mock[Users]
    var mockProducts    = mock[Products]
    var mockOrders      = mock[Orders]
    val routesUnderTest = new Routes(mockUsers, mockProducts, mockOrders).routes

    val u       = User("0", "test", "test", "test@test.com", isSeller = false)
    var cookies = createSession(u, mockUsers, mockProducts, routesUnderTest)

    requestCookies(
      Get("/profile?userId=1"),
      cookies
    ) ~> routesUnderTest ~> check {
      status should ===(StatusCodes.SeeOther)
    }

    (mockUsers.getUserById _).expects("0").returns(Future(Some(u))).once()

    requestCookies(
      Get("/profile?userId=0"),
      cookies
    ) ~> routesUnderTest ~> check {
      status should ===(StatusCodes.OK)
    }

    (mockUsers.getUserById _).expects("0").returns(Future(None)).once()

    requestCookies(
      Get("/profile?userId=0"),
      cookies
    ) ~> routesUnderTest ~> check {
      status should ===(StatusCodes.NotFound)
    }

  }
}
