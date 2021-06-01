// My try of printing Kontix AST

package trac.kontix

object MyPP {

  import trac._
  import AST._

  def print_kontix (p:Program) {
    println(kontix_to_str(p))
  }

  def kontix_to_str (p:Program) : String = {
    p match {
    case Program (defs, _main) =>
        defs_to_str(defs)
    }
  }

  def defs_to_str (defs:List[Definition]) : String = {
    defs match {
      case Nil => ""
      case DefFun (f, args, e) :: xs =>
        String.format("DefFun (%s, [%s],\n\t%s) \n%s", f, args_to_str(args), tailexpr_to_str(e), defs_to_str(xs))
      case DefCont (f, env, arg, e) :: xs =>
        String.format("DefCont (%s, [%s], %s,\n\t%s) \n%s", f, args_to_str(env), arg, tailexpr_to_str(e), defs_to_str(xs))
    }
  }

  def args_to_str(args:List[Ident]) : String = {
    args match {
      case Nil => ""
      case x :: Nil => x
      case x :: xs => x + ", " + args_to_str(xs)
    }
  }

  def tailexpr_to_str(e:TailExpr) : String = {
    e match {
      case Let (id, e1, e2) =>
        String.format ("Let (%s,\n\t%s,\n\t%s)", id, basicexpr_to_str(e1), tailexpr_to_str(e2))
      case If (c, e1, e2) =>
        String.format ("If (%s,\n\t%s,\n\t%s)", "TODO", tailexpr_to_str(e1), tailexpr_to_str(e2))
      case Call (e, args) =>
        String.format ("Call (%s, [%s])", basicexpr_to_str(e), basicexprs_to_str(args))
      case Ret (e) =>
        String.format ("Ret (%s)", basicexpr_to_str(e))
      case PushCont (c, saves, e) =>
        String.format ("PushCont(%s, [%s], %s)", c, args_to_str(saves), tailexpr_to_str(e))
    }
  }

  def basicexprs_to_str(e:List[BasicExpr]) : String = {
    e match {
      case Nil => ""
      case x :: Nil => basicexpr_to_str(x)
      case x :: xs => basicexpr_to_str(x) + ", " + basicexprs_to_str(xs)
    }
  }

  def basicexpr_to_str(e:BasicExpr) : String = {
    e match {
      case Num (n) => String.format("Num(%s)", n.toString)
      case Str (s) => String.format("Str(%s)", s)
      case Fun (fid) => String.format("Fun(%s)", fid)
      case Var (id) => String.format("Var(%s)", id)
      case BLet (id, e1, e2) => String.format("BLet (%s, %s, %s)", id, basicexpr_to_str(e1), basicexpr_to_str(e2))
      case BIf (c, e1, e2) => String.format("If(%s, %s, %s)", "TODO", basicexpr_to_str(e1), basicexpr_to_str(e2))
      case Op (o, e1, e2) => String.format ("Op(%s, %s, %s)", "TODO", basicexpr_to_str(e1), basicexpr_to_str(e2))
      case Prim (p, args) => String.format ("Prim (%s, %s)", "TODO", "TODO")
    }
  }
}
