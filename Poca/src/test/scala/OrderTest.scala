import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalamock.scalatest.MockFactory
import poca.Order
import java.sql.Timestamp
import poca.Users
import poca.User
import scala.concurrent.Future
import poca.OrderSeller
import poca.OrderUser

class OrderTest extends AnyFunSuite with Matchers with MockFactory {
  implicit val executionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  test("Order to OrderSeller") {
    var mockUsers = mock[Users]
    val o = Order(
      "order",
      "product",
      "user",
      "seller",
      new Timestamp(0),
      1,
      "requested"
    )
    val u = User(o.userId, "username", "pwd", "mail", true);
    (mockUsers.getUserById _).expects(o.userId).returns(Future(Some(u))).once()
    var os = o.toOrderSeller(mockUsers)
    os shouldEqual Some(
      OrderSeller(
        o.orderId,
        o.productId,
        u,
        o.sellerId,
        o.date,
        o.quantity,
        o.status
      )
    )
    (mockUsers.getUserById _).expects(o.userId).returns(Future(None)).once()
    os = o.toOrderSeller(mockUsers)
    os shouldEqual None
  }

  test("Order to OrderUser") {
    var mockUsers = mock[Users]
    val o = Order(
      "order",
      "product",
      "user",
      "seller",
      new Timestamp(0),
      1,
      "requested"
    )
    val u = User(o.sellerId, "username", "pwd", "mail", true);
    (mockUsers.getUserById _)
      .expects(o.sellerId)
      .returns(Future(Some(u)))
      .once()
    var os = o.toOrderUser(mockUsers)
    os shouldEqual Some(
      OrderUser(
        o.orderId,
        o.productId,
        o.userId,
        u,
        o.date,
        o.quantity,
        o.status
      )
    )
    (mockUsers.getUserById _).expects(o.sellerId).returns(Future(None)).once()
    os = o.toOrderUser(mockUsers)
    os shouldEqual None
  }

  test("Order to defaults") {
    val o = Order(
      "order",
      "product",
      "user",
      "seller",
      new Timestamp(0),
      1,
      "requested"
    )
    var u  = User(o.userId, "", "", "", true)
    val os = o.toDefaultOrderSeller
    os shouldEqual
      OrderSeller(
        o.orderId,
        o.productId,
        u,
        o.sellerId,
        o.date,
        o.quantity,
        o.status
      )

    u = User(o.sellerId, "", "", "", true)
    val ou = o.toDefaultOrderUser
    ou shouldEqual
      OrderUser(
        o.orderId,
        o.productId,
        o.userId,
        u,
        o.date,
        o.quantity,
        o.status
      )
  }
}
