/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package poca

/**
  * Creates a Seller instance
  *
  * @param id User ID of the seller
  */
class Seller(id: String) {

  /**
    * Lists the products currently sold by this seller
    *
    * @param products Products DB table interface
    * @return Future[Seq[Product]] -- List of products
    */
  def getProducts(products: Products) = {
    products.filterProductsBySeller(id)
  }

  /**
    * Get the User instance of this seller
    *
    * @param users Users DB table interface
    * @return User -- User instance
    */
  def getUser(users: Users) = {
    users.getUserById(id)
  }

  def addProduct(products : Products, productFields :(String, String, List[String], String,String)) = {
    products.createProduct("",productFields._1,productFields._2,productFields._3,productFields._4.toInt,productFields._5.toInt,id)
  }
}
