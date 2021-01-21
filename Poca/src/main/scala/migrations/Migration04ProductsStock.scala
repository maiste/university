/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package poca

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.PostgresProfile.api._

class Migration04ProductsStock(db: Database)
    extends Migration
    with LazyLogging {

  class CurrentProductsTable(tag: Tag)
      extends Table[(String, String, String, String)](tag, "products") {
    def productId          = column[String]("productId", O.PrimaryKey)
    def productName        = column[String]("productName")
    def productDescription = column[String]("productDescription")
    def productImages      = column[String]("productImages")
    def *                  = (productId, productName, productDescription, productImages)
  }

  override def apply() {
    val request             = sqlu"""ALTER TABLE products ADD COLUMN quantity INTEGER"""
    val addCOl: Future[Int] = db.run(request)

    Await.result(addCOl, Duration.Inf)
    logger.info("Done adding new column quantity")

    val request2              = sqlu"UPDATE products SET quantity = 20"
    val addPrice: Future[Int] = db.run(request2)

    Await.result(addPrice, Duration.Inf)
    logger.info("Done adding quantity")
  }
}
