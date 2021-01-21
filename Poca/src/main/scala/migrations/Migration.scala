/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package poca

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.PostgresProfile.api._
import org.postgresql.util.PSQLException

trait Migration {
  def apply(): Unit
}

class RunMigrations(db: Database) extends LazyLogging {
  implicit val executionContext =
    scala.concurrent.ExecutionContext.Implicits.global
  val migrationList: List[Migration] = List(
    new Migration00AddVersionNumber(db),
    new Migration01CreateTables(db),
    new Migration02AddProduct(db),
    new Migration03UpdateProduct(db),
    new Migration04ProductsStock(db),
    new Migration05ProductSeller(db),
    new Migration06Seller(db),
    new Migration07MockSeller(db),
    new Migration08Order(db),
  )

  def getCurrentDatabaseVersion(): Int = {
    val getVersionRequest: DBIO[Seq[Int]] =
      sql"select * from database_version;".as[Int]
    val responseFuture: Future[Seq[Int]] = db.run(getVersionRequest)

    val versionFuture =
      responseFuture.map(versionSeq => versionSeq(0)).recover {
        case exc: PSQLException => {
          if (
            exc.toString
              .contains("ERROR: relation \"database_version\" does not exist")
          ) {
            0
          } else {
            throw exc
          }
        }
      }

    val version = Await.result(versionFuture, Duration.Inf)
    logger.info(s"Database version is $version")
    version
  }

  def incrementDatabaseVersion(): Unit = {
    val oldVersion = getCurrentDatabaseVersion()
    val newVersion = oldVersion + 1

    val updateVersionRequest: DBIO[Int] =
      sqlu"update database_version set number = ${newVersion};"

    val updateVersionFuture: Future[Int] = db.run(updateVersionRequest)

    Await.result(updateVersionFuture, Duration.Inf)
    logger.info(s"Database version incremented from $oldVersion to $newVersion")
  }

  def apply() {
    val version = getCurrentDatabaseVersion();
    migrationList
      .slice(version, migrationList.length)
      .foreach(migration => {
        migration()
        incrementDatabaseVersion()
      })
  }
}
