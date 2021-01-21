/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package poca

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.PostgresProfile.api._

class Migration07MockSeller(db: Database) extends Migration with LazyLogging {
  override def apply() {
    val request =
      sqlu"""UPDATE products SET seller = '66b2b19b-1005-46af-a849-3d184888762e' WHERE "productName"='Apricots' """
    val setCol: Future[Int] = db.run(request)
    Await.result(setCol, Duration.Inf)
    logger.info("Done setting seller for apricots")
  }
}
