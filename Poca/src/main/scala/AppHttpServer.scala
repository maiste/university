/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package poca

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import com.typesafe.scalalogging.LazyLogging
import ch.qos.logback.classic.{Level, Logger}
import org.slf4j.LoggerFactory
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}

object AppHttpServer extends LazyLogging {
  val rootLogger: Logger = LoggerFactory.getLogger("com").asInstanceOf[Logger]
  rootLogger.setLevel(Level.INFO)
  val slickLogger: Logger =
    LoggerFactory.getLogger("slick").asInstanceOf[Logger]
  slickLogger.setLevel(Level.INFO)

  def initDatabase() = {
    val isRunningOnCloud = sys.env.getOrElse("DB_HOST", "") != ""
    var rootConfig       = ConfigFactory.load()
    val dbConfig = if (isRunningOnCloud) {
      val dbHost     = sys.env.getOrElse("DB_HOST", "")
      val dbPassword = sys.env.getOrElse("DB_PASSWORD", "")

      val originalConfig = rootConfig.getConfig("cloudDB")
      originalConfig
        .withValue(
          "properties.serverName",
          ConfigValueFactory.fromAnyRef(dbHost)
        )
        .withValue(
          "properties.password",
          ConfigValueFactory.fromAnyRef(dbPassword)
        )
    } else {
      rootConfig.getConfig("localDB")
    }
    MyDatabase.initialize(dbConfig)
  }

  def main(args: Array[String]): Unit = {
    implicit val actorsSystem =
      ActorSystem(guardianBehavior = Behaviors.empty, name = "my-system")
    implicit val actorsExecutionContext = actorsSystem.executionContext

    initDatabase
    val db = MyDatabase.db
    new RunMigrations(db)()

    var users    = new Users()
    val products = new Products()
    val orders   = new Orders()
    val routes   = new Routes(users, products, orders)

    val bindingFuture = Http().newServerAt("0.0.0.0", 8080).bind(routes.routes)

    val serverStartedFuture = bindingFuture.map(binding => {
      val address = binding.localAddress
      logger.info(
        s"Server online at http://${address.getHostString}:${address.getPort}/"
      )
    })

    val waitOnFuture = serverStartedFuture.flatMap(unit => Future.never)

    scala.sys.addShutdownHook {
      actorsSystem.terminate
      db.close
    }

    Await.ready(waitOnFuture, Duration.Inf)
  }
}
