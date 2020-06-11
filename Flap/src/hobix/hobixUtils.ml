let literal = HobixAST.(function
  | HopixAST.LInt x -> LInt x
  | HopixAST.LString s -> LString s
  | HopixAST.LChar c -> LChar c)

(** [is_equal e1 e2] is the boolean expression [e1 = e2]. *)
let is_equal l e1 e2 =
  let equality = HobixAST.(match l with
    | LInt _ -> "`=?`"
    | LString _ -> "equal_string"
    | LChar _ -> "equal_char"
  ) in
  HobixAST.(Apply (Variable (Id equality), [e1; e2]))
