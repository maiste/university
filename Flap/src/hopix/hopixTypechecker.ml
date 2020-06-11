(** This module implements a type checker for Hopix. *)
open HopixAST
open HopixTypes
open HopixPrettyPrinter
open Position

let initial_typing_environment = HopixTypes.initial_typing_environment

type typing_environment = HopixTypes.typing_environment

let type_error = HopixTypes.type_error

let located f x = f (Position.position x) (Position.value x)

let string_of_con (TCon str) = str
let string_of_label (LId id) = id

let from_option x =
  match x with
  | None -> assert false
  | Some x -> x

(** [check_program_is_fully_annotated ast] performs a syntactic check
 that the programmer wrote sufficient type annotations for [typecheck]
 to run correctly. *)
let check_program_is_fully_annotated ast =
  (**
      We check the presence of type ascriptions on:
      - variables
      - tagged values patterns
   *)
  let rec program p = List.iter (located definition) p

  and definition pos = function
    | DefineValue vdef ->
      value_definition vdef
    | _ ->
      ()

  and value_definition = function
    (** A toplevel definition for a value. *)
    | SimpleValue (x, s, e) ->
       if s = None then missing_type_annotation (Position.position x);
       located expression e
    (** A toplevel definition for mutually recursive functions. *)
    | RecFunctions fs ->
       List.iter function_definition fs

  and function_definition = function
    | (f, s, FunctionDefinition (xs, e)) ->
       if s = None then missing_type_annotation (Position.position f);
       located expression e

  and expression pos = function
    | Define (vdef, e) ->
       value_definition vdef;
       located expression e
    | Apply (a, b) ->
       List.iter (located expression) [a; b]
    | Tuple ts ->
       List.iter (located expression) ts
    | Record (fields, a) ->
       if a = None then type_error pos "A type annotation is missing.";
       List.iter (fun (_, e) -> located expression e) fields
    | TypeAnnotation ({ Position.value = Fun (FunctionDefinition (xs, e)) },
                      _) ->
       located expression e
    | Fun (FunctionDefinition (_, _)) ->
       type_error pos "An anonymous function must be annotated."
    | Field (e, _) | TypeAnnotation (e, _) | Ref e | Read e ->
       located expression e
    | Sequence es ->
       List.iter (located expression) es
    | Tagged (_, a, es) ->
       if a = None then type_error pos "A type annotation is missing.";
       List.iter (located expression) es
    | For (_, e1, e2, e3) ->
       List.iter (located expression) (
           [ e1; e2; e2 ]
         )
    | IfThenElse (c, t, f) ->
       List.iter (located expression) [c; t; f]
    | Case (e, bs) ->
      located expression e;
      List.iter (located branch) bs
    | Assign (e1, e2) | While (e1, e2) ->
      located expression e1;
      located expression e2
    | Literal _ | Variable _ ->
      ()

  and pattern pos = function
    | PTypeAnnotation ({ Position.value = (PWildcard | PVariable _) }, _) ->
      ()
    | PRecord (fields, a) ->
       if a = None then type_error pos "A type annotation is missing.";
       List.iter (fun (_, p) -> located pattern p) fields
    | PTuple ps ->
       List.iter (located pattern) ps
    | PTypeAnnotation (p, _) ->
      located pattern p
    | PVariable _ | PWildcard ->
      missing_type_annotation pos
    | PTaggedValue (_, a, ps) ->
       if a = None then type_error pos "A type annotation is missing.";
       List.iter (located pattern) ps
    | POr ps | PAnd ps ->
      List.iter (located pattern) ps
    | PLiteral _ ->
       ()

  and branch pos = function
    | Branch (p, e) ->
      located pattern p;
      located expression e
  and missing_type_annotation pos =
    type_error pos "A type annotation is missing."
  in
  program ast

let invalid_instantiation pos given expected =
  type_error pos (
      Printf.sprintf
        "Invalid number of types in instantiation: \
         %d given while %d were expected." given expected
    )

let typecheck_lit = function
  | LInt _    -> hint
  | LString _ -> hstring
  | LChar _   -> hchar

