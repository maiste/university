
// The abstract syntax tree for javix programs.

/* See https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-6.html
   For details of jvm instructions */

package trac.javix

object AST {

import trac._

sealed case class Program
  (classname : String,
   code : List[Instruction],
   varsize : Int,
   stacksize : Int)

type Label = String
type Var = Int

sealed abstract class Instruction

/* First, some pseudo-instructions to ease things */
case class Labelize (lab:Label)                    extends Instruction
case class Comment (msg:String)                    extends Instruction
case object Box                                    extends Instruction
case object Unbox                                  extends Instruction
/* Checkarray : checks that the top of stack is an array of Object
 * (cf checkcast). To be used before aaload/aastore */
case object Checkarray                             extends Instruction
/* IPrint : a pseudo-instruction that prints the (unboxed) integer
 * on top of stack, and removes it (use "dup" before if necessary) */
case object IPrint                                 extends Instruction
/* Push(n) become const_0 or ... or bipush or sipush or ldc (according to n) */
case class Push (n:Int)                            extends Instruction
/* IOp(o) will become iadd or isub or imul or idiv or irem */
case class IOp (o:IntOp.T)                         extends Instruction
/* if_icmp{eq,ne,gt,..} : jump if comparison of two integers succeeds */
case class Ificmp (cmp:CompOp.T, lab:Label)        extends Instruction
/* if{eq,ne,gt,..} : jump if comparing an integer with 0 succeeds */
case class If (cmp:CompOp.T, lab:Label)            extends Instruction
/* Now, real JVM instructions */
case object Pop                                    extends Instruction
case object Swap                                   extends Instruction
case object Dup                                    extends Instruction
case class AStore (v:Var)                          extends Instruction
case class ALoad (v:Var)                           extends Instruction
case class IStore (v:Var)                          extends Instruction
case class ILoad (v:Var)                           extends Instruction
case class Goto (lab:Label)                        extends Instruction
case object ANewarray                              extends Instruction
case object AAStore                                extends Instruction
case object AALoad                                 extends Instruction
case object Return                                 extends Instruction
case class Tableswitch (offset:Int,tab:List[Label],default:Label)
                                                   extends Instruction
/* Extension: Strings */
/* Ldc(s) push a string on the stack (actually a pointer to it) */
case class Ldc (s:String)                          extends Instruction
/* SCat : pseudo-operation that concatenates two strings */
case object SCat                                   extends Instruction
/* SPrint : a pseudo-instruction that prints the string on top of stack */
case object SPrint                                 extends Instruction
/* Checkstring : checks that the top of stack is a string
 * (cf checkcast). To be used before SCat and SPrint */
case object Checkstring                            extends Instruction


sealed case class StackInfo
  (needed : Int, // how large should be the stack before this instruction
   max : Int, // how many extra stack elements during run (for pseudo-instrs)
   delta : Int) // stack size evolution (after - before)

def stackUse (i:Instruction) : StackInfo =
  i match {
    case Labelize(_) | Comment(_) | Goto(_) | Return => StackInfo(0,0,0)
    case Box => StackInfo(1,3,0)
    case Unbox => StackInfo(1,1,0)
    case Push(_) | Ldc(_) => StackInfo(0,1,+1)
    case Pop => StackInfo(1,0,-1)
    case Swap => StackInfo(2,0,0)
    case Dup => StackInfo(1,1,+1)
    case IOp(_) | SCat => StackInfo(2,0,-1)
    case AStore(_) | IStore(_) => StackInfo(1,0,-1)
    case ALoad(_) | ILoad(_) => StackInfo(0,1,+1)
    case Ificmp(_,_) => StackInfo(2,0,-2)
    case If(_,_) => StackInfo(1,0,-1) 
    case ANewarray | Checkarray | Checkstring => StackInfo(1,0,0)
    case IPrint | SPrint => StackInfo(1,1,-1)
    case AAStore => StackInfo(3,0,-3)
    case AALoad => StackInfo(2,0,-1)
    case Tableswitch(_,_,_) => StackInfo(1,0,-1)
  }

}
