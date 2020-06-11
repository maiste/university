(** From Hopix to Hobix *)
open HobixUtils

module Source = Hopix
module Target = Hobix

(** The compilation environment.
    ———————————————————————————–

    To translate a program written in a source language into another
    semantically equivalent program written in a target language, it
    is convenient to carry some information about the correspondence
    between the two programs along the process. The compilation
    environment is meant to that.

    In this particular pass, we want to remember an assignment of
    integers to constructor and label identifiers. Therefore, the
    compilation environment is composed of two maps representing these
    assignments. The environment is populated each time we cross a
    type definitions while it is read each time we translate language
    constructions related to record and tagged values.
*)

module ConstructorMap = Map.Make (struct
  type t = HopixAST.constructor
  let compare = compare
end)

module LabelMap = Map.Make (struct
  type t = HopixAST.label
  let compare = compare
end)

type environment = {
  constructor_tags : Int64.t ConstructorMap.t;
  label_positions  : Int64.t LabelMap.t;
}

let initial_environment () = {
  constructor_tags = ConstructorMap.empty;
  label_positions  = LabelMap.empty;
}

(*[fresh_index ()] returns a new index for the constructor *)
let fresh_index =
  let r = ref 0 in
  fun () -> incr r; Int64.of_int !r

let index_of_constructor env k =
  ConstructorMap.find k env.constructor_tags

let index_of_constructor' env k =
  index_of_constructor env k |> Int64.to_int

let insert_constructor env k =
  let index = fresh_index () in
  {
    env with
    constructor_tags =
      ConstructorMap.add k index env.constructor_tags
  }

let position_of_label env l =
  LabelMap.find l env.label_positions

let insert_label_position env l pos =
  let pos = Int64.of_int pos in
  {
    env with
    label_positions =
      LabelMap.add l pos env.label_positions
  }

(** Utilities *)
module ListMonad = struct
  let return x = [x]
  let fmap f x = List.map f x
  let bind xs f = List.(concat (map f xs))
end

let id_block =
  let r = ref 0 in
  fun () -> r := !r+1; HobixAST.(Id ("__block" ^ (string_of_int !r)))

let lint i = HobixAST.(Literal (LInt (Mint.of_int i)))

(** Code generation
    ———————————————

    A compilation pass produces code. We could directly
    write down caml expressions made of applications of
    HobixAST constructors. Yet, the resulting code would
    be ugly...

    A better way consists in defining functions that build
    Hobix AST terms and are convenient to use. Here are a
    list of functions that may be convenient to you when
    you will implement this pass.

*)

(** [fresh_identifier ()] returns a fresh identifier, that is
    an identifier that has never been seen before. *)
let fresh_identifier =
  let r = ref 0 in
  fun () -> incr r; HobixAST.Id ("_h2h_" ^ string_of_int !r)

let primitive_arity = function
  | "`=?`" | "`<?`" | "`>?`" | "`>=?`" | "`<=?`"
  | "`+`"  | "`-`"  |  "`*`" |  "`/`"
  | "`&&`" | "`||`"
  | "equal_string" | "equal_char" -> Some (2)
  | "print" | "print_int" | "print_string"-> Some (1)
  | _ -> None

