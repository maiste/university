open Whilery

(* Examples for binaries operations *)

(* Expression Plus *)
let plus =
  (3, Cseq (
      (1, Cassign (3, Ebop (Badd, Evar 0, Evar 1))),
      (2, Cassign (3, Ebop (Badd, Evar 3, Evar 2)))
    )
  )

(* Expression Minus *)
let minus =
  (3, Cseq (
      (1, Cassign (3, Ebop(Bsub, Evar 1, Evar 2))),
      (2, Cassign (3, Ebop(Bsub, Evar 0, Evar 0)))
    )
  )

(* Expression Mult *)
let mult =
  (3, Cseq (
      (1, Cassign (3, Ebop(Bmul, Evar 0, Evar 1))),
      (2, Cassign (3, Ebop(Bmul, Evar 3, Evar 2)))
    )
  )

(* 
   Example 3.13 [RY], page 80.

   1 is the variable x,
   0 is the variable y.
 *)
let conditional =
  (11, Cif ((Csup, 1, 7), 
            (13, Cassign (0, Ebop (Bsub, Evar 1, Ecst 7))), 
            (17, Cassign (0, Ebop (Bsub, Ecst 7, Evar 1)))))

(*
  Example 3.9(a) [RY], page 83.

  1 is the variable x.
*)
let divergence = 
  (0, Cseq 
     ((1, Cassign (1, Ecst 1)), 
      (2, Cwhile ( (Csup,1,0), 
                   (3,Cassign (1, Ebop (Badd,Evar 1,Ecst 1)))
                 )
      )))



(*
A program that does not terminate and does nothing. 
*)
let divergence_ugly = 
  (0, Cseq 
     ((1, Cassign (1, Ecst 1)), 
      (2, Cwhile ( (Csup,1,0), (3,Cskip)))))
