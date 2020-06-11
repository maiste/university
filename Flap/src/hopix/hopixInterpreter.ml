open Position
open Error
open HopixAST

(** [error pos msg] reports execution error messages. *)
let error positions msg =
  errorN "execution" positions msg

(** Every expression of Hopix evaluates into a [value].

   The [value] type is not defined here. Instead, it will be defined
   by instantiation of following ['e gvalue] with ['e = environment].
   Why? The value type and the environment type are mutually recursive
   and since we do not want to define them simultaneously, this
   parameterization is a way to describe how the value type will use
   the environment type without an actual definition of this type.

*)
type 'e gvalue =
  | VInt       of Mint.t
  | VChar      of char
  | VString    of string
  | VUnit
  | VTagged    of constructor * 'e gvalue list
  | VTuple     of 'e gvalue list
  | VRecord    of (label * 'e gvalue) list
  | VLocation  of Memory.location
  | VClosure   of 'e * pattern located * expression located
  | VPrimitive of string * ('e gvalue Memory.t -> 'e gvalue -> 'e gvalue)

(** Two values for booleans. *)
let ptrue  = VTagged (KId "True", [])
let pfalse = VTagged (KId "False", [])

(**
    We often need to check that a value has a specific shape.
    To that end, we introduce the following coercions. A
    coercion of type [('a, 'e)] coercion tries to convert an
    Hopix value into a OCaml value of type ['a]. If this conversion
    fails, it returns [None].
*)

type ('a, 'e) coercion = 'e gvalue -> 'a option
let fail = None
let ret x = Some x
let value_as_int      = function VInt x -> ret x | _ -> fail
let value_as_char     = function VChar c -> ret c | _ -> fail
let value_as_string   = function VString s -> ret s | _ -> fail
let value_as_tagged   = function VTagged (k, vs) -> ret (k, vs) | _ -> fail
let value_as_record   = function VRecord fs -> ret fs | _ -> fail
let value_as_location = function VLocation l -> ret l | _ -> fail
let value_as_closure  = function VClosure (e, p, b) -> ret (e, p, b) | _ -> fail
let value_as_primitive = function VPrimitive (p, f) -> ret (p, f) | _ -> fail
let value_as_bool = function
  | VTagged (KId "True", []) -> true
  | VTagged (KId "False", []) -> false
  | _ -> assert false

let from_option x =
  match x with
  | None -> failwith "from_option"
  | Some x -> x

(**
   It is also very common to have to inject an OCaml value into
   the types of Hopix values. That is the purpose of a wrapper.
 *)
type ('a, 'e) wrapper = 'a -> 'e gvalue
let int_as_value x  = VInt x
let bool_as_value b = if b then ptrue else pfalse

(**

  The flap toplevel needs to print the result of evaluations. This is
   especially useful for debugging and testing purpose. Do not modify
   the code of this function since it is used by the testsuite.

*)
let print_value m v =
  (** To avoid to print large (or infinite) values, we stop at depth 5. *)
  let max_depth = 5 in

  let rec print_value d v =
    if d >= max_depth then "..." else
      match v with
        | VInt x ->
          Mint.to_string x
        | VChar c ->
          "'" ^ Char.escaped c ^ "'"
        | VString s ->
          "\"" ^ String.escaped s ^ "\""
        | VUnit ->
          "()"
        | VLocation a ->
          print_array_value d (Memory.dereference m a)
        | VTagged (KId k, []) ->
          k
        | VTagged (KId k, vs) ->
          k ^ print_tuple d vs
        | VTuple (vs) ->
           print_tuple d vs
        | VRecord fs ->
           "{"
           ^ String.concat ", " (
                 List.map (fun (LId f, v) -> f ^ " = " ^ print_value (d + 1) v
           ) fs) ^ "}"
        | VClosure _ ->
          "<fun>"
        | VPrimitive (s, _) ->
          Printf.sprintf "<primitive: %s>" s
    and print_tuple d vs =
      "(" ^ String.concat ", " (List.map (print_value (d + 1)) vs) ^ ")"
    and print_array_value d block =
      let r = Memory.read block in
      let n = Mint.to_int (Memory.size block) in
      "[ " ^ String.concat ", " (
                 List.(map (fun i -> print_value (d + 1) (r (Mint.of_int i)))
                         (ExtStd.List.range 0 (n - 1))
               )) ^ " ]"
  in
  print_value 0 v

let print_values m vs =
  String.concat "; " (List.map (print_value m) vs)

module Environment : sig
  (** Evaluation environments map identifiers to values. *)
  type t

  (** The empty environment. *)
  val empty : t

  (** [bind env x v] extends [env] with a binding from [x] to [v]. *)
  val bind    : t -> identifier -> t gvalue -> t

  (** [update pos x env v] modifies the binding of [x] in [env] so
      that [x ↦ v] ∈ [env]. *)
  val update  : Position.t -> identifier -> t -> t gvalue -> unit

  (** [lookup pos x env] returns [v] such that [x ↦ v] ∈ env. *)
  val lookup  : Position.t -> identifier -> t -> t gvalue

  (** [mem x env] return if x is in env. *)
  val mem : identifier -> t -> bool

  (** [UnboundIdentifier (x, pos)] is raised when [update] or
      [lookup] assume that there is a binding for [x] in [env],
      where there is no such binding. *)
  exception UnboundIdentifier of identifier * Position.t

  (** [last env] returns the latest binding in [env] if it exists. *)
  val last    : t -> (identifier * t gvalue * t) option

  (** [print env] returns a human readable representation of [env]. *)
  val print   : t gvalue Memory.t -> t -> string
end = struct

  type t =
    | EEmpty
    | EBind of identifier * t gvalue ref * t

  let empty = EEmpty

  let mem x =
    let rec aux = function
    | EEmpty -> false
    | EBind (id,_,xs) ->
       id = x || aux xs
    in aux

  let bind e x v =
    EBind (x, ref v, e)

  exception UnboundIdentifier of identifier * Position.t

  let lookup' pos x =
    let rec aux = function
      | EEmpty -> raise (UnboundIdentifier (x, pos))
      | EBind (y, v, e) ->
        if x = y then v else aux e
    in
    aux

  let lookup pos x e = !(lookup' pos x e)

  let update pos x e v =
    lookup' pos x e := v

  let last = function
    | EBind (x, v, e) -> Some (x, !v, e)
    | EEmpty -> None

  let print_binding m (Id x, v) =
    x ^ " = " ^ print_value m !v

  let print m e =
    let b = Buffer.create 13 in
    let push x v = Buffer.add_string b (print_binding m (x, v)) in
    let rec aux = function
      | EEmpty -> Buffer.contents b
      | EBind (x, v, EEmpty) -> push x v; aux EEmpty
      | EBind (x, v, e) -> push x v; Buffer.add_string b "\n"; aux e
    in
    aux e

end

(**
    We have everything we need now to define [value] as an instantiation
    of ['e gvalue] with ['e = Environment.t], as promised.
*)
type value = Environment.t gvalue

(**
   The following higher-order function lifts a function [f] of type
   ['a -> 'b] as a [name]d Hopix primitive function, that is, an
   OCaml function of type [value -> value].
*)
let primitive name ?(error = fun () -> assert false) coercion wrapper f
: value
= VPrimitive (name, fun x ->
    match coercion x with
      | None -> error ()
      | Some x -> wrapper (f x)
  )

type runtime = {
  memory      : value Memory.t;
  environment : Environment.t;
}

type observable = {
  new_memory      : value Memory.t;
  new_environment : Environment.t;
  }

let literal = function
  | LInt i -> VInt i
  | LString s -> VString s
  | LChar c -> VChar c

(** [primitives] is an environment that contains the implementation
    of all primitives (+, <, ...). *)
let primitives =
  let intbin name out op =
    let error m v =
      Printf.eprintf
        "Invalid arguments for `%s': %s\n"
        name (print_value m v);
      assert false (* By typing. *)
    in
    VPrimitive (name, fun m -> function
      | VInt x ->
         VPrimitive (name, fun m -> function
         | VInt y -> out (op x y)
         | v -> error m v)
      | v -> error m v)
  in
  let bind_all what l x =
    List.fold_left (fun env (x, v) -> Environment.bind env (Id x) (what x v))
      x l
  in
  (* Define arithmetic binary operators. *)
  let binarith name =
    intbin name (fun x -> VInt x) in
  let binarithops = Mint.(
    [ ("`+`", add); ("`-`", sub); ("`*`", mul); ("`/`", div) ]
  ) in
  (* Define arithmetic comparison operators. *)
  let cmparith name = intbin name bool_as_value in
  let cmparithops =
    [ ("`=?`", ( = ));
      ("`<?`", ( < ));
      ("`>?`", ( > ));
      ("`>=?`", ( >= ));
      ("`<=?`", ( <= )) ]
  in
  let boolbin name out op =
    VPrimitive (name, fun m x -> VPrimitive (name, fun m y ->
        out (op (value_as_bool x) (value_as_bool y))))
  in
  let boolarith name = boolbin name (fun x -> if x then ptrue else pfalse) in
  let boolarithops =
    [ ("`||`", ( || )); ("`&&`", ( && )) ]
  in
  let generic_printer =
    VPrimitive ("print", fun m v ->
      output_string stdout (print_value m v);
      flush stdout;
      VUnit
    )
  in
  let print s =
    output_string stdout s;
    flush stdout;
    VUnit
  in
  let print_int =
    VPrimitive  ("print_int", fun m -> function
      | VInt x -> print (Mint.to_string x)
      | _ -> assert false (* By typing. *)
    )
  in
  let print_string =
    VPrimitive  ("print_string", fun m -> function
      | VString x -> print x
      | _ -> assert false (* By typing. *)
    )
  in
  let bind' x w env = Environment.bind env (Id x) w in
  Environment.empty
  |> bind_all binarith binarithops
  |> bind_all cmparith cmparithops
  |> bind_all boolarith boolarithops
  |> bind' "print"        generic_printer
  |> bind' "print_int"    print_int
  |> bind' "print_string" print_string
  |> bind' "true"         ptrue
  |> bind' "false"        pfalse
  |> bind' "nothing"      VUnit

let initial_runtime () = {
  memory      = Memory.create (640 * 1024 (* should be enough. -- B.Gates *));
  environment = primitives;
  }

let rec last = function
  | [] -> assert false
  | [x] -> x
  | x::xs -> last xs

let variable environment x =
  Environment.lookup x.position x.value environment

let match_lit env lit exp =
  let ret l r = if l = r then Some (env) else None
  in
  match lit, exp with
  | LInt l, VInt  r -> ret l r
  | LChar l, VChar r -> ret l r
  | LString l, VString r -> ret l r
  | _, _ -> None

let rec pattmatch env p e =
  match p, e with
  | PVariable id, _ -> Some (Environment.bind env id.value e)
  | PLiteral ip, _ -> match_lit env ip.value e
  | PTuple tp, VTuple te -> match_tuple env tp te
  | PRecord (lp,_), VRecord le -> match_record env lp le
  | PTaggedValue (cp, _, cpl), VTagged (cc, cel) ->
     if cp.value = cc then match_tuple env cpl cel
     else None
  | PWildcard, _ -> Some (env)
  | POr l, _ -> match_or env l e
  | PAnd l, _ -> match_and env l e
  | PTypeAnnotation (p,_),_ -> pattmatch env p.value e
  | _,_ -> None

and match_tuple env lp le =
  List.fold_left2 (
      fun acc p e ->
      match acc with None -> None
        | Some env -> pattmatch env p.value e
    ) (Some env) lp le

and match_record env p exp =
  List.fold_left (
    fun acc field ->
      match acc with
      | None -> None
      | Some env -> match_label env field exp
  ) (Some env)  p

and match_label env (id, p) exp =
  List.fold_left (
    fun acc (label, value) ->
      match acc with
      | None ->
          if id.value = label then
            pattmatch env p.value value
          else None
      | Some env -> Some env
  ) None exp

and match_or env lst exp =
  List.fold_left (
      fun acc p ->
      match acc with
      | None -> pattmatch env p.value exp
      | Some env -> acc
    ) None lst

and match_and env lst exp =
  List.fold_left (
      fun acc p ->
      match acc with
      | None -> None
      | Some env -> pattmatch env p.value exp
    ) (Some env) lst

let rec_functions env xs =
  let env =
    List.fold_left
      (fun env (id,_,_) -> Environment.bind env id.value VUnit) env xs in
  let closures =
    List.map
      (fun (id,_,FunctionDefinition (p,c)) ->
        id,VClosure (env, p, c)) xs in
  List.iter
    (fun (id,c) -> Environment.update id.position id.value env c) closures;
  env

let rec evaluate runtime ast =
  try
    let runtime' = List.fold_left definition runtime ast in
    (runtime', extract_observable runtime runtime')
  with Environment.UnboundIdentifier (Id x, pos) ->
    Error.error "interpretation" pos (Printf.sprintf "`%s' is unbound." x)

(** [definition pos runtime d] evaluates the new definition [d]
    into a new runtime [runtime']. In the specification, this
    is the judgment:

                        E, M ⊢ dv ⇒ E', M'

*)
and definition runtime d =
  match d.value with
  | DefineValue d -> value_definition runtime d
  | DeclareExtern _ | DefineType _ -> runtime (* TODO really ? *)

and value_definition {environment;memory} d =
  let environment =
    match d with
    | SimpleValue (id,_,e) ->
       let value = expression' environment memory e in
       Environment.bind environment id.value value
    | RecFunctions xs ->
       rec_functions environment xs
  in {environment;memory}

and expression' environment memory e =
  expression (position e) environment memory (value e)

(** [expression pos runtime e] evaluates into a value [v] if

                          E, M ⊢ e ⇓ v, M'

   and E = [runtime.environment], M = [runtime.memory].
*)
and expression position environment memory e =
  match e with
  | Literal lit ->
     literal lit.value
  | Variable (x,_) -> variable environment x
  | Tuple xs ->
     VTuple (tuple environment memory xs)
  | IfThenElse (i,t,e) ->
     let i = expression' environment memory i in
     expression' environment memory (if value_as_bool i then t else e)
  | Define (d,e) ->
     let {environment;memory} = value_definition {environment;memory} d in
     expression' environment memory e
  | Sequence xs ->
     List.hd (tuple environment memory (List.rev xs))
  | Tagged (constr,_,xs) ->
     let xs = tuple environment memory xs in
     VTagged (constr.value,xs)
  | Apply (f,x) ->
     apply environment memory f x
  | Ref x ->
     let e_val = expression' environment memory x in
     VLocation (Memory.allocate memory Mint.one e_val)
  | Read x ->
     let x_val = expression' environment memory x in
     let x_block = Memory.dereference memory (from_option (value_as_location x_val)) in
     Memory.read x_block Mint.zero
  | Assign (x,e) ->
     assign environment memory x e;
     VUnit
  | Record (r,_) ->
     VRecord (List.rev (record environment memory r))
  | Field (e, l) ->
     begin
       match expression' environment memory e with
       | VRecord r -> List.assoc l.value r
       | _ -> assert false (* by parsing *)
     end
  | Case (e, b) ->
      let v = expression' environment memory e in
      begin
        match branches environment memory b v with
        | Some v -> v
        | None -> failwith "case: error"
      end
  | While (c, e) ->
     while_loop environment memory c e ; VUnit
  | For (id, stt, stp, e) ->
     for_loop environment memory id stt stp e ;
     VUnit
  | TypeAnnotation (e,_) ->
     expression' environment memory e
  | Fun (FunctionDefinition (p,c)) ->
     VClosure (environment, p, c)

and branch env mem (Branch (p,e)) exp =
  match pattmatch env p.value exp with
  | None -> None
  | Some env -> Some (expression' env mem e)

and branches env mem lst exp =
  List.fold_left (
      fun acc b ->
      match acc with
      | Some a -> acc
      | None -> branch env mem b.value exp
    ) None lst

and tuple environment memory xs =
  List.fold_right
    (fun x xs ->
      let x =
        expression' environment memory x in
      x::xs
    ) xs []

and record environment memory xs =
  List.fold_left
    (fun xs (l,e) ->
      let v = expression' environment memory e in
      (l.value, v)::xs
    ) [] xs

and while_loop environment memory c e =
  while
    value_as_bool (expression' environment memory c)
  do
    expression' environment memory e |> ignore ;
  done

and for_loop environment memory id start stop e =
  let eval_int x =
    expression' environment memory x |>
    value_as_int |> from_option |> Mint.to_int in
  for x = (eval_int start) to eval_int stop do
    expression'
    (Environment.bind environment id.value
    (Mint.of_int x |> int_as_value)) memory e
    |> ignore ;
  done

and assign environment memory x e =
  let x_val = expression' environment memory x in
  let x_block = Memory.dereference memory (from_option (value_as_location x_val)) in
  let e_val = expression' environment memory e in
  Memory.write x_block Mint.zero e_val

and apply environment memory f x =
  let x_val = expression' environment memory x in
  match expression' environment memory f with
  | VClosure (environment,pat,expr) ->
     begin
       match pattmatch environment pat.value x_val with
       | Some environment -> expression' environment memory expr
       | None -> assert false (* by typing *)
     end
  | VPrimitive (_,f) -> f memory x_val
  | _ -> assert false (* by typing *)

(** This function returns the difference between two runtimes. *)
and extract_observable runtime runtime' =
  let rec substract new_environment env env' =
    if env == env' then new_environment
    else
      match Environment.last env' with
        | None -> assert false (* Absurd. *)
        | Some (x, v, env') ->
          let new_environment = Environment.bind new_environment x v in
          substract new_environment env env'
  in
  {
    new_environment =
      substract Environment.empty runtime.environment runtime'.environment;
    new_memory =
      runtime'.memory
  }

(** This function displays a difference between two runtimes. *)
let print_observable runtime observation =
  Environment.print observation.new_memory observation.new_environment
