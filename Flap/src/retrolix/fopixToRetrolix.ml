(** This module implements a compiler from Fopix to Retrolix. *)

(**

    Here are the two main problems to be solved.

    1. Fopix has complex expressions while Retrolix has only atomic
   instructions. In addition, in Fopix, scopes can arbitrarily nested
   while Retrolix has only one scope per function.

    2. Fopix is independent from the calling conventions of the target
   architecture while Retrolix is not. In particular, hardware registers
   are used in Retrolix to pass the first arguments to a function while
   in Fopix there is no such mechanism.

*)

let error pos msg =
  Error.error "compilation" pos msg

(** As in any module that implements {!Compilers.Compiler}, the source
    language and the target language must be specified. *)
module Source = Fopix
module Target = Retrolix
module S = Source.AST
module T = Target.AST

(** We are targetting the X86_64_Architecture. *)
module Arch = X86_64_Architecture

(** The compilation environment stores the list of global
    variables (to compute local variables) and a table
    representing a renaming (for alpha-conversion). *)
type environment = T.IdSet.t * (S.identifier * S.identifier) list

(** Initially, the environment is empty. *)
let initial_environment () = (T.IdSet.empty, [])

(** [fresh_label ()] returns a new identifier for a label. *)
let fresh_label =
  let c = ref 0 in
  fun () -> incr c; T.Label ("l" ^ string_of_int !c)

(** [fresh_label ()] returns a new identifier for a variable. *)
let fresh_variable =
  let c = ref 0 in
  fun () -> incr c; T.(Id ("X" ^ string_of_int !c))

