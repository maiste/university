open Position
open Error
open FopixAST

(** [error pos msg] reports runtime error messages. *)
let error positions msg =
  errorN "execution" positions msg

(** Every expression of fopi evaluates into a [value]. *)
type value =
  | VUnit
  | VInt       of Mint.t
  | VBool      of bool
  | VChar      of char
  | VString    of string
  | VAddress   of Memory.location
  | VFun       of function_identifier

type 'a coercion = value -> 'a option
let value_as_int      = function VInt x -> Some x | _ -> None
let value_as_bool     = function VBool x -> Some x | _ -> None
let value_as_address  = function VAddress x -> Some x | _ -> None
let value_as_unit     = function VUnit -> Some () | _ -> None

type 'a wrapper = 'a -> value
let int_as_value x  = VInt x
let bool_as_value x = VBool x
let address_as_value x = VAddress x
let unit_as_value () = VUnit

let print_value m v =
  let max_depth = 5 in

  let rec print_value d v =
    if d >= max_depth then "..." else
      match v with
        | VInt x ->
          Mint.to_string x
        | VBool true ->
          "true"
        | VBool false ->
          "false"
        | VChar c ->
          "'" ^ Char.escaped c ^ "'"
        | VString s ->
          "\"" ^ String.escaped s ^ "\""
        | VUnit ->
          "()"
        | VAddress a ->
          print_block m d a
        | VFun _ ->
          "<fun>"
  and print_block m d a =
    let b = Memory.dereference m a in
    let vs = Array.to_list (Memory.array_of_block b) in
    "[ " ^ String.concat "; " (List.map (print_value (d + 1)) vs) ^ " ]"
  in
  print_value 0 v

module Environment : sig
  type t
  val initial : t
  val bind    : t -> identifier -> value -> t
  exception UnboundIdentifier of identifier
  val lookup  : identifier -> t -> value
  val last    : t -> (identifier * value * t) option
  val print   : value Memory.t -> t -> string
end = struct
  type t = (identifier * value) list

  let initial = []

  let bind e x v = (x, v) :: e

  exception UnboundIdentifier of identifier

  let _ =
    Printexc.register_printer (function
        | UnboundIdentifier (Id x) ->
           Some (Printf.sprintf "Unbound identifier %s" x)
        | _ ->
           None
      )

  let lookup x e =
    try
      List.assoc x e
    with Not_found ->
      raise (UnboundIdentifier x)

  let last = function
    | [] -> None
    | (x, v) :: e -> Some (x, v, e)

  let print_binding memory (Id x, v) =
    (* Identifiers starting with '_' are reserved for the compiler.
       Their values must not be observable by users. *)
    if x.[0] = '_' then
      ""
    else
      x ^ " = " ^ print_value memory v

  let print memory env =
    String.concat "\n" (
      List.(filter (fun s -> s <> "") (map (print_binding memory) env))
    )

end

type runtime = {
    memory : value Memory.t;
    environment : Environment.t;
    functions   : (function_identifier * (formals * expression)) list;
}

type observable = {
    new_environment : Environment.t;
  }

let initial_runtime () =
  let bind_bool s b env = Environment.bind env (Id s) (VBool b) in
  let bind_unit s env = Environment.bind env (Id s) VUnit in
  {
    memory = Memory.create (640 * 1024);
    environment =
      Environment.initial
      |> bind_bool "true"  true
      |> bind_bool "false" false
      |> bind_unit "nothing";
    functions  = [];
  }

let rec evaluate runtime ast =
  let runtime = List.fold_left bind_function runtime ast in
  let runtime' = List.fold_left declaration runtime ast in
  (runtime', extract_observable runtime runtime')

and bind_function runtime = function
  | DefineValue _ ->
    runtime

  | DefineFunction (f, xs, e) ->
    { runtime with
      functions = (f, (xs, e)) :: runtime.functions
    }

  | ExternalFunction _ ->
    runtime

and declaration runtime = function
  | DefineValue (i, e) ->
    let v = expression runtime e in
    { runtime with environment = Environment.bind runtime.environment i v }
  | DefineFunction _ ->
    runtime
  | ExternalFunction _ ->
    runtime

and arith_operator_of_symbol = function
  | "`+`" -> Mint.add
  | "`-`" -> Mint.sub
  | "`/`" -> Mint.div
  | "`*`" -> Mint.mul
  | _ -> assert false

and cmp_operator_of_symbol = function
  | "`<?`" -> ( < )
  | "`>?`" -> ( > )
  | "`<=?`" -> ( <= )
  | "`>=?`" -> ( >= )
  | "`=?`" -> ( = )
  | _ -> assert false

and boolean_operator_of_symbol = function
  | "`&&`" -> ( && )
  | "`||`" -> ( || )
  | _ -> assert false

and evaluation_of_binary_symbol environment = function
  | ("`+`" | "`-`" | "`*`" | "`/`") as s ->
    arith_binop environment (arith_operator_of_symbol s)
  | ("`<?`" | "`>?`" | "`<=?`" | "`>=?`" | "`=?`") as s ->
    arith_cmpop environment (cmp_operator_of_symbol s)
  | _ -> assert false

and is_binary_primitive = function
  | "`+`" | "`-`" | "`*`" | "`/`" | "`<?`"
  | "`>?`" | "`<=?`" | "`>=?`" | "`=?`"
  | "`&&`" | "`||`" -> true
  | _ -> false

