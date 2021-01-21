/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package poca

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.PostgresProfile.api._

class Migration06Seller(db: Database) extends Migration with LazyLogging {

  override def apply() {

    // Add a new table
    val request = sqlu"""ALTER TABLE users ADD COLUMN is_seller BOOLEAN"""
    val addCol: Future[Int] = db.run(request)
    Await.result(addCol, Duration.Inf)
    logger.info("Done adding new column is_seller in users")

    val request2 = sqlu"UPDATE users SET is_seller = TRUE"
    val setCol: Future[Int] = db.run(request2)
    Await.result(setCol, Duration.Inf)
    logger.info("Done setting is_seller")

  }
}
