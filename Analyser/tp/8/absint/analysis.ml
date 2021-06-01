open WhileRY
open Signdomain


let rec ai_expr e aenv =
  match e with 
  | Ecst n -> val_cst n
  | Evar x -> read x aenv
  | Ebop (o, e0, e1) ->
     val_binop o (ai_expr e0 aenv) (ai_expr e1 aenv)


let ai_cond (r, x, n) aenv =
  let av = val_sat r n (read x aenv) in
  if av = val_bot
  then nr_bot aenv
  else write x av aenv


(* Computes the negation of a relational condition *)
let cneg (r, x, n) = 
  match r with
  | Cinfeq -> (Csup, x, n)
  | Csup -> (Cinfeq, x, n)


let rec postlfp f a =
  let anext = f a in
  if nr_is_le anext a 
  then a
  else postlfp f (nr_join a anext)
         
let rec ai_com (l, c) aenv =
  if nr_is_bot aenv 
  then aenv
  else 
    match c with
    | Cskip -> aenv
    | Cseq (c0, c1) -> ai_com c1 (ai_com c0 aenv)
    | Cassign (x, e) -> write x (ai_expr e aenv) aenv
    | Cinput x -> write x val_top aenv
    | Cif (b, c0, c1) ->
       nr_join 
      (ai_com c0 (ai_cond b aenv)) 
      (ai_com c1 (ai_cond (cneg b) aenv))
    | Cwhile (b, c) ->
       let f_loop = fun a -> ai_com c (ai_cond b a) in
       ai_cond (cneg b) (postlfp f_loop aenv)