(** [translate' p env] turns a Fopix program [p] into a Retrolix
    program using [env] to retrieve contextual information. *)
let rec translate' p env =
  (** The global variables are extracted in a first pass. *)
  let (globals, renaming) = env in
  let globals = List.fold_left get_globals globals p in
  let env = (globals, renaming) in
  (** Then, we translate Fopix declarations into Retrolix declarations. *)
  let defs = List.map (declaration globals) p in
  (defs, env)

and identifier (S.Id x) =
  T.Id x

and register r =
  T.((`Register (RId (Arch.string_of_register r)) : lvalue))

and get_globals env = function
  | S.DefineValue (x, _) ->
    push env x
  | _ ->
    env

and push env x =
  T.IdSet.add (identifier x) env

and declaration env = T.(function
  | S.DefineValue (S.Id x, e) ->
    let x = Id x in
    let ec = expression (`Variable x) e in
    let locals = locals env ec in
    DValues ([x], (locals, ec @ [labelled T.Ret]))

  | S.DefineFunction (S.FunId f, xs, e) ->
    Target.AST.(
      let in_registers_arguments, _, remaining = Arch.(
        ExtStd.List.asymmetric_map2
          (fun r x ->
            labelled (Assign (`Variable (identifier x),
                              Copy,
                              [(register r :> rvalue)])))
          argument_passing_registers
          xs
      )
      in
      let save_registers, restore_registers = List.(
          if Options.(get_regalloc_variant () = Realistic) then
            split (
                List.map (fun r ->
                    let x = `Variable (fresh_variable ()) in
                    let r = register r in
                    labelled (Assign (x, Copy, [(r :> rvalue)])),
                    (fun () -> labelled (Assign (r, Copy, [x])))
                  ) Arch.allocable_callee_saved_registers)
          else ([], []))
      in
      let return =
        labelled Ret
      in
      let prolog =
        in_registers_arguments
        @ save_registers
      in
      let postlog =
        (List.map (fun f -> f ()) restore_registers) @
        [return]
      in
      let function_body =
        let x = `Variable (fresh_variable ()) in
        expression x e @ [
          labelled (Assign (register Arch.return_register, Copy, [x]))
        ]
      in
      let ec = prolog @ function_body @ postlog in
      let remaining = List.map identifier remaining in
      let locals = locals env ec in
      let locals = List.filter (fun x -> not (List.mem x remaining)) locals in
      DFunction (FId f, remaining, (locals, ec))
    )

  | S.ExternalFunction (S.FunId f, _) ->
    DExternalFunction (FId f)
)
(** [expression out e] compiles [e] into a block of Retrolix
    instructions that stores the evaluation of [e] into [out]. *)
and expression out = T.(function
  | S.Literal l ->
    [labelled (Assign (out, Copy, [ `Immediate (literal l) ]))]

  | S.Variable (S.Id "true") ->
     expression out (S.(Literal (LInt (Mint.one))))

  | S.Variable (S.Id "false") ->
     expression out (S.(Literal (LInt (Mint.zero))))

  | S.Variable (S.Id x) ->
    [labelled (Assign (out, Copy, [ `Variable (Id x) ]))]

  | S.Define (S.Id x, e1, e2) ->
    (** Hey student! The following code is wrong in general,
        hopefully, you will implement [preprocess] in such a way that
        it will work, right? *)
    expression (`Variable (Id x)) e1 @ expression out e2

  | S.While (c, e) ->
     let tc = expression out e in
     let l = fresh_label () in
     let cc = condition (first_label tc) l c in
     cc @ tc @ [labelled (Jump (first_label cc))]
     @ [l, Comment "Exit of while loop"]

  | S.IfThenElse (c, t, f) ->
    let tc = expression out t
    and fc = expression out f in
    let l = fresh_label () in
    condition (first_label tc) (first_label fc) c
    @ tc
    @ [labelled (Jump l) ]
    @ fc
    @ [l, Comment "Join control point"]

  | S.FunCall (S.FunId "`&&`", [e1; e2]) ->
     expression out (S.(IfThenElse (e1, e2, Variable (Id "false"))))

  | S.FunCall (S.FunId "`||`", [e1; e2]) ->
     expression out (S.(IfThenElse (e1, Variable (Id "true"), e2)))

  | S.FunCall (S.FunId f, es) when is_binop f ->
    assign out (binop f) es

  | S.FunCall (S.FunId f, es) as e when is_condition f ->
    expression out (S.(
      IfThenElse (e, Literal (LInt Mint.one), Literal (LInt Mint.zero)))
    )

  | S.FunCall (S.FunId f, actuals) ->
    call None (`Immediate (LFun (FId f))) actuals out

  | S.UnknownFunCall (ef, actuals) ->
    let f, ef = as_rvalue ef in
    ef @ (call None f actuals out)

  | S.Switch (e, cases, default) ->
    let f, ef = as_rvalue e in
    let l = fresh_label () in
    let cases = Array.to_list cases in
    let ldefault, cdefault =
      match default with
      | None -> None, []
      | Some e -> let l, le = expression_block l out e in (Some l, le)
    in
    let branch lces = function
      | None -> (match ldefault with
                 | None -> assert false (* By exhaustiveness. *)
                 | Some l -> (lces, l))
      | Some e ->
         let (l, lces') = expression_block l out e in
         (lces @ lces', l)
    in
    let lces, lcases = ExtStd.List.foldmap branch cdefault cases in
    ef @ [
      labelled (T.Switch (f, Array.of_list lcases, ldefault))
    ]
    @ lces
    @ [(l, Comment "Join control point")]
)

and expression_block l out (e : S.expression) =
  let lstart = fresh_label () in
  let ec = expression out e in
  (lstart, [(lstart, T.Comment "Start block")] @ ec @ [labelled (T.Jump l)])

and call tail =
  match tail with
    | None ->
      normal_call
    | Some postlog ->
      failwith "Not implemented yet"

and push_input_arguments actuals =
  let xs, es = List.(split (map as_rvalue actuals)) in
  (** The first four arguments are passed to a0 ... a3. *)
  let in_registers_arguments, _, remaining = Arch.(
    ExtStd.List.asymmetric_map2
      (fun r x ->
        labelled (Target.AST.(Assign (`Register (RId (string_of_register r)),
                                      Copy,
                                      [x]))))
      argument_passing_registers
      xs
  )
  in
  (List.flatten es @ in_registers_arguments, remaining)