let eta_expand_primitive (HopixAST.Id x) =
  let open HobixAST in
  let vx = Variable (Id x) in
  match primitive_arity x with
  | None -> vx
  | Some i ->
     let args = List.init i (fun _ -> fresh_identifier ()) in
     let args' = List.map (fun x -> Variable x) args in
     List.fold_right (fun x e -> Fun ([x],e)) args (Apply(vx, args'))


(** [def w (fun x -> e)] returns an abstract syntax tree of
    the form:

    val x = w; e

    where [x] is chosen fresh.
*)
let def w f =
  let x = fresh_identifier () in
  HobixAST.(Define (SimpleValue (x, w), f x))

(** [defines [d1; ..; dN] e] returns an abstract syntax tree of
    the form:

    val d1;
    ..
    val dN;
    e

*)
let defines =
  List.fold_right (fun (x, xe) e ->
      HobixAST.(Define (SimpleValue (x, xe), e)))

(** [seq s1 s2] is

    val _ = s1;
    s2

*)
let seq s1 s2 =
  HobixAST.(Define (SimpleValue (fresh_identifier (), s1), s2))

(** [htrue] represents the primitive true in Hobix. *)
let htrue =
  HobixAST.(Variable (Id "true"))

(** [seqs [s1; ...; sN] is

    val _ = s1;
    ...
    val _ = s(N - 1);
    sN
*)
let rec seqs = function
  | [] -> assert false
  | [e] -> e
  | e :: es -> seq e (seqs es)

let define_block size e =
  let id_block = id_block () in
  HobixAST.Define (SimpleValue (id_block, AllocateBlock (lint size))
                 , seq (seqs (e id_block)) (Variable id_block))

(** [conj e1 e2] is the boolean expression [e1 && e2]. *)
let conj e1 e2 =
  match e1,e2 with
  | HobixAST.(Variable (Id "false")),_ | _,HobixAST.(Variable (Id "false")) -> HobixAST.(Variable (Id "false"))
  | HobixAST.(Variable (Id "true")),_ -> e2
  | _,HobixAST.(Variable (Id "true")) -> e1
  | _ -> HobixAST.(Apply (Variable (Id "`&&`"), [ e1; e2 ]))

(** [conjs [e1; ..; eN]] is the boolean expression [e1 && .. && eN]. *)
let rec conjs = HobixAST.(function
  | [] -> htrue
  | [c] -> c
  | c :: cs -> conj c (conjs cs)
)

(** [component x i] returns [x[i]] where x is an Hobix expression
    denoting a block. *)
let component x i =
  List.fold_right (
    fun idx block ->
      HobixAST.ReadBlock(block, lint idx)
  ) i x

let located  f x = f (Position.value x)
let located' f x = Position.map f x

let arity_of_type = HopixAST.(function
  | TyVar _           -> 0
  | TyCon (_, _)     -> 0
  | TyArrow (_, _) -> 1
  | TyTuple _ -> 0
)

(** [program env p] turns an Hopix program into an equivalent
    Hobix program. *)
let rec program env p =
  let env, defs = ExtStd.List.foldmap definition' env p in
  (List.flatten defs, env)

(** Compilation of Hopix toplevel definitions. *)
and definition' env p =
  definition env (Position.value p)

and definition env = HobixAST.(function
  | HopixAST.DeclareExtern (x, s) ->
    let { Position.value = HopixAST.ForallTy (_, ty); _ } = s in
    let ty = Position.value ty in
    env, [DeclareExtern (located identifier x, arity_of_type ty)]

  | HopixAST.DefineValue vd ->
     let vd = value_definition env vd in
     env, [DefineValue vd]

  | HopixAST.DefineType (_, _, tydef) ->
    type_definition env tydef, []
)

and value_definition env = function
  | HopixAST.SimpleValue (x, _, e) ->
     HobixAST.SimpleValue (located identifier x, located (expression env) e)
  | HopixAST.RecFunctions fs ->
     HobixAST.RecFunctions (List.map (function_binding env) fs)

and function_binding env (f, _, fdef) =
  (located identifier f, function_definition env fdef)

and function_definition env (HopixAST.FunctionDefinition (x, e)) =
  let y = HopixASTHelper.fresh_identifier () in
  let wpos t = Position.(with_pos (position x) t) in
  let e = HopixAST.(
      Case (wpos (Variable (wpos y, None)),
            [
              wpos (Branch (x, e))
            ])
  )
  in
  (HobixAST.Fun ([identifier y], expression env e))

and identifier (HopixAST.Id x) =
  HobixAST.Id x

and expression' env e =
  expression env (Position.value e)

(** Compilation of Hopix expressions. *)
and expression env =
  let open HobixAST in function
  | HopixAST.Variable (x, _) ->
      located eta_expand_primitive x

  | HopixAST.Tagged (k, _, es) ->
     let size = List.length es + 1 in
     let k id_block = WriteBlock (Variable id_block, lint 0, Literal (LInt (index_of_constructor env k.value))) in
     let es id_block = List.mapi (fun i e -> WriteBlock(Variable id_block, lint (i+1), expression' env e)) es in
     define_block size (fun id -> ((k id)::(es id)))

  | HopixAST.Case (e, bs) ->
     let fail = HobixAST.(Apply (Variable (Id "`/`"), [ lint 42; lint 0 ])) in
     let bs = expands_or_patterns bs in
     if Options.get_fast_match () then
       let bs = List.map (fun (HopixAST.Branch (p,e)) -> (p, expression' env e)) bs in
       let e = expression' env e in
       PatternMatchingCompiler.translate (index_of_constructor' env) bs e
     else
       begin
         let aux (HopixAST.Branch (pat,exp)) acc =
           let cond,xs = pattern env (expression' env e) (Position.value pat) in
           HobixAST.(
             if cond = htrue
             then defines xs (expression' env exp)
             else IfThenElse (cond, defines xs (expression' env exp),acc)
           )
         in List.fold_right aux bs fail
       end

  | HopixAST.Ref e ->
     let e = expression' env e in
     let w id_block = [WriteBlock (Variable id_block, lint 0, e)] in
     define_block 1 w

  | HopixAST.Read r ->
     let r = expression' env r in
     ReadBlock (r, lint 0)

  | HopixAST.Assign (r, v) ->
     let r = expression' env r in
     let v = expression' env v in
     WriteBlock (r,lint 0, v)

  | HopixAST.While (c, b) ->
    HobixAST.While (located (expression env) c,
                    located (expression env) b)

  | HopixAST.Apply (a, b) ->
    Apply (located (expression env) a,
           [located (expression env) b])

  | HopixAST.Literal l ->
    Literal (located literal l)

  | HopixAST.Define (vd, e) ->
    Define (value_definition env vd, located (expression env) e)

  | HopixAST.TypeAnnotation (e, ty) ->
    located (expression env) e

  | HopixAST.IfThenElse (c, t, f) ->
     let f = located (expression env) f in
     HobixAST.IfThenElse (located (expression env) c,
                          located (expression env) t,
                          f)

  | HopixAST.Record (fs, _) ->
     let size = List.length fs in
     let fs id_block =
       List.map (fun (l,e) ->
           WriteBlock (Variable id_block, Literal (LInt (position_of_label env l.Position.value)),
                       expression' env e)) fs in
     define_block size fs

  | HopixAST.Tuple ts ->
     let size = List.length ts in
     let ts id_block = List.mapi (
            fun i e -> WriteBlock (Variable id_block, lint i, expression' env e)
          ) ts in
     define_block size ts

  | HopixAST.Field (e, l) ->
     let e = expression' env e in
     ReadBlock (e, Literal (LInt (position_of_label env l.value)))

  | HopixAST.Sequence es ->
     seqs (List.map (located (expression env)) es)

  | HopixAST.For (x, start, stop, e) ->
     let open HobixAST in
     let x' = fresh_identifier () in
     let create_x' cont =
       Define (SimpleValue (x', AllocateBlock (lint 1))
             , seqs [WriteBlock (Variable x', lint 0, expression' env start); cont]) in
     let readx' = ReadBlock ((Variable x'), lint 0) in
     let compare = Apply (Variable (Id "`<=?`"), [readx'; expression' env stop]) in
     let e = expression' env e in
     let addone =
       WriteBlock (Variable x', lint 0
                   , Apply (Variable (Id "`+`"), [readx'; lint 1])) in
     let whilebody =
       Define (SimpleValue (identifier (Position.value x), readx'), seqs [e;addone]) in
     create_x' (While (compare, whilebody))

  | HopixAST.Fun fdef ->
    function_definition env fdef

(** [expands_or_patterns branches] returns a sequence of branches
    equivalent to [branches] except that their patterns do not contain
    any disjunction. {ListMonad} can be useful to implement this
    transformation. *)
and expands_or_patterns branches =
  let open Position in
  let open ListMonad in
  let open HopixAST in
  let conj xs =
    fmap List.rev @@ (* NB: this conjunction keeps the order when distributing (Not very monadic...) *)
      List.fold_left
        (fun acc x -> bind acc (fun ys -> fmap (fun y -> y::ys) x))
        (return []) xs in
  let rec explode_pattern pat =
    let dumbpos = pat.position in
    let unliftpos = List.map (with_pos dumbpos) in
    let rec use_conj f xs =
      List.map (fun x -> with_pos x.position (f x.value))
        (unliftpos (conj (fmap explode_pattern xs))) in
    match pat.value with
    | PVariable _ | PLiteral _ | PWildcard ->
       return pat
    | PTypeAnnotation (x,ty) ->
       fmap (fun x -> with_pos pat.position (PTypeAnnotation (x,ty)))
         (explode_pattern x)
    | POr xs ->
       bind xs explode_pattern
    | PAnd xs ->
       use_conj (fun x -> PAnd x) xs
    | PTuple xs ->
       use_conj (fun x -> PTuple x) xs
    | PTaggedValue (c,ty,xs) ->
       use_conj (fun x -> PTaggedValue (c,ty,x)) xs
    | PRecord (xs,ty) ->
       use_conj (fun x -> PRecord (List.map2 (fun (l,_) x -> l,x) xs x,ty)) (List.map snd xs)
  in let aux x =
       let (Branch (x,e)) = Position.value x in
       fmap (fun x -> Branch (x,e)) (explode_pattern x)
  in bind branches aux

(** [pattern env scrutinee p] returns an HopixAST expression
    representing a boolean condition [c] and a list of definitions
    [ds] such that:

    - [c = true] if and only if [p] matches the [scrutinee] ;
    - [ds] binds all the variables that appear in [p].

    Precondition: p does not contain any POr.
 *)
and pattern env scrutinee p =
  let open HopixAST in
  let conj_pats pats =
    match pats with
    | [] -> assert false
    | x::xs ->
       List.fold_left
         (fun (f,defs) (facc,defacc) -> conj f facc,defs@defacc)
         x xs in
  let rec aux pi p =
    match p with
    | HopixAST.PWildcard ->
       htrue, []
    | HopixAST.PVariable x ->
       htrue,[HobixAST.((identifier x.value, component scrutinee pi))]
    | HopixAST.PLiteral i ->
       let lit = literal (Position.value i) in
       (is_equal lit (Literal lit) (component scrutinee pi)),[]
    | HopixAST.PTypeAnnotation (p, _) ->
       aux pi p.value
    | HopixAST.PTuple ps ->
       let pats = List.mapi (fun i x -> aux (i::pi) (Position.value x)) ps in
       conj_pats pats
    | HopixAST.PAnd ps ->
       let pats = List.map (fun x -> aux pi (Position.value x)) ps in
       conj_pats pats
    | HopixAST.PTaggedValue (c,_,xs) ->
       let pats = List.mapi (fun i x -> aux (i+1::pi) (Position.value x)) xs in
       let c = HobixAST.LInt (index_of_constructor env (Position.value c)) in
       let verify_constr = is_equal c (Literal c) (HobixAST.ReadBlock (component scrutinee pi, lint 0)) in
       List.fold_left (fun (f,defs) (facc,defacc) -> conj f facc,defs@defacc) (verify_constr,[]) pats
    | HopixAST.PRecord (xs,_) ->
       let pats =
         List.map
           (fun (x,p) ->
             let i = Mint.to_int (position_of_label env (Position.value x)) in
             aux (i::pi) (Position.value p))
           xs in
       conj_pats pats
    | HopixAST.POr _ ->
       assert false
  in aux [] p

(** Compilation of type definitions. *)
and type_definition env = HopixAST.(function
  | DefineSumType tys ->
      List.fold_right (
        fun (c, _) env ->
          let c = Position.value c in
          insert_constructor env c
      ) tys env
  | DefineRecordType ls ->
      let ls = List.sort compare ls in
      List.fold_right (
        fun (l,_) (env, k) ->
          let l = Position.value l in
          let env =
            insert_label_position env l k
          in (env, (k+1))
          ) ls (env, 0) |> fst
  | Abstract -> env
)

(** Here is the compiler! *)
let translate source env =
  program env source
