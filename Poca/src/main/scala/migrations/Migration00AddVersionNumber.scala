/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package poca

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.PostgresProfile.api._

class Migration00AddVersionNumber(db: Database)
    extends Migration
    with LazyLogging {
  override def apply() {
    val setVersionRequest =
      sqlu"create table database_version (number int not null); insert into database_version values (0);"
    val setVersionFuture: Future[Int] = db.run(setVersionRequest)

    val result = Await.result(setVersionFuture, Duration.Inf)
    logger.info(s"Database version is set.")
  }
}
