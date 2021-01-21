/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

/*
    Initialization code for the database, although simple, is extracted to its own file in order to avoid mixing up the execution context used for the database with the other execution contexts of the application.

    The access to the database is made available as a singleton.
 */

package poca

import slick.jdbc.PostgresProfile.api._
import com.typesafe.config.Config

/*
    It's important to wrap the connection to the database in a singleton Object, because database connections are expensive.
 */
object MyDatabase {
  implicit val executionContext =
    scala.concurrent.ExecutionContext.Implicits.global
  var db: Database = null

  /*
        Run this method at the entrypoint. This is a kind of manual dependency injection to make it possible to test against a test database.
        After initialization, you can get the member `db` and use it directy.
        At the end, call `db.close`.
   */
  def initialize(config: Config): Unit = {
    db = Database.forConfig(path = "", config = config)
  }
}
