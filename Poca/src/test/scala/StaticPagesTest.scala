import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.Matchers
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.flatspec.AnyFlatSpec
import akka.http.scaladsl.model.StatusCodes
import poca.{
  MyDatabase,
  Users,
  User,
  UserAlreadyExistsException,
  Products,
  Product,
  Routes,
  StaticPages
}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.testkit.RouteTestTimeout
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model.ContentTypes

class StaticPagesTest
    extends AnyFunSuite
    with Matchers
    with MockFactory
    with ScalatestRouteTest {

  val routeUnderTest = get {
    concat(
      path("code") {
        parameter("code") { (code) =>
          complete(StaticPages.html(StatusCode.int2StatusCode(code.toInt)))
        }
      },
      path("valid") {
        parameter("name") { (name) =>
          complete(StaticPages.html(name))
        }
      }
    )
  }

  def statusCodeTest(code: Int) = {
    test(s"Static Page ${code}") {
      val s = StaticPages.html(code)
      s.status should ===(StatusCode.int2StatusCode(code))
      s.entity.isChunked() should ===(true)
      println(s.entity.dataBytes)
      Get(s"/code?code=${code}") ~> routeUnderTest ~> check {
        status should ===(StatusCode.int2StatusCode(code))
      }
    }
  }

  val codes = List(401, 403, 404, 500)

  for (code <- codes) {
    statusCodeTest(code)
  }
}