let rec extract_last xs =
  match xs with
  | [] -> failwith "extract_last"
  | [x] -> [],x
  | x::xs ->
     let ys,y = extract_last xs in
     x::ys,y

(** [check_expected_type pos xty ity] verifies that the expected
      type [xty] is syntactically equal to the inferred type [ity]
      and raises an error otherwise. *)
let check_expected_type pos xty ity =
  if xty <> ity then
    type_error pos (
        Printf.sprintf "Type error:\nExpected:\n  %s\nGiven:\n  %s\n"
          (print_aty xty) (print_aty ity)
      )

let destruct_arrow = function
  | ATyArrow (_, right) -> right
  | _ as m -> m

let apply_arrow_type aty_arrow argst =
  let (expected_args,res) = destruct_arrows aty_arrow in
  List.iter2
    (fun expected (pos,actual) -> check_expected_type pos expected actual)
    expected_args argst;
  res

 let sort_record xs =
    List.sort (
      fun (l1,_) (l2, _) ->
        let l1 = match l1.value with LId l1 -> l1 in
        let l2 = match l2.value with LId l2 -> l2 in
        String.compare l1 l2
    ) xs

  let sort_label xs =
    List.sort (
      fun (LId l1) (LId l2) -> String.compare l1 l2
    ) xs

  let check_same_label l1 l2 =
    let l1 = sort_record l1 in
    let l2 = sort_label l2 in
    List.iter2 (
      fun ({value=LId l1;position}, _) (LId l2) ->
        if l1 <> l2 then
          type_error position "Undifined record label"
        else ()
    ) l1 l2

  let check_same_record_type l1 l2 =
    let l1 = sort_record l1 in
    let l2 = sort_label l2 in
    List.iter (
      fun({value=elt;position}, _) ->
        if not (List.mem elt l2) then
            type_error position "Incorrect label"
        else ()
    ) l1

  let check_duplicate p =
    let rec aux acc = function
    | PWildcard -> acc
    | PLiteral l -> acc
    | PVariable {value=Id id;position} ->
        if List.mem id acc then
          Printf.sprintf "Variable %s defined twice" id
          |> type_error position
        else id::acc
    | PTypeAnnotation (p, _) -> aux acc p.value
    | PTaggedValue (_,_, xs) | PTuple (xs)
    | PAnd (xs) | POr (xs) ->
        List.fold_left (fun acc x -> aux acc x.value) acc xs
    | PRecord (xs, _) ->
        List.fold_left (fun acc (l,x) -> aux acc x.value) acc xs
    in aux [] p |> ignore

(** [typecheck tenv ast] checks that [ast] is a well-formed program
    under the typing environment [tenv]. *)
