import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.Matchers
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.flatspec.AnyFlatSpec

import poca.{Basket, BasketToken, BasketItem, Product}

class BasketTest extends AnyFunSuite with Matchers with MockFactory {
  test("BasketToken <-> Basket conversions") {
    val bt = new BasketToken("a:1::b:2::c:1")
    val b  = new Basket(bt)

    b.size should ===(3)
    b.getProductIds should contain theSameElementsAs (Array("a", "b", "c"))
    b.toToken should ===("a:1::b:2::c:1")

    val bt2: BasketToken = b
    bt2 should ===(bt)
  }

  test("Add to Basket") {
    val b = new Basket("")
    b.addProduct("a", 1)
    b.size should ===(1)
    b.toToken should ===("a:1")

    b.addProduct("a", 2)
    b.size should ===(1)
    b.toToken should ===("a:3")

    b.addProduct("b", 2)
    b.size should ===(2)
    b.toToken should ===("a:3::b:2")
  }

  test("Delete from Basket") {
    val b = new Basket("a:1::b:3")
    b.size should ===(2)

    b.deleteProduct("a")
    b.size should ===(1)
    b.toToken should ===("b:3")

    b.deleteProduct("b")
    b.size should ===(0)
    b.toToken should ===("")
  }

  test("Remove from Basket") {
    val b = new Basket("a:1::b:3")

    b.size should ===(2)

    b.removeProduct("b")
    b.size should ===(2)
    b.toToken should ===("a:1::b:2")

    b.removeProduct("a")
    b.size should ===(1)
    b.toToken should ===("b:2")

    b.removeProduct("b", 2)
    b.size should ===(0)
    b.toToken should ===("")
  }

  test("Get product by id and count") {
    val b = new Basket("a:1::b:3")
    val product = b.getProductByIdAndCount(1)
    product match {
      case (x, y) => {
        x should ===("b")
        y should ===(3)
      }
    }
  }

  test("Should fail") {
    val b = new Basket("unvalid;basket")
    b.size should ===(0)
  }
}
