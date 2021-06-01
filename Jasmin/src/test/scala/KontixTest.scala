import org.scalatest.funsuite.AnyFunSuite
import org.scalatest._
import org.scalatest.concurrent.TimeLimits._
import org.scalatest.time.SpanSugar._
import java.lang.StackOverflowError

import trac.transl._
import trac.kontix.{AST => T, Interp}
import trac.fopix.{AST => S, Parse}
import trac.Invalid

class KontixTestSuite extends AnyFunSuite {

  def testCode(file: String, res : String) : Unit = {
    val ast = Parse.parseFile(file)
    ast.foreach {
      ast => {
        failAfter(5 seconds) { // In case of timeout, fail
          info("Compile")
          val anfixAst = Fopix2Anfix.trans(ast)
          val kontixAst = Anfix2Kontix.trans(anfixAst)

          try {
            info("Interpret")
            val kontixEval = Interp.eval(kontixAst)
            info("Assert")
            assert(kontixEval == res)
          } catch {
            case e : StackOverflowError => fail("StackOverflow")
            case e : Throwable => fail("Error with " + e.getMessage())
            case _ : Exception => fail("UnknowException")
          }
        }
      }
    }
  }

  test ("Is well configured?") {
    assert(0 == 0)
  }

  test("Ack.fx") {
    testCode("examples/ack.fx", "61\n")
  }

  test("Callif.fx") {
    testCode("examples/callif.fx", "86\n")
  }

  test("Compose.fx") {
    testCode("examples/compose.fx", "81\n")
  }

  test("Even.fx") {
    testCode("examples/even.fx", "1\n")
  }

  test("Fact.fx") {
    testCode("examples/fact.fx", "3628800\n")
  }

  test("Factopt.fx") {
     testCode("examples/factopt.fx", "3628800\n")
  }

  test("Fibiter.fx") {
    testCode("examples/fibiter.fx", "89\n")
  }

  test("Fibo.fx") {
    testCode("examples/fibo.fx", "89\n")
  }

  test("Intstring.fx") {
    testCode("examples/intstring.fx", "123456\n")
  }

  test("Listmap.fx") {
    testCode("examples/listmap.fx", "12\n")
  }

  test("Positive.fx") {
   testCode("examples/positive.fx", "6 7\n")
  }

  test("Pri.fx") {
    testCode("examples/pri.fx", "")
  }

  test("Test2.fx") {
    // testCode("examples/test2.fx", "21\n")
  }

  test("Teststr.fx") {
    testCode("examples/teststr.fx", "abcdefgh\n")
  }

  test("Treelist.fx") {
    testCode("examples/treelist.fx", "7\n")
  }
}
