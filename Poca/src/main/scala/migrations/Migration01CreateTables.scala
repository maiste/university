/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package poca

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta.MTable

class Migration01CreateTables(db: Database) extends Migration with LazyLogging {

  class CurrentUsersTable(tag: Tag)
      extends Table[(String, String, String, String)](tag, "users") {
    def userId   = column[String]("userId", O.PrimaryKey)
    def username = column[String]("username")
    def password = column[String]("password")
    def email    = column[String]("email")
    def *        = (userId, username, password, email)
  }

  class CurrentProductsTable(tag: Tag)
      extends Table[(String, String, String, String)](tag, "products") {
    def productId          = column[String]("productId", O.PrimaryKey)
    def productName        = column[String]("productName")
    def productDescription = column[String]("productDescription")
    def productImages      = column[String]("productImages")
    def *                  = (productId, productName, productDescription, productImages)
  }

  override def apply(): Unit = {
    implicit val executionContext =
      scala.concurrent.ExecutionContext.Implicits.global
    val users    = TableQuery[CurrentUsersTable]
    val products = TableQuery[CurrentProductsTable]
    val creationFuture: Future[Unit] = db.run(
      DBIO.seq(
        users.schema.createIfNotExists,
        products.schema.createIfNotExists
      )
    )

    Await.result(creationFuture, Duration.Inf)
    logger.info("Done creating table: Users, Products")
  }
}
