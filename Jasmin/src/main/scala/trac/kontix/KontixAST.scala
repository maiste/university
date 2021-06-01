// The abstract syntax tree for kontix programs.

package trac.kontix

import trac._

object AST {

/* A Kontix program is made of a list of definitions (for both regular
 * functions and continuation functions) and a TailExpr used as a starting
 * point (i.e. a main).
 * When running a Kontix program, we use a current continuation K and
 * current environment E, which aren't mentionned in the Kontix AST,
 * but could be modified via a PushCont (see below). The execution
 * of the main starts with a current continuation which stops
 * the program and an empty current environment */

case class Program (defs:List[Definition], main:TailExpr)

type Ident = String
type FunName = String
type ContName = String

/* Formals : the list of standard argument names received by a regular
 * function. Actually, such function will also receive a continuation
 * argument K and an env E, but these extra arguments aren't listed
 * here */

type Formals = List[Ident]

/* FormalEnv : an environment received by a continuation function,
 * which is a tuple of an inner continuation K and old env E
 * (both implicit here) and the name of saved variables */

type FormalEnv = List[Ident]

sealed abstract class Definition
case class DefFun (f:FunName,args:Formals,e:TailExpr)    extends Definition
case class DefCont (f:ContName,env:FormalEnv,arg:Ident,e:TailExpr)
                                                         extends Definition

/* In pseudo-code:
 *
 * Defcont(kont,[x,y,z],r,expr)
 * ==
 * def kont(e,r) =
 *     let [K,E,x,y,z] = e in
 *     expr
 *
 * Note that expr will (implicitely) use K and E
 */

sealed abstract class TailExpr
case class Let (id:Ident,e1:BasicExpr,e2:TailExpr)       extends TailExpr
case class If (c:Comparison,e1:TailExpr,e2:TailExpr)     extends TailExpr

/* Call(f,args) launch a regular function f. The current continuation K
 * and env E are also given to f, even if they are not explicitely in args */
case class Call (e:BasicExpr,args:List[BasicExpr])       extends TailExpr

/* Ret(res) launches the current continuation K on the current
 * environment E and on the result r. */
case class Ret (e:BasicExpr)                             extends TailExpr

/* PushCont(c,ids,code) let the current continuation for code
 * become c, and the current environment becomes (old K, old E, ids...) */
case class PushCont (c:ContName,saves:List[Ident],e:TailExpr)
                                                         extends TailExpr

/* In pseudo-code :
 *
 * PushCont(cont,[x,y,z],expr)
 * ==
 * let E = [K,E,x,y,z] in
 * let K = cont in
 * expr
 *
 * Note that expr will (implicitely) use K and E
 */


sealed abstract class BasicExpr
case class Num (n:Int)                                     extends BasicExpr
case class Str (s:String)                                  extends BasicExpr
case class Fun (fid:FunName)                               extends BasicExpr
case class Var (id:Ident)                                  extends BasicExpr
case class BLet (id:Ident,e1:BasicExpr,e2:BasicExpr)       extends BasicExpr
case class BIf (c:Comparison,e1:BasicExpr,e2:BasicExpr)    extends BasicExpr
case class Op (o:IntOp.T,e1:BasicExpr,e2:BasicExpr)        extends BasicExpr
case class Prim (p:PrimOp.T,args:List[BasicExpr])          extends BasicExpr

type Comparison = (CompOp.T, BasicExpr, BasicExpr)

}

object PP {
  def pp (p:AST.Program) : String =
    fopix.PP.pp(transl.Kontix2Fopix.trans(p))
}

object Interp {
  def eval(p:AST.Program) : String =
    fopix.Interp.eval(transl.Kontix2Fopix.trans(p))
}
