/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package poca

import java.util.concurrent.TimeUnit

import poca.{Product, Products}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.collection.immutable.ListMap
import html.product
import com.typesafe.scalalogging.LazyLogging

case class BasketToken(token: String)

object BasketToken {
  implicit def basketTokenToString(b: BasketToken) = b.token
  implicit def basketTokenToBasket(b: BasketToken) = new Basket(b)
  implicit def fromString(s: String) = new BasketToken(s)
  implicit def fromBasket(b: Basket) = new BasketToken(b.toToken)
}

case class BasketItem(product: Product, count: Integer) {
  def hasValidProduct = product.id != ""
}

object BasketItem {
  implicit def basketItemToString(b: BasketItem) =
    b.product.toString() :+ s" x ${b.count}"
}

class Basket(
    private var productsList: Map[String, Integer] // (id -> count)
) extends LazyLogging {

  /**
    * @return New empty basket
    */
  def this() = {
    this(new ListMap[String, Integer])
  }

  /**
    * Creates a Basket from a cookie String token
    *
    * @param token Cookie String token
    * @return Basket
    */
  def this(token: String) = {
    this(new ListMap[String, Integer])
    parseToken(token)
  }

  /**
    * Parses the cookie String token.
    *
    * A string token is defined as follows:
    * `id1:n1::id2:n2::id3:n3 ... `
    *
    * @param token
    */
  private def parseToken(token: String) = {
    if (token != "") {
      for (t <- token.split("::")) {
        try {
          val value = parseProductToken(t)
          productsList += (value._1 -> value._2)
        } catch {
          case e: IllegalArgumentException =>
            logger.error(s"Wrong token form: {${t}} in {${token}}")
        }
      }
    }
  }

  @throws[IllegalArgumentException]
  private def parseProductToken(token: String): (String, Integer) = {
    val params = 2

    val tok: Array[String] = token.split(":")
    if (tok.size != params) {
      throw new IllegalArgumentException(
        "Not enough arguments in token string."
      )
    }
    (tok(0), tok(1).toInt)
  }

  /**
    * Creates a cookie token String from Basket
    *
    * @return `id1:n1::id2:n2::id3:n3 ... `
    */
  def toToken: String = {
    productsList.map(_.productIterator.mkString(":")).mkString("::")
  }

  /**
    * Number of different products
    *
    * @return Number of different products
    */
  def size = {
    productsList.size
  }

  def getProductIds: Array[String] = {
    productsList.keys.toArray
  }

  def getProductByIdAndCount: Array[(String, Integer)] = {
    productsList.toArray
  }

  /**
    * Returns an Future[List[BasketItem]] of the products in the basket
    *
    * @param products Implicit Products factory
    * @return
    */
  def getItems()(implicit products: Products): Future[List[BasketItem]] = {
    Future(
      productsList
        .map(k =>
          k match {
            case (id: String, v: Integer) => {
              val maybeProd = Await.result(
                products.getProductById(id),
                Duration(10, TimeUnit.SECONDS)
              )
              maybeProd match {
                case Some(p) => BasketItem(p, v)
                case None    => BasketItem(new Product, v)
              }
            }
          }
        )
        .filter(bi => bi.hasValidProduct)
        .toList
    )
  }

  /**
    * Adds a new product. If exists increments the count of the given product
    *
    * @param id Product ID
    * @param n How many to add. Defaults to 1
    */
  def addProduct(id: String, n: Integer = 1) {
    if (productsList.contains(id)) {
      productsList = productsList.updated(id, productsList(id) + n)
    } else {
      productsList += (id -> n)
    }
  }

  /**
    * Decrements the number of copies of a given product by `n`.
    * If count is negative or naught, it deletes the product from the basket.
    *
    * @param id Product ID
    * @param n How many to remove. Defaults to 1
    */
  def removeProduct(id: String, n: Integer = 1) {
    if (productsList.contains(id)) {
      if (productsList(id) - n <= 0) {
        deleteProduct(id)
      } else {
        productsList = productsList.updated(id, productsList(id) - n)
      }
    }
  }

  /**
    * Deletes the product from the basket.
    *
    * @param id Product ID
    */
  def deleteProduct(id: String) {
    if (productsList.contains(id)) {
      productsList -= id
    }
  }
}