let typecheck tenv ast : typing_environment =
  check_program_is_fully_annotated ast;

  let rec program p =
    List.fold_left (fun env x -> located (definition env) x) tenv p

  and definition tenv pos = function
    | DefineValue vdef ->
       value_definition tenv vdef

    | DefineType (t, ts, tdef) ->
       let ts = List.map Position.value ts in
       HopixTypes.bind_type_definition (Position.value t) ts tenv tdef

    | DeclareExtern (x, s) ->
       let s = located (type_scheme tenv) s in
       bind_value (Position.value x) s tenv

  and type_scheme tenv pos (ForallTy (ts, ty)) =
    let ts = List.map Position.value ts in
    let tenv = bind_type_variables pos tenv ts in
    Scheme (ts, internalize_ty tenv ty)

  and bind_type_variables pos tenv ts =
    List.iter (fun v ->
        if HopixTypes.is_type_variable_defined pos tenv v then
          type_error pos (
              Printf.sprintf
                "The type variable `%s' is already bound in the environment."
                (HopixPrettyPrinter.(to_string type_variable v))
            )
      ) ts;
    HopixTypes.bind_type_variables pos tenv ts

  and value_definition (tenv : typing_environment) = function
    | SimpleValue (x, Some s, e) ->
       let pos = Position.position s in
       let Scheme (ts, aty) as s = located (type_scheme tenv) s in
       let tenv' = bind_type_variables pos tenv ts in
       check_expression_monotype tenv' aty e;
       bind_value (Position.value x) s tenv

    | SimpleValue (_, _, _) ->
       assert false (* By check_program_is_fully_annotated. *)

    | RecFunctions fs ->
       recursive_definitions tenv fs

  and recursive_definitions tenv recdefs =
    let tenv =
      List.fold_left (fun tenv (f, fs, d) ->
          let fs = from_option fs in (* By check_program_is_fully_annotated. *)
          let f = Position.value f in
          let fs = located (type_scheme tenv) fs in
          let fs = refresh_type_scheme fs in
          bind_value f fs tenv
        ) tenv recdefs
    in
    List.iter (fun (f, fs, d) ->
        let fs = from_option fs in
        let pos = Position.position f in
        let fs = located (type_scheme tenv) fs in
        check_function_definition pos tenv fs d
      ) recdefs;
    tenv

  (** [check_function_definition tenv fdef] checks that the
      function definition [fdef] is well-typed with respect to the
      type annotations written by the programmer. We assume that
      [tenv] already contains the type scheme of the function [f]
      defined by [fdef] as well as all the functions which are
      mutually recursively defined with [f]. *)
  and check_function_definition pos tenv aty = function
    | FunctionDefinition (p, e) ->
       match aty with
       | Scheme (ts, ATyArrow (xty, out)) ->
          let tenv = bind_type_variables pos tenv ts in
          let tenv, _ = located (pattern tenv) p in
          check_expression_monotype tenv out e
       | _ ->
          type_error pos "A function must have an arrow type."

  (** [check_expression_monotype tenv xty e] checks if [e] has
      the monotype [xty] under the context [tenv]. *)
  and check_expression_monotype tenv xty e : unit =
    let pos = Position.position e in
    let ity = located (type_of_expression tenv) e in
    check_expected_type pos xty ity

  and type_of_expression' tenv x = type_of_expression tenv x.position x.value

  (** [type_of_expression tenv pos e] computes a type for [e] if it exists. *)
  and type_of_expression tenv pos : expression -> aty = function
    | Literal l ->
       typecheck_lit l.value
    | Variable (v,ty) ->
       variable tenv v ty
    | Tuple xs ->
       let xs = List.map (type_of_expression' tenv) xs in
       ATyTuple xs
    | Ref e ->
       href (type_of_expression' tenv e)
    | Apply (f,x) ->
       apply tenv f x
    | Fun (FunctionDefinition (pat,e)) ->
       fundef tenv pat e
    | Assign (v,e) ->
       assign tenv v e
    | IfThenElse (i,t,e) ->
       ifthenelse tenv i t e
    | Read r ->
       read tenv r
    | While (cond,e) ->
       while_loop tenv cond e
    | For (id, from, todo, e) ->
       for_loop tenv id from todo e
    | Sequence xs ->
       sequence tenv xs
    | TypeAnnotation (e,ty) ->
       let et = type_of_expression' tenv e in
       check_expected_type e.position (aty_of_ty' ty) et;
       et
    | Define (v,e) ->
       let tenv = value_definition tenv v in
       type_of_expression' tenv e
    | Tagged (constr,tys,e) ->
       tagged tenv constr tys e
    | Field (e,l) ->
       field tenv e l
    | Case (exp,branches) ->
       case tenv exp branches
    | Record (xs, tys) ->
        record tenv xs tys

  and field tenv e l =
    let err_pos x = type_error x.position in
    let et = type_of_expression' tenv e in
    match et with
    | ATyCon (con,ty_list) ->
       begin
         let (_,infos) = List.assoc con tenv.type_constructors in
         match infos with
         | Record xs ->
            if List.mem l.value xs
            then
              let scheme = lookup_type_scheme_of_label l.value tenv in
              destruct_arrow (instantiate_type_scheme scheme ty_list)
            else
              err_pos l @@
                  Printf.sprintf "Type error:\n%s is not a field of record %s.\n"
                    (string_of_label l.value) (string_of_con con)
         | _ ->
            err_pos e @@
                Printf.sprintf "Type error:\n%s is not a record.\n"
                  (string_of_con con)
       end
    | _ ->
       err_pos e @@
         Printf.sprintf "Type error:\n%s is not a record.\n"
           (print_aty et)

  and variable tenv v ty =
    let actual = lookup_type_scheme_of_value v.position v.value tenv in
    match ty with
    | None -> type_of_monotype actual
    | Some tys ->
       let tys = List.map (fun x -> aty_of_ty x.value) tys in
       instantiate_type_scheme actual tys

  and tagged tenv constr tys args =
    let c_scheme = lookup_type_scheme_of_constructor constr.value tenv in
    let tys = List.map (fun x -> aty_of_ty x.value) (from_option tys) in
    let aty_of_constr = instantiate_type_scheme c_scheme tys in
    let argst = List.map (fun x -> x.position, type_of_expression' tenv x) args in
    apply_arrow_type aty_of_constr argst

  and case tenv exp branches =
    let expt = type_of_expression' tenv exp in
    let test_branch maybe_t x =
      let (Branch (pat,e)) = x.value in
      let tenv,patt = pattern tenv pat.position pat.value in
      check_expected_type x.position expt patt;
      let et = type_of_expression' tenv e in
      begin
        match maybe_t with
        | None -> ()
        | Some ty ->
           check_expected_type e.position ty et
      end;
      Some et in
    from_option @@ List.fold_left test_branch None branches

  and record tenv xs ty =
    let tyCon, _, labels =
      let label = (List.hd xs |> fst).value in
      lookup_type_constructor_of_label label tenv
    in
    let ty =
      List.map (fun t -> (t.value |> aty_of_ty)) (from_option ty)
    in
    check_same_label xs labels;
    check_labels tenv xs ty;
    ATyCon (tyCon, ty)


  and check_labels tenv xs ty =
    List.iter (
      fun (l, e) ->
        let label_scheme = lookup_type_scheme_of_label l.value tenv in
        let l' = instantiate_type_scheme label_scheme ty in
        let t = destruct_arrow l' in
        let e = type_of_expression' tenv e in
        check_expected_type l.position t e
    ) xs

  and while_loop tenv cond e =
    let condt = type_of_expression' tenv cond in
    check_expected_type cond.position hbool condt;
    let et = type_of_expression' tenv e in
    check_expected_type e.position hunit et;
    hunit

  and for_loop tenv id from todo e =
    let fromt = type_of_expression' tenv from in
    check_expected_type from.position hint fromt;
    let todot = type_of_expression' tenv todo in
    check_expected_type todo.position hint todot;
    let tenv =
      let values =
        (id.value, monotype hint)::tenv.values in
      {tenv with values} in
    let et = type_of_expression' (tenv) e in
    check_expected_type e.position hunit et;
    hunit

  and sequence tenv xs =
    let xs,x = extract_last xs in
    List.iter
      (fun x -> check_expected_type x.position hunit (type_of_expression' tenv x))
      xs;
    type_of_expression' tenv x

  and apply tenv f x =
    let ft = type_of_expression' tenv f in
    match ft with
    | ATyArrow (fromt,dirt) ->
       let xt = type_of_expression' tenv x in
       check_expected_type x.position fromt xt;
       dirt
    | _ ->
       let err = "This expression has type "
                 ^ print_aty ft
                 ^ ". This is not a function and cannot be applied."
       in type_error f.position err

  and fundef tenv pat e =
    let tenv, pat = pattern tenv pat.position pat.value in
    let e = type_of_expression' tenv e in
    ATyArrow (pat,e)

  and assign tenv v e =
    let vt = type_of_expression' tenv v in
      match type_of_reference_type vt with
      | vt ->
         let et = type_of_expression' tenv e in
         check_expected_type v.position et vt;
         hunit
      | exception NotAReference ->
         let err = "This expression has type "
                   ^ print_aty vt
                   ^ ". This is not a reference and cannot be set."
         in type_error v.position err

  and ifthenelse tenv i t e =
    let it = type_of_expression' tenv i in
    check_expected_type i.position hbool it;
    let tt = type_of_expression' tenv t in
    let et = type_of_expression' tenv e in
    check_expected_type e.position tt et;
    tt

  and read tenv r =
    let rt = type_of_expression' tenv r in
    try
      type_of_reference_type rt
    with
    | NotAReference ->
       let err = "This expression has type "
                 ^ print_aty rt
                 ^ ". This is not a reference and cannot be dereferenced."
       in type_error r.position err

  and patterns tenv = function
    | [] ->
       tenv, []
    | p :: ps ->
       let tenv, ty = located (pattern tenv) p in
       let tenv, tys = patterns tenv ps in
       tenv, (p.position,ty) :: tys

  (** [pattern tenv pos p] computes a new environment completed with
      the variables introduced by the pattern [p] as well as the type
      of this pattern. *)
  and pattern tenv pos = function
    | PWildcard -> assert false
    | PLiteral l ->
        let ty = typecheck_lit l.value
        in tenv, ty
    | PVariable id -> assert false
    | PTypeAnnotation (p, ty) ->
        let ty = aty_of_ty' ty in
        begin
          match p.value with
          | PWildcard -> tenv, ty
          | PVariable id -> bind_value id.value (monotype ty) tenv, ty
          | _ ->
              let tenv, ty' = pattern tenv pos p.value in
                check_expected_type pos ty ty' ; tenv, ty'
        end
    | PTuple (xs) as t ->
       check_duplicate t;
       pattern_tuple tenv pos xs
    | PAnd (xs) as a ->
        check_duplicate a;
        pattern_list tenv pos xs
    | POr (xs) ->
       pattern_list tenv pos xs
    | PTaggedValue (constr,tys,pats) as c->
        check_duplicate c;
        pattern_tagged tenv constr tys pats
    | PRecord (xs,ty) as r ->
        check_duplicate r;
        pattern_record  tenv xs ty

  and pattern_tagged tenv constr tys pats =
    let c_scheme = lookup_type_scheme_of_constructor constr.value tenv in
    let tys = List.map (fun x -> aty_of_ty x.value) (from_option tys) in
    let aty_of_constr = instantiate_type_scheme c_scheme tys in
    let tenv,argst = patterns tenv pats in
    tenv,apply_arrow_type aty_of_constr argst

  and pattern_tuple tenv pos xs =
    let tenv, tys =
      List.fold_left (
        fun (tenv, tys) p ->
          let (tenv,ty) = pattern tenv pos p.value in
          (tenv, ty::tys)
      ) (tenv, []) xs in
      (tenv, ATyTuple (List.rev tys))

  and pattern_list tenv pos xs =
    let oenv, oty = pattern tenv pos ((List.hd xs).value) in
    let tenv = List.fold_left (
      fun tenv p ->
        let tenv, ty = pattern tenv p.position p.value in
        check_expected_type pos ty oty ;
        tenv
    ) oenv xs
    in tenv, oty

  and check_label_get_env tenv xs ty =
    List.fold_left (
      fun tenv (l, p) ->
        let label_scheme = lookup_type_scheme_of_label l.value tenv in
        let l' = instantiate_type_scheme label_scheme ty in
        let _,t = destruct_arrows l' in
        let (tenv,p) = pattern tenv l.position p.value in
        check_expected_type l.position t p;
        tenv
    ) tenv xs

  and pattern_record tenv xs ty =
    let tyCon, _, labels =
      let label =  (List.hd xs |> fst).value in
      lookup_type_constructor_of_label label tenv
    in
    let ty =
       List.map (fun t -> (t.value |> aty_of_ty)) (from_option ty)
    in
    check_same_record_type xs labels;
    (check_label_get_env tenv xs ty, ATyCon (tyCon, ty))

  in
  program ast

let print_typing_environment = HopixTypes.print_typing_environment
