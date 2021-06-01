
package trac.fopix

import scala.util.parsing.input.StreamReader
import scala.util.parsing.combinator.syntactical._

object Parse extends StandardTokenParsers {

  import trac._
  import trac.BinOp._
  import trac.fopix.AST._

  lexical.delimiters +=
    ("(", ")", "[", "]", ",", ";", "?", "&", ":=", "=",
     "+", "-", "*", "/", "%", "==", "!=", "<=", "<", ">", ">=", "^")

  lexical.reserved +=
    ("val", "def", "let", "in", "if", "then", "else", "new",
     "print_int", "print_string")

  // Parsing rules (see at bottom for the special constructors)

  def program : Parser[Program] = rep (definition)

  def definition : Parser[Definition] =
    ( "val" ~! ident ~! "=" ~! expr ^^ mkVal
    | "def" ~! ident ~! tuple_idents ~! "=" ~! expr ^^ mkDef )

  // Note : sequence ";" is right-associative, comparisons are nonassoc

  def expr : Parser[Expr] = expr1 ~! opt (";" ~>! expr) ^^ mkSeq
  def expr1 : Parser[Expr] = expr2 ~! opt (compOp ~! expr2) ^^ mkComp
  def expr2 : Parser[Expr] = expr3 ~! rep (additiveOp ~! expr3) ^^ mkOps
  def expr3 : Parser[Expr] = expr4 ~! rep (multOp ~! expr4) ^^ mkOps
  def expr4 : Parser[Expr] =
    atom ~! opt (rep1 (bracket_expr1) ~! opt (":=" ~>! expr1)) ^^ mkSetGets

  def atom : Parser[Expr] =
    ( numericLit ^^ mkNum
    | stringLit ^^ Str
    | "&" ~>! ident ^^ Fun
    | "let" ~! ident ~! "=" ~! expr ~! "in" ~! expr ^^ mkLet
    | "if" ~! expr ~! "then" ~! expr ~! "else" ~! expr ^^ mkIf
    | "new" ~>! bracket_expr1 ^^ AST.mkNew
    | array_exprs ^^ AST.mkTuple
    | "?" ~>! paren_expr ~! tuple_exprs ^^ mkCall
    | "print_int" ~! "(" ~! expr ~! ")" ^^ mkPrintint
    | "print_string" ~! "(" ~! expr ~! ")" ^^ mkPrintstr
    | ident ~! opt(tuple_exprs) ^^ mkCallFunOrVar
    | paren_expr )

  def paren_expr : Parser[Expr] = "(" ~>! expr <~! ")"
  def bracket_expr1 : Parser[Expr] = "[" ~>! expr1 <~! "]"
  def tuple_idents : Parser[List[Ident]] = "(" ~>! repsep(ident, ",") <~! ")"
  def tuple_exprs : Parser[List[Expr]] = "(" ~>! repsep(expr,",") <~! ")"
  def array_exprs : Parser[List[Expr]] = "[" ~>! repsep(expr, ",") <~! "]"

  def additiveOp : Parser[Option[BinOp.T]] =
    "+" ^^^ Some(Add) | "-" ^^^ Some(Sub) | "^" ^^^ None /* ^ is for strings */
  def multOp : Parser[Option[BinOp.T]] =
    "*" ^^^ Some(Mul) | "/" ^^^ Some(Div) | "%" ^^^ Some(Mod)
  def compOp : Parser[BinOp.T] =
    ( "==" ^^^ Eq
    | "!=" ^^^ Ne
    | "<=" ^^^ Le
    | "<" ^^^ Lt
    | ">=" ^^^ Ge
    | ">" ^^^ Gt )

  // Special constructors for semantical values

  val mkVal : String ~ Ident ~ String ~ Expr => Definition =
    { case "val" ~ x ~ "=" ~ e => Val(x,e) }

  val mkDef : String ~ Ident ~ List[Ident] ~ String ~ Expr => Definition =
    { case "def" ~ f ~ args ~ "=" ~ e => Def(f,args,e) }

  val mkSeq : Expr ~ Option[Expr] => Expr =
    { case e1 ~ Some(e2) => Let("_",e1,e2)
      case e1 ~ None => e1 }

  val mkComp : Expr ~ Option[BinOp.T ~ Expr] => Expr =
    { case e1 ~ Some(o ~ e2) => Op(o,e1,e2)
      case e1 ~ None => e1 }

  val mkOp : (Expr, Option[BinOp.T] ~ Expr) => Expr =
    { case (e, Some(o) ~ e2) => Op(o,e,e2)
      case (e, None ~ e2) => AST.mkCat(e,e2) }

  val mkOps : Expr ~ List[Option[BinOp.T] ~ Expr] => Expr =
    { case e ~ el => el.foldLeft(e)(mkOp) }

  val mkGets : (Expr,List[Expr]) => Expr = (e,l) => l.foldLeft(e)(AST.mkGet)

  val mkSetGets : Expr ~ Option[List[Expr] ~ Option[Expr]] => Expr =
    { case e ~ Some((idx::l) ~ Some(v)) => AST.mkSet(mkGets(e,l),idx,v)
      case e ~ Some(l ~ None) => mkGets(e,l)
      case e ~ _ => e }

  val mkPrintint : String ~ String ~ Expr ~ String => Expr =
    { case "print_int" ~ _ ~ e ~ _ => AST.mkPrintint(e) }

  val mkPrintstr : String ~ String ~ Expr ~ String => Expr =
    { case "print_string" ~ _ ~ e ~ _ => AST.mkPrintstr(e) }

  val mkNum : String => Expr = n => Num(n.toInt)

  val mkLet : String ~ Ident ~ String ~ Expr ~ String ~ Expr => Expr =
    { case "let" ~ x ~ "=" ~ e1 ~ "in" ~ e2 => Let(x,e1,e2) }

  val mkIf : String ~ Expr ~ String ~ Expr ~ String ~ Expr => Expr =
    { case "if" ~ e1 ~ "then" ~ e2 ~ "else" ~ e3 => If(e1,e2,e3) }

  val mkCall : Expr ~ List[Expr] => Expr =
    { case e ~ args => Call (e,args) }

  val mkCallFunOrVar : Ident ~ Option[List[Expr]] => Expr =
    { case f ~ Some(args) => Call(Fun(f),args)
      case id ~ None => Var(id) }

  // Main parser entry point

  def parseFile(file : String) : Option[Program] = {
    val reader = StreamReader(new java.io.FileReader(file))
    val tokens = new lexical.Scanner(reader)
    phrase(program)(tokens) match {
      case Success(matched,_) => Some(matched)
      case e => println(e); None
    }
  }
}
