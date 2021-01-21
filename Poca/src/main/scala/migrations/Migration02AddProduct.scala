/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package poca

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.PostgresProfile.api._

class Migration02AddProduct(db: Database) extends Migration with LazyLogging {

  class CurrentProductsTable(tag: Tag)
      extends Table[(String, String, String, String)](tag, "products") {
    def productId          = column[String]("productId", O.PrimaryKey)
    def productName        = column[String]("productName")
    def productDescription = column[String]("productDescription")
    def productImages      = column[String]("productImages")
    def *                  = (productId, productName, productDescription, productImages)
  }

  override def apply() {
    val products = TableQuery[CurrentProductsTable]
    val setup: Future[Unit] = db.run(
      DBIO.seq(
        // Insert some products
        products ++= Seq(
          (
            "11",
            "Apricots",
            "Orange fruits, do not taste like oranges.",
            "https://cdn.pixabay.com/photo/2016/07/11/12/52/apricots-1509634_960_720.jpg"
          ),
          (
            "12",
            "Superior Coffee",
            "Coffee but tastier and more expensive than standard coffee.",
            "https://cdn.pixabay.com/photo/2013/08/11/19/46/coffee-171653_960_720.jpg"
          ),
          (
            "13",
            "Unicorn",
            "A horse with a horn.",
            "https://cdn.pixabay.com/photo/2018/11/19/20/05/unicorn-3825978_960_720.jpg"
          ),
          (
            "14",
            "Shelf",
            "You can put stuff on this.",
            "https://cdn.pixabay.com/photo/2016/02/19/11/15/american-1209605_960_720.jpg"
          ),
          (
            "15",
            "Sweater",
            "Your grandma will not be concerned with you being cold if you wear this sweater.",
            "https://cdn.pixabay.com/photo/2018/10/01/18/07/human-3716933_960_720.jpg"
          ),
          (
            "16",
            "Boat",
            "Prevents drowning",
            "https://cdn.pixabay.com/photo/2015/11/28/17/50/origami-1067673_960_720.jpg"
          ),
          (
            "17",
            "Banana",
            "Yellow fruit, will bring happiness to anyone.",
            "https://cdn.pixabay.com/photo/2016/10/13/14/03/delfin-bananas-1737840_960_720.jpg"
          ),
          (
            "18",
            "Chair",
            "Allows you to seat on it.",
            "https://cdn.pixabay.com/photo/2015/09/18/11/36/chair-945412_960_720.jpg"
          ),
          (
            "19",
            "Plane",
            "Pretend you are superior to others with this splendid paper plane.",
            "https://cdn.pixabay.com/photo/2020/07/10/16/19/paper-5391196_960_720.jpg"
          ),
          (
            "20",
            "Kermit",
            "Will become your best friend, do not prevent alcoholism.",
            "https://cdn.pixabay.com/photo/2016/09/07/10/37/kermit-1651325_960_720.jpg"
          ),
          (
            "21",
            "Teapot",
            "Splendid teapot, will keep you warm during the winter.",
            "https://cdn.pixabay.com/photo/2019/03/16/09/11/small-4058702_960_720.jpg"
          ),
          (
            "22",
            "Cactus",
            "Lets you believe you know how to take care of plants.",
            "https://cdn.pixabay.com/photo/2015/11/26/00/01/cactus-1063094_960_720.jpg"
          ),
          (
            "23",
            "Padlock",
            "Seals your love with Cynthia with this lock, even if you know she is having an affair with your brother.",
            "https://cdn.pixabay.com/photo/2016/07/14/07/29/lock-1516241_960_720.jpg"
          ),
          (
            "24",
            "Bulb",
            "Produces light.",
            "https://cdn.pixabay.com/photo/2016/07/14/07/29/lock-1516241_960_720.jpg"
          ),
          (
            "25",
            "Socks",
            "Keeps your feet warm and cozy",
            "https://cdn.pixabay.com/photo/2015/09/09/18/25/feet-932346_960_720.jpg"
          ),
          (
            "26",
            "WheelBarrow",
            "Beautiful wheelbarrow, 10/10, you should buy it.",
            "https://cdn.pixabay.com/photo/2014/05/21/16/00/wheelbarrow-349962_960_720.jpg"
          ),
          (
            "27",
            "Digger",
            "Cute digger, really easy to use, you can bury a lot of stuff with it.",
            "https://cdn.pixabay.com/photo/2019/07/12/12/33/loader-4332778_960_720.jpg"
          ),
          (
            "28",
            "Vintage balance",
            "Allows you to weight stuff but it takes more times than a modern one. Coins aren't send with the product.",
            "https://cdn.pixabay.com/photo/2014/08/21/16/06/justice-423446_960_720.jpg"
          ),
          (
            "29",
            "Toilet paper",
            "We hope you know why you should use it.",
            "https://cdn.pixabay.com/photo/2015/11/10/20/16/frog-1037714_960_720.jpg"
          ),
          (
            "30",
            "Paper bag",
            "Ugly paper bag, but it is ecological.",
            "https://cdn.pixabay.com/photo/2019/03/30/18/30/bag-4091711_960_720.jpg"
          )
        )
      )
    )

    Await.result(setup, Duration.Inf)
    logger.info("Done populating table: Products")
  }
}
