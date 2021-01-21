/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package poca

import scala.concurrent.Future
import scala.language.implicitConversions
import slick.jdbc.PostgresProfile.api._
import java.util.UUID
import java.util.Date
import java.sql.Timestamp
import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

final case class StatusConvertorException(
    private val message: String = "",
    private val cause: Throwable = None.orNull
) extends Exception(message, cause)

object StatusConvertor {
  implicit def status2String(s: Status): String =
    s match {
      case Requested() => "requested"
      case Ongoing()   => "ongoing"
      case Delivered() => "delivered"
      case Cancelled() => "cancelled"
      case _           => throw new StatusConvertorException()
    }

  implicit def string2Status(s: String): Status =
    s match {
      case "requested" => Requested()
      case "ongoing"   => Ongoing()
      case "delivered" => Delivered()
      case "cancelled" => Cancelled()
      case _           => throw new StatusConvertorException()
    }
}

abstract class Status
final case class Requested() extends Status
final case class Ongoing()   extends Status
final case class Delivered() extends Status
final case class Cancelled() extends Status

case class Order(
    orderId: String,
    productId: String,
    userId: String,
    sellerId: String,
    date: Timestamp,
    quantity: Int,
    status: String
) {
  def toOrderSeller(u: Users): Option[OrderSeller] = {
    val maybeUser = Await.result(
      u.getUserById(userId),
      Duration(10, TimeUnit.SECONDS)
    )
    maybeUser match {
      case Some(user) =>
        Some(
          OrderSeller(
            orderId,
            productId,
            user,
            sellerId,
            date,
            quantity,
            status
          )
        )
      case None => None
    }
  }

  def toOrderUser(u: Users): Option[OrderUser] = {
    val maybeUser = Await.result(
      u.getUserById(sellerId),
      Duration(10, TimeUnit.SECONDS)
    )
    maybeUser match {
      case Some(seller) =>
        Some(
          OrderUser(orderId, productId, userId, seller, date, quantity, status)
        )
      case None => None
    }
  }

  def toDefaultOrderSeller = {
    OrderSeller(
      orderId,
      productId,
      User(userId, "", "", "", true),
      sellerId,
      date,
      quantity,
      status
    )
  }
  def toDefaultOrderUser = {
    OrderUser(
      orderId,
      productId,
      userId,
      User(sellerId, "", "", "", true),
      date,
      quantity,
      status
    )
  }

  implicit def fromOrderSeller(o: OrderSeller) = {
    Order(
      o.orderId,
      o.productId,
      o.user.userId,
      o.sellerId,
      o.date,
      o.quantity,
      o.status
    )
  }
  implicit def fromOrderUser(o: OrderUser) = {
    Order(
      o.orderId,
      o.productId,
      o.userId,
      o.seller.userId,
      o.date,
      o.quantity,
      o.status
    )
  }
}

case class OrderSeller(
    orderId: String,
    productId: String,
    user: User,
    sellerId: String,
    date: Timestamp,
    quantity: Int,
    status: String
)

case class OrderUser(
    orderId: String,
    productId: String,
    userId: String,
    seller: User,
    date: Timestamp,
    quantity: Int,
    status: String
)

class Orders {
  class OrdersTable(tag: Tag)
      extends Table[(String, String, String, String, Timestamp, Int, String)](
        tag,
        "orders"
      ) {
    def orderId   = column[String]("order_id", O.PrimaryKey)
    def productId = column[String]("product_id")
    def userId    = column[String]("user_id")
    def sellerId  = column[String]("seller_id")
    def date      = column[Timestamp]("date")
    def quantity  = column[Int]("quantity")
    def status    = column[String]("status")
    def *         = (orderId, productId, userId, sellerId, date, quantity, status)
  }

  implicit val executionContext =
    scala.concurrent.ExecutionContext.Implicits.global
  val db     = MyDatabase.db
  val orders = TableQuery[OrdersTable]

