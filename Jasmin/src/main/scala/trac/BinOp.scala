
// Arithmetical operations and comparisons, to be used in AST

// First, Separate versions

package trac

object IntOp extends Enumeration {
  type T = Value
  val Add : T = Value("+")
  val Sub : T = Value("-")
  val Mul : T = Value("*")
  val Div : T = Value("/")
  val Mod : T = Value("%")

  def eval(o:T,x:Int,y:Int) : Int =
    o match {
      case Add => x + y
      case Sub => x - y
      case Mul => x * y
      case Div => x / y
      case Mod => x % y
    }
}

object CompOp extends Enumeration {
  type T = Value
  val Eq : T = Value("==")
  val Ne : T = Value("!=")
  val Le : T = Value("<=")
  val Lt : T = Value("<")
  val Ge : T = Value(">=")
  val Gt : T = Value(">")

  def neg : T => T = {
    case Eq => Ne
    case Ne => Eq
    case Le => Gt
    case Lt => Ge
    case Ge => Lt
    case Gt => Le
  }

  def sym : T => T = {
    case Eq => Eq
    case Ne => Ne
    case Le => Ge
    case Lt => Gt
    case Ge => Le
    case Gt => Lt
  }

  def eval(o:T,x:Int,y:Int) : Boolean =
    o match {
      case Eq => x == y
      case Ne => x != y
      case Lt => x < y
      case Le => x <= y
      case Gt => x > y
      case Ge => x >= y
    }
}

// Same, but in only one enumeration

object BinOp extends Enumeration {
  type T = Value
  val Add : T = Value("+")
  val Sub : T = Value("-")
  val Mul : T = Value("*")
  val Div : T = Value("/")
  val Mod : T = Value("%")
  val Eq : T = Value("==")
  val Ne : T = Value("!=")
  val Le : T = Value("<=")
  val Lt : T = Value("<")
  val Ge : T = Value(">=")
  val Gt : T = Value(">")

  val firstCmp : T = Eq

  def isCmp (o:T) : Boolean = o >= firstCmp
  def isArith (o:T) : Boolean = !isCmp(o)

  def ofArith (o:IntOp.T) : T = BinOp(o.id)
  def ofCmp (o:CompOp.T) : T = BinOp(o.id + firstCmp.id)

  def toArith (o:T) : IntOp.T =
    if (isArith(o))
      IntOp(o.id)
    else
      throw new Invalid("Not a arith binop")

  def toCmp (o:T) : CompOp.T =
    if (isCmp(o))
      CompOp(o.id - firstCmp.id)
    else
      throw new Invalid("Not a comparison")
}