and normal_call f actuals out = Target.AST.(
  (** Implementing calling conventions. *)
  let _save_registers, _restore_registers = List.(
      map (fun r ->
          let x = `Variable (fresh_variable ()) in
          let r = register r in
          labelled (Assign (x, Copy, [(r :> rvalue)])),
          (fun () -> labelled (Assign (r, Copy, [x])))
      ) Arch.caller_saved_registers |> split)
  in
  let in_registers_arguments, remaining = push_input_arguments actuals in
  let result_register = register Arch.return_register in
  let get_result =
    labelled (
      Assign (out, Copy, [ (result_register :> rvalue)])
    )
  in
  in_registers_arguments
  @ [(labelled (Target.AST.Call (f, remaining, false)))]
  @ [get_result]
)

and as_rvalue e =
  let x = `Variable (fresh_variable ()) in
  (x, expression x e)

and as_rvalues rs f =
  let xs, es = List.(split (map as_rvalue rs)) in
  List.flatten es @ f xs

and assign out op rs =
  as_rvalues rs (fun xs ->
    [labelled (T.Assign (out, op, xs))]
  )

and condition lt lf c =
T.(match c with
  | S.FunCall (S.FunId "`&&`", [a; b]) ->
     let lta = fresh_label () in
     condition lta lf a
     @ [ (lta, Comment "Left-hand-side of conjunction is true.") ]
     @ condition lt lf b

  | S.FunCall (S.FunId "`||`", [a; b]) ->
     let lfa = fresh_label () in
     condition lt lfa a
     @ [ (lfa, Comment "Left-hand-side of disjunction is false.") ]
     @ condition lt lf b

  | S.FunCall (S.FunId f, [a; b]) when is_condition f ->
     as_rvalues [a; b] @@ fun args -> [
       labelled (ConditionalJump (condition_op f, args, lt, lf))
     ]
  | c ->
     let x = fresh_variable () in
     expression (`Variable x) c
     @ [ labelled (ConditionalJump (EQ, [ `Variable x;
                                          `Immediate (LInt (Mint.of_int 0)) ],
                                    lf,
                                    lt))]
)

and first_label = function
  | [] -> assert false
  | (l, _) :: _ -> l

and labelled i =
  (fresh_label (), i)

and literal = T.(function
  | S.LInt x ->
    LInt x
  | S.LFun (S.FunId f) ->
    LFun (FId f)
  | S.LChar c ->
    LChar c
  | S.LString s ->
    LString s
)

and is_binop = function
  | "`+`" | "`-`" | "`*`" | "`/`" -> true
  | c -> false

and binop = T.(function
  | "`+`" -> Add
  | "`-`" -> Sub
  | "`*`" -> Mul
  | "`/`" -> Div
  | c -> assert false (* By [is_binop] *)
)

and is_condition = function
  | "`<?`" | "`>?`" | "`=?`" | "`<=?`" | "`>=?`" -> true
  | _ -> false

and condition_op = T.(function
  | "`<?`" -> LT
  | "`>?`" -> GT
  | "`<=?`" -> LTE
  | "`>=?`" -> GTE
  | "`=?`" -> EQ
  | _ -> assert false
)

let fresh_name =
  let c = ref 0 in
  fun (S.Id x) -> incr c; S.Id (x ^ string_of_int !c)

let rec preprocess p (globals, renaming) =
  let renaming, p = ExtStd.List.foldmap declaration renaming p in
  (p, (globals, renaming))

and rename renaming x =
  let y = fresh_name x in
  ((x, y) :: renaming, y)

and declaration renaming = S.(function
  | DefineValue (x, e) ->
    let renaming, x' = rename renaming x in
    (renaming, DefineValue (x', expression renaming e))

  | DefineFunction (f, xs, e) ->
    let renaming', xs = ExtStd.List.foldmap rename renaming xs in
    (renaming, DefineFunction (f, xs, expression renaming' e))

  | ExternalFunction (f, n) ->
    (renaming, ExternalFunction (f, n))
)
and expression renaming = S.(function
  | Variable x ->
    Variable (try List.assoc x renaming with Not_found -> x)
  | Define (x, e1, e2) ->
    let renaming, x' = rename renaming x in
    Define (x',
            expression renaming e1,
            expression renaming e2)
  | FunCall (f, es) ->
    FunCall (f, List.map (expression renaming) es)
  | IfThenElse (e1, e2, e3) ->
    IfThenElse (expression renaming e1,
                expression renaming e2,
                expression renaming e3)
  | UnknownFunCall (f, es) ->
    UnknownFunCall (expression renaming f,
                    List.map (expression renaming) es)
  | Literal l ->
     Literal l
  | While (e, s) ->
     While (expression renaming e, expression renaming s)
  | Switch (e, es, d) ->
     Switch (expression renaming e,
             Array.map (Option.map (expression renaming)) es,
             Option.map (expression renaming) d)
)

(** [translate p env] turns the fopix program [p] into a semantically
    equivalent retrolix program. *)
let translate p env =
  let p, env = preprocess p env in
  let p, env = translate' p env in
  (p, env)
