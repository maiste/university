/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

import scala.util.{Success, Failure}
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta._
import org.scalatest.{Matchers, BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.funsuite.AnyFunSuite
import com.typesafe.scalalogging.LazyLogging
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import ch.qos.logback.classic.{Level, Logger}
import org.slf4j.LoggerFactory
import poca.{
  MyDatabase,
  Users,
  User,
  UserAlreadyExistsException,
  Routes,
  RunMigrations,
  Products,
  Product,
  Seller,
  Order,
  Orders,
  Status,
  Requested
}

class DatabaseTest
    extends AnyFunSuite
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with LazyLogging {
  val rootLogger: Logger = LoggerFactory.getLogger("com").asInstanceOf[Logger]
  rootLogger.setLevel(Level.INFO)
  val slickLogger: Logger =
    LoggerFactory.getLogger("slick").asInstanceOf[Logger]
  slickLogger.setLevel(Level.INFO)

  // In principle, mutable objets should not be shared between tests, because tests should be independent from each other. However for performance the connection to the database should not be recreated for each test. Here we prefer to share the database.
  override def beforeAll() {
    val isRunningOnCI = sys.env.getOrElse("CI", "") != ""
    val configName    = if (isRunningOnCI) "myTestDBforCI" else "myTestDB"
    val config        = ConfigFactory.load().getConfig(configName)
    MyDatabase.initialize(config)
  }
  override def afterAll() {
    MyDatabase.db.close
  }

  override def beforeEach() {
    val resetSchema              = sqlu"drop schema public cascade; create schema public;"
    val resetFuture: Future[Int] = MyDatabase.db.run(resetSchema)
    Await.result(resetFuture, Duration.Inf)
    new RunMigrations(MyDatabase.db)()
  }

// ----------------  USER ------------------------

  test("Users.createUser should create a new user") {
    val users: Users = new Users()

    val createUserFuture: Future[Unit] =
      users.createUser("toto", "password", "test@test.fr")
    Await.ready(createUserFuture, Duration.Inf)

    // Check that the future succeeds
    createUserFuture.value should be(Some(Success(())))

    val getUsersFuture: Future[Seq[User]] = users.getAllUsers()
    var allUsers: Seq[User]               = Await.result(getUsersFuture, Duration.Inf)

    allUsers.length should be(1)
    allUsers.head.username should be("toto")
  }

  test(
    "Users.createUser returned future should fail if the user already exists"
  ) {
    val users: Users = new Users()

    val createUserFuture: Future[Unit] =
      users.createUser("toto", "password", "test@test.fr")
    Await.ready(createUserFuture, Duration.Inf)

    val createDuplicateUserFuture: Future[Unit] =
      users.createUser("toto", "password", "test@test.fr")
    Await.ready(createDuplicateUserFuture, Duration.Inf)

    createDuplicateUserFuture.value match {
      case Some(Failure(exc: UserAlreadyExistsException)) => {
        exc.getMessage should equal(
          "A user with username 'toto' already exists."
        )
      }
      case _ => fail("The future should fail.")
    }
  }

  test("Users.getUserByUsername should return no user if it does not exist") {
    val users: Users = new Users()

    val createUserFuture: Future[Unit] =
      users.createUser("toto", "password", "test@test.fr")
    Await.ready(createUserFuture, Duration.Inf)

    val returnedUserFuture: Future[Option[User]] =
      users.getUserByUsername("somebody-else")
    val returnedUser: Option[User] =
      Await.result(returnedUserFuture, Duration.Inf)

    returnedUser should be(None)
  }

  test("Users.createUser should fail with bad email") {
    try {
      val users: Users = new Users()

      val createUserFuture: Future[Unit] =
        users.createUser("toto", "password", "testgsdgsdgdsstr")
      Await.ready(createUserFuture, Duration.Inf)

      fail("Should have failed")
    }
    catch {
      case _ : Exception => ()
    }
  }

 test("Users.updatePassword on unknown user should fail") {
    try {
      val users: Users = new Users()

       val test: Future[Unit] = users.updatePassword("none", "new_password")
      Await.result(test, Duration.Inf)

      // No exception was raised, but the user doesn't exist.
      fail("Should have failed")

    } catch {
      // The program must raise an exception if the user is not found.
      case _ : Exception => ()
    }
 }

  test("Users.getUserById should return None") {
    val users: Users = new Users()

    val createUserFuture: Future[Unit] =
      users.createUser("toto", "password", "test@test.fr")
    Await.ready(createUserFuture, Duration.Inf)

    val returnedUserFuture: Future[Option[User]] =
      users.getUserById("test")
    val returnedUser: Option[User] =
      Await.result(returnedUserFuture, Duration.Inf)

    returnedUser should be(None)
  }
 
  test("Users.updatePassword should change the password") {
    val users: Users = new Users()

    val createUserFuture: Future[Unit] =
      users.createUser("toto", "password", "test@test.fr")
    Await.ready(createUserFuture, Duration.Inf)

    val returnedUserFutureBefore: Future[Option[User]] =
      users.getUserByUsername("toto")
    val returnedUserBefore: Option[User] =
      Await.result(returnedUserFutureBefore, Duration.Inf)

    val previous_password = returnedUserBefore match {
      case Some (user) => user.password
      case None => ""
    }

    val test: Future[Unit] = users.updatePassword("toto", "new_password")
    Await.result(test, Duration.Inf)

    val returnedUserFuture: Future[Option[User]] =
      users.getUserByUsername("toto")
    val returnedUser: Option[User] =
      Await.result(returnedUserFuture, Duration.Inf)

    returnedUser match {
      case Some(user) => user.password should !==(previous_password)
      case None => fail("Should return a user.")
    }
  }

  test("Users.updateMail should change the email") {
    val users: Users = new Users()

    val createUserFuture: Future[Unit] =
      users.createUser("toto", "password", "test@test.fr")
    Await.ready(createUserFuture, Duration.Inf)

    val returnedUserFutureBefore: Future[Option[User]] =
      users.getUserByUsername("toto")
    val returnedUserBefore: Option[User] =
      Await.result(returnedUserFutureBefore, Duration.Inf)

    val test: Future[Unit] = users.updateMail("toto", "new_mail@test.fr")
    Await.result(test, Duration.Inf)

    val returnedUserFuture: Future[Option[User]] =
      users.getUserByUsername("toto")
    val returnedUser: Option[User] =
      Await.result(returnedUserFuture, Duration.Inf)

    returnedUser match {
      case Some(user) => user.email should ===("new_mail@test.fr")
      case None => fail("Should return a user.")
    }

    try {
      val test: Future[Unit] = users.updateMail("toto", "gfhdsghfsjhgfs")
      Await.result(test, Duration.Inf)

      fail("Should have failed")
    } catch {
      case _ : Exception => ()
    }

    try {
      val test: Future[Unit] = users.updateMail("tgdfgsdoto", "test@mail.fr")
      Await.result(test, Duration.Inf)

      fail("Should have failed")
    } catch {
      case _ : Exception => ()
    }
  }

  test("Users.checkUserPassword") {
    val users: Users = new Users()

    val createUserFuture: Future[Unit] =
      users.createUser("toto", "password", "test@test.fr")
    Await.ready(createUserFuture, Duration.Inf)

    val returnedUserFuture: Future[Option[User]] =
      users.getUserByUsername("toto")
    val returnedUser: Option[User] =
      Await.result(returnedUserFuture, Duration.Inf)

    returnedUser match {
      case Some(user) => {
        users.checkUserPassword(user, "password") should be(true)
        users.checkUserPassword(user, "badpassword") should be(false)
      }
      case None => fail("Should be Some")
    }
  }

  test("Users.getUserByUsername should return a user") {
    val users: Users = new Users()

    val createUserFuture: Future[Unit] =
      users.createUser("toto", "password", "test@test.fr")
    Await.ready(createUserFuture, Duration.Inf)

    val returnedUserFuture: Future[Option[User]] =
      users.getUserByUsername("toto")
    val returnedUser: Option[User] =
      Await.result(returnedUserFuture, Duration.Inf)

    returnedUser match {
      case Some(user) => user.username should be("toto")
      case None       => fail("Should return a user.")
    }
  }

  test("Users.getAllUsers should return a list of users") {
    val users: Users = new Users()

    val createUserFuture: Future[Unit] =
      users.createUser("riri", "password2", "test@test.fr")
    Await.ready(createUserFuture, Duration.Inf)

    val createAnotherUserFuture: Future[Unit] =
      users.createUser("fifi", "password2", "test@test.fr")
    Await.ready(createAnotherUserFuture, Duration.Inf)

    val returnedUserSeqFuture: Future[Seq[User]] = users.getAllUsers()
    val returnedUserSeq: Seq[User] =
      Await.result(returnedUserSeqFuture, Duration.Inf)

    returnedUserSeq.length should be(2)
  }

//--------------  PRODUCT -------------------------

  test("Products.createProduct should create a new product") {
    val products: Products = new Products()

    val getProducts: Future[Seq[Product]] = products.getAllProducts()
    var lenProducts: Seq[Product]         = Await.result(getProducts, Duration.Inf)

    val createProductFuture: Future[Unit] =
      products.createProduct("1", "toto", "the best", List("link.jpg"))
    Await.ready(createProductFuture, Duration.Inf)

    // Check that the future succeeds
    createProductFuture.value should be(Some(Success(())))

    val getProductsFuture: Future[Seq[Product]] = products.getAllProducts()
    var allProducts: Seq[Product] =
      Await.result(getProductsFuture, Duration.Inf)

    val res = lenProducts.length + 1
    allProducts.length should be(res)
    allProducts.last.name should be("toto")
  }

  test("Products.getProductById should return no product if it does not exist") {
    val products: Products = new Products()

    val createProductFuture: Future[Unit] =
      products.createProduct("1", "toto", "the best", List("link.jpg"))
    Await.ready(createProductFuture, Duration.Inf)

    val returnedProductFuture: Future[Option[Product]] =
      products.getProductById("2")
    val returnedProduct: Option[Product] =
      Await.result(returnedProductFuture, Duration.Inf)

    returnedProduct should be(None)
  }

  test("Products.getProductById should return a product") {
    val products: Products = new Products()

    val createProductFuture: Future[Unit] =
      products.createProduct("1", "toto", "the best", List("link.jpg"))
    Await.ready(createProductFuture, Duration.Inf)

    val returnedProductFuture: Future[Option[Product]] =
      products.getProductById("1")
    val returnedProduct: Option[Product] =
      Await.result(returnedProductFuture, Duration.Inf)

    returnedProduct match {
      case Some(product) => product.name should be("toto")
      case None          => fail("Should return a product.")
    }
  }

  test("Products.buyProduct test if the quantity reduces on buyProduct") {
    val products: Products = new Products()

    val createProductFuture: Future[Unit] =
       products.createProduct("1", "toto", "the best", List("link.jpg"))
    Await.ready(createProductFuture, Duration.Inf)

    val test : Option[Int] = products.buyProduct("1", 5)

    val returnedProductFuture: Future[Option[Product]] =
      products.getProductById("1")
    val returnedProduct: Option[Product] =
      Await.result(returnedProductFuture, Duration.Inf)

    returnedProduct match {
      case Some(product) => product.quantity should be(0)
      case None => fail("Should return a product")
    }
  }

  test("Products.updateProductQuantity tests") {
    val products: Products = new Products()

    val createProductFuture: Future[Unit] =
       products.createProduct("1", "toto", "the best", List("link.jpg"))
    Await.ready(createProductFuture, Duration.Inf)

    val test = products.updateProductQuantity("1", 5)

    val returnedProductFuture: Future[Option[Product]] =
      products.getProductById("1")
    val returnedProduct: Option[Product] =
      Await.result(returnedProductFuture, Duration.Inf)

    returnedProduct match {
      case Some(product) => product.quantity should be(5)
      case None => fail("Should return a product")
    }
  }


// ----------------- SELLER ------------

  test("Seller.getProductsFromSeller should return a list of products") {
    val products: Products = new Products()

    val returnedProduct: Future[Seq[Product]] = products.getAllProducts()
    val lenProductSeq: Seq[Product] =
      Await.result(returnedProduct, Duration.Inf)

    val createProductFuture: Future[Unit] = products.createProduct(
      "1",
      "toto",
      "the best",
      List("link.jpg"),
      10,
      15,
      "alibaba"
    )
    Await.ready(createProductFuture, Duration.Inf)

    val createAnotherProductFuture: Future[Unit] = products.createProduct(
      "2",
      "titi",
      "the other",
      List("link.jpg"),
      10,
      15,
      "alibaba"
    )
    Await.ready(createAnotherProductFuture, Duration.Inf)

    val returnedProductSeqFuture: Future[Seq[Product]] =
      products.filterProductsBySeller("alibaba")
    val returnedProductSeq: Seq[Product] =
      Await.result(returnedProductSeqFuture, Duration.Inf)

    val res = 2
    returnedProductSeq.length should be(res)
  }


// ----------------- ORDER ------------

  test("Orders.createOrder should create a new order") {
    val orders: Orders = new Orders()

    val createOrderFuture: Future[Unit] =
      orders.createOrder("0", "0", "1", 10, Requested())
    Await.ready(createOrderFuture, Duration.Inf)

    // Check that the future succeeds
    createOrderFuture.value should be(Some(Success(())))

    val getOrderFuture: Future[Seq[Order]] = orders.getAllOrders()
    var allOrders: Seq[Order]               = Await.result(getOrderFuture, Duration.Inf)

    allOrders.length should be(1)
    allOrders.head.productId should be("0")
  }

  test("Orders.getAllOrdersForUserId should return an order") {
    val orders: Orders = new Orders()

    val createOrderFuture: Future[Unit] =
      orders.createOrder("0", "0", "1", 10, Requested())
    Await.ready(createOrderFuture, Duration.Inf)

    // Check that the future succeeds
    createOrderFuture.value should be(Some(Success(())))

    val getOrderFuture: Future[Seq[Order]] = orders.getAllOrdersForUserId("0")
    var allOrders: Seq[Order]               = Await.result(getOrderFuture, Duration.Inf)

    allOrders.length should be(1)
    allOrders.head.productId should be("0")
  }

  test("Orders.getAllOrdersForSellerId should return an order") {
    val orders: Orders = new Orders()

    val createOrderFuture: Future[Unit] =
      orders.createOrder("0", "0", "1", 10, Requested())
    Await.ready(createOrderFuture, Duration.Inf)

    // Check that the future succeeds
    createOrderFuture.value should be(Some(Success(())))

    val getOrderFuture: Future[Seq[Order]] = orders.getAllOrdersForSellerId("1")
    var allOrders: Seq[Order]               = Await.result(getOrderFuture, Duration.Inf)

    allOrders.length should be(1)
    allOrders.head.productId should be("0")
  }

  test("Orders.getOrderById tests") {
    val orders: Orders = new Orders()

    val createOrderFuture: Future[Unit] =
      orders.createOrder("0", "0", "1", 10, Requested())
    Await.ready(createOrderFuture, Duration.Inf)

    // Check that the future succeeds
    createOrderFuture.value should be(Some(Success(())))

    val getOrderFuture : Future[Option[Order]] = orders.getOrderById("0")
    val order : Option[Order] = Await.result(getOrderFuture, Duration.Inf)

    order match {
      case Some (order) => order.orderId should be("0")
      case None => ()
    }
  }

  test("Orders.updateOrderStatus(id, on going) should set status to on going") {
    val orders: Orders = new Orders()

    val createOrderFuture: Future[Unit] =
      orders.createOrderTest("0", "0", "0", "1", 10, Requested())
    Await.ready(createOrderFuture, Duration.Inf)

    // Check that the future succeeds
    createOrderFuture.value should be(Some(Success(())))

    val test = orders.updateOrderStatusS("0", "ongoing")

    val getOrderFuture : Future[Option[Order]] = orders.getOrderById("0")
    val order : Option[Order] = Await.result(getOrderFuture, Duration.Inf)

    order match {
      case Some (order) => order.status should be("ongoing")
      case None => fail("Should return an order")
    }
  }

  test("Orders.updateOrderStatus(id, bad status) should not update") {
    val orders: Orders = new Orders()

    val createOrderFuture: Future[Unit] =
      orders.createOrderTest("0", "0", "0", "1", 10, Requested())
    Await.ready(createOrderFuture, Duration.Inf)

    // Check that the future succeeds
    createOrderFuture.value should be(Some(Success(())))

    val test = orders.updateOrderStatusS("0", "bad boy")

    val getOrderFuture : Future[Option[Order]] = orders.getOrderById("0")
    val order : Option[Order] = Await.result(getOrderFuture, Duration.Inf)

    order match {
      case Some (order) => order.status should be("requested")
      case None => fail("Should return an order")
    }
  }
}