and expression runtime = function
  | Literal l ->
    literal l

  | Variable (Id "true") ->
     VBool true

  | Variable (Id "false") ->
     VBool false

  | Variable x ->
    Environment.lookup x runtime.environment

  | While (cond, e) ->
     let rec loop () =
       match expression runtime cond with
       | VBool true ->
          ignore (expression runtime e);
          loop ()
       | VBool false ->
          ()
       | _ ->
          assert false (* By typing. *)
     in
     loop ();
     VUnit

  | Switch (e, bs, default) ->
    begin match value_as_int (expression runtime e) with
      | None -> error [] "Switch on integers only."
      | Some i ->
        let i = Mint.to_int i in
        if i < Array.length bs && bs.(i) <> None then
          match bs.(i) with
          | None -> assert false (* By condition. *)
          | Some t -> expression runtime t
        else match default with
          | Some t -> expression runtime t
          | None -> error [] "No default case in switch."
    end

  | IfThenElse (c, t, f) ->
    begin match value_as_bool (expression runtime c) with
      | Some true -> expression runtime t
      | Some false -> expression runtime f
      | None -> error [] "Condition is not a boolean."
    end

  | Define (x, ex, e) ->
    let v = expression runtime ex in
    let runtime = { runtime with
      environment = Environment.bind runtime.environment x v
    }
    in
    expression runtime e

  | FunCall (FunId "allocate_block", [size]) ->
    begin match value_as_int (expression runtime size) with
      | Some size ->
        let a = Memory.allocate runtime.memory size VUnit in
        VAddress a
      | None ->
        error [] "A block size should be an integer."
    end

  | (FunCall (FunId "read_block", [location; index])) as e ->
    begin match
        (value_as_address (expression runtime location),
         value_as_int (expression runtime index))
      with
        | Some location, Some index ->
           let block = Memory.dereference runtime.memory location in
           Memory.read block index
        | None, _ ->
          error [] (Printf.sprintf "Expecting a block while evaluating %s" (FopixPrettyPrinter.(to_string expression e)))
        | _, None ->
          error [] "Expecting an integer."
    end

  | FunCall (FunId "equal_string", [e1; e2]) ->
     begin match expression runtime e1, expression runtime e2 with
     | VString s1, VString s2 -> VBool (String.compare s1 s2 = 0)
     | _ -> assert false (* By typing. *)
     end

  | FunCall (FunId "equal_char", [e1; e2]) ->
     begin match expression runtime e1, expression runtime e2 with
     | VChar s1, VChar s2 -> VBool (Char.compare s1 s2 = 0)
     | _ -> assert false (* By typing. *)
     end

  | FunCall (FunId ("observe_int" | "print_int"), [e]) ->
     begin match expression runtime e with
     | VInt x ->
        ignore (print_string (Mint.to_string x));
        VUnit
     | _ -> assert false (* By typing. *)
     end

  | FunCall (FunId "print_string", [e]) ->
     begin match expression runtime e with
     | VString s -> print_string s
     | _ -> assert false (* By typing. *)
     end

  | FunCall (FunId "write_block", [location; index; e]) ->
    begin match
        (value_as_address (expression runtime location),
         value_as_int (expression runtime index))
      with
        | Some location, Some index ->
          let v = expression runtime e in
          let block = Memory.dereference runtime.memory location in
          Memory.write block index v;
          VUnit
        | None, _ ->
          error [] "Expecting a block."
        | _, None ->
          error [] "Expecting an integer."
    end

  | FunCall (FunId (("`&&`" | "`||`") as binop), [e1; e2]) ->
     begin match expression runtime e1, binop with
     | VBool true, "`&&`" | VBool false, "`||`" -> expression runtime e2
     | VBool false, "`&&`" -> VBool false
     | VBool true, "`||`" -> VBool true
     | _, _ -> assert false (* By typing. *)
     end

  | FunCall (FunId s, [e1; e2]) when is_binary_primitive s ->
    evaluation_of_binary_symbol runtime s e1 e2

  | FunCall (f, arguments) ->
    let vs = List.map (expression runtime) arguments in
    let (formals, body) = try
        List.assoc f runtime.functions
      with Not_found ->
        let FunId f = f in
        error [] (Printf.sprintf "Unbound function `%s'." f)
    in
    let runtime =
      { runtime with
        environment =
          Environment.(List.fold_left2 bind runtime.environment formals vs)
      }
    in
    expression runtime body

  | UnknownFunCall (e, arguments) ->
    begin match expression runtime e with
      | VFun f ->
        expression runtime (FunCall (f, arguments))
      | _ ->
        assert false (* By construction. *)
    end

and binop
: type a b.
  _ -> string ->
  a coercion -> b wrapper -> _ -> (a -> a -> b) -> _ -> _ -> value
= fun m kind coerce wrap runtime op l r ->
  let lv = expression runtime l
  and rv = expression runtime r in
  match coerce lv, coerce rv with
    | Some li, Some ri ->
      wrap (op li ri)
    | _, _ ->
      error [] (Printf.sprintf "Invalid %s binary operation between %s and %s."
                  kind (print_value m lv) (print_value m rv))

and arith_binop env =
  binop env.memory "arithmetic" value_as_int int_as_value env
and arith_cmpop env =
  binop env.memory "comparison" value_as_int bool_as_value env
and boolean_binop env =
  binop env.memory "boolean" value_as_bool bool_as_value env

and literal = function
  | LInt x -> VInt x
  | LString s -> VString s
  | LChar c -> VChar c
  | LFun f -> VFun f

and print_string s =
  output_string stdout s;
  flush stdout;
  VUnit

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
      substract Environment.initial runtime.environment runtime'.environment
  }

let print_observable runtime observation =
  Environment.print runtime.memory observation.new_environment
