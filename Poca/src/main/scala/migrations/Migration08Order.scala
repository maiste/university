/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package poca

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.PostgresProfile.api._
import java.sql.Timestamp

class Migration08Order(db: Database) extends Migration with LazyLogging {

  class CurrentOrdersTable(tag: Tag)
    extends Table[(String, String, String, String, Timestamp, Int, String)](tag, "orders") {
        def orderId   = column[String]("order_id", O.PrimaryKey)
        def productId = column[String]("product_id")
        def userId    = column[String]("user_id")
        def sellerId  = column[String]("seller_id")
        def date      = column[Timestamp]("date")
        def quantity  = column[Int]("quantity")
        def status    = column[String]("status")
        def *         = (orderId, productId, userId, sellerId, date, quantity, status)
  }

  override def apply() : Unit = {
    implicit val executionContext =
      scala.concurrent.ExecutionContext.Implicits.global
    val orders = TableQuery[CurrentOrdersTable]
    val creationFuture : Future[Unit] = db.run(
      DBIO.seq (
        orders.schema.createIfNotExists
      )
    )
    Await.result(creationFuture, Duration.Inf)
    logger.info("Done creating table : Orders")
  }
}