  /**
    * Create and add a new order to database
    */
  def createOrder(
      productId: String,
      userId: String,
      sellerId: String,
      quantity: Integer,
      status: Status
  ): Future[Unit] = {
    val currentDate: Date = new Date
    val newOrder = Order(
      orderId = UUID.randomUUID.toString(),
      productId = productId,
      userId = userId,
      sellerId = sellerId,
      date = new Timestamp(currentDate.getTime()),
      quantity = quantity,
      status = StatusConvertor.status2String(status)
    )
    val newOrderAsTuple
        : (String, String, String, String, Timestamp, Int, String) =
      Order.unapply(newOrder).get
    val addOrder: DBIO[Int]      = (orders += newOrderAsTuple)
    val queryFuture: Future[Int] = db.run(addOrder)
    queryFuture.map(_ => ())
  }

  /**
    * MEANT FOR TESTING
    * Create and add a new order to database
    */
  def createOrderTest(
      orderId: String,
      productId: String,
      userId: String,
      sellerId: String,
      quantity: Integer,
      status: Status
  ): Future[Unit] = {
    val currentDate: Date = new Date
    val newOrder = Order(
      orderId = orderId,
      productId = productId,
      userId = userId,
      sellerId = sellerId,
      date = new Timestamp(currentDate.getTime()),
      quantity = quantity,
      status = StatusConvertor.status2String(status)
    )
    val newOrderAsTuple
        : (String, String, String, String, Timestamp, Int, String) =
      Order.unapply(newOrder).get
    val addOrder: DBIO[Int]      = (orders += newOrderAsTuple)
    val queryFuture: Future[Int] = db.run(addOrder)
    queryFuture.map(_ => ())
  }

  /**
    * Find an order thanks to its id
    */
  def getOrderById(id: String): Future[Option[Order]] = {
    val query        = orders.filter(_.orderId === id)
    val ordersFuture = db.run(query.result)
    ordersFuture.map(
      (ordersLst: Seq[
        (String, String, String, String, Timestamp, Int, String)
      ]) => {
        ordersLst.length match {
          case 0 => None
          case 1 => Some(Order tupled ordersLst.head)
          case _ =>
            throw new InconsistentStateException(
              s"OrderId $id is linked to several orders in database!"
            )
        }
      }
    )
  }

  /**
    * Returns all orders in the database
    */
  def getAllOrders(): Future[Seq[Order]] = {
    val orderFuture = db.run(orders.result)
    orderFuture.map(
      (orderLst: Seq[
        (String, String, String, String, Timestamp, Int, String)
      ]) => {
        orderLst.map(Order tupled _)
      }
    )
  }

  /**
    * Return all orders in the database for one user
    */
  def getAllOrdersForUserId(userId: String): Future[Seq[Order]] = {
    val query        = orders.filter(_.userId === userId)
    val ordersFuture = db.run(query.result)
    ordersFuture.map(
      (orderLst: Seq[
        (String, String, String, String, Timestamp, Int, String)
      ]) => {
        orderLst.map(Order tupled _)
      }
    )
  }

  /**
    * Return all orders in the database for one seller
    */
  def getAllOrdersForSellerId(sellerId: String): Future[Seq[Order]] = {
    val query        = orders.filter(_.sellerId === sellerId)
    val ordersFuture = db.run(query.result)
    ordersFuture.map(
      (orderLst: Seq[
        (String, String, String, String, Timestamp, Int, String)
      ]) => {
        orderLst.map(Order tupled _)
      }
    )
  }

  /**
    * Update order status
    */
  def updateOrderStatus(id: String, status: Status) {
    val request =
      orders
        .filter(_.orderId === id)
        .map(p => (p.status))
        .update(StatusConvertor.status2String(status))

    val requestFuture = db.run(request)
  }

  /**
    * Update order status with status = string
    */
  def updateOrderStatusS(id: String, status: String) {
    try {
      updateOrderStatus(id, StatusConvertor.string2Status(status))
    } catch {
      case _: StatusConvertorException => ()
    }
  }

}
