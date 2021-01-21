/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package poca

import scala.concurrent.{Await, Future}
import scala.util.{Try, Success, Failure}
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.duration._
import scala.language.postfixOps
import java.util.UUID

case class Product(
    id: String,
    name: String,
    description: String,
    imageLinks: String,
    price: Int = 0,
    quantity: Int = 0,
    seller: String = "0"
) {

  def this() = this("", "", "", "", 0, 0, "")

  override def toString = {
    s"${id} - ${name} - ${description} - ${imageLinks} - ${price} - ${quantity}"
  }
}

class Products {
  class ProductsTable(tag: Tag)
      extends Table[(String, String, String, String, Int, Int, String)](
        tag,
        "products"
      ) {
    def productId          = column[String]("productId", O.PrimaryKey)
    def productName        = column[String]("productName")
    def productDescription = column[String]("productDescription")
    def productImages      = column[String]("productImages")
    def productPrice       = column[Int]("price")
    def productQuantity    = column[Int]("quantity")
    def productSeller      = column[String]("seller")
    def * =
      (
        productId,
        productName,
        productDescription,
        productImages,
        productPrice,
        productQuantity,
        productSeller
      )
  }

  implicit val executionContext =
    scala.concurrent.ExecutionContext.Implicits.global
  val db       = MyDatabase.db
  val products = TableQuery[ProductsTable]

  def createProduct(
      id: String,
      name: String,
      description: String,
      imageLinks: List[String],
      price: Int = 0,
      quantity: Int = 0,
      seller: String = "0"
  ): Future[Unit] = {
    val id_aux = if (id == "") {
      UUID.randomUUID.toString()
    } else {
      id
    }

    val existingProductsFuture = getProductById(id)

    existingProductsFuture.flatMap(existingProducts => {
      if (existingProducts.isEmpty) {
        val images = imageLinks.mkString(";")
        val newProduct = Product(
          id = id_aux,
          name = name,
          description = description,
          imageLinks = images,
          price = price,
          quantity = quantity,
          seller = seller
        )
        val newProductAsTuple
            : (String, String, String, String, Int, Int, String) =
          Product.unapply(newProduct).get

        val dbio: DBIO[Int]           = products += newProductAsTuple
        var resultFuture: Future[Int] = db.run(dbio)

        // We do not care about the Int value
        resultFuture.map(_ => ())
      } else {
        createProduct("", name, description, imageLinks)
      }
    })
  }

  def getProductById(id: String): Future[Option[Product]] = {
    val query = products.filter(_.productId === id)

    val productListFuture = db.run(query.result)

    productListFuture.map(
      (productList: Seq[(String, String, String, String, Int, Int, String)]) =>
        {
          productList.length match {
            case 0 => None
            case 1 => Some(Product tupled productList.head)
            case _ =>
              throw new InconsistentStateException(
                s"Id $id is linked to several products in database!"
              )
          }
        }
    )
  }

  def getAllProducts(): Future[Seq[Product]] = {
    val productListFuture = db.run(products.result)

    productListFuture.map(
      (productList: Seq[(String, String, String, String, Int, Int, String)]) =>
        {
          productList.map(Product tupled _)
        }
    )
  }

  /**
    * Returns an Option containing the remaining quantity of the product
    * if it lasts more than 15 seconds or failed -> None
    */
  def buyProduct(id: String, quantity: Int): Option[Int] = {
    val productFuture = getProductById(id)
    val quantityFuture = productFuture.map(prodOption =>
      prodOption.flatMap(prod => {
        val realQuantity: Int =
          if (quantity >= prod.quantity) prod.quantity
          else quantity
        val remain: Int = prod.quantity - realQuantity
        val request =
          products
            .filter(_.productId === id)
            .map(p => (p.productQuantity))
            .update(remain)
        val requestFuture = db.run(request)
        Try(Await.result(requestFuture, 5 seconds)) match {
          case Success(0) => None
          case Success(_) => Some(realQuantity)
          case _          => None
        }
      })
    )
    Try(Await.result(quantityFuture, 10 seconds)) match {
      case Success(i) => i
      case Failure(_) => None
    }
  }

  def filterProductsBySeller(id: String) = {
    val productsSeller    = products.filter(_.productSeller === id)
    val productListFuture = db.run(productsSeller.result)

    productListFuture.map(
      (productList: Seq[(String, String, String, String, Int, Int, String)]) =>
        {
          productList.map(Product tupled _)
        }
    )
  }

  def updateProductQuantity(id: String, quantity: Int) = {
    val request =
      products
        .filter(_.productId === id)
        .map(p => (p.productQuantity))
        .update(quantity)
    val requestFuture = db.run(request)
  }
}
