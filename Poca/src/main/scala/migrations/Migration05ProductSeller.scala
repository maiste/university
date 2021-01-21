/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package poca

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.PostgresProfile.api._

class Migration05ProductSeller(db: Database)
    extends Migration
    with LazyLogging {

  override def apply() {

    // Add a new table
    val request             = sqlu"""ALTER TABLE products ADD COLUMN seller VARCHAR(256)"""
    val addCol: Future[Int] = db.run(request)
    Await.result(addCol, Duration.Inf)
    logger.info("Done adding new column seller in products")

    val request2            = sqlu"UPDATE products SET seller = '0'"
    val setCol: Future[Int] = db.run(request2)
    Await.result(setCol, Duration.Inf)
    logger.info("Done setting seller")

  }
}
