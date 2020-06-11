(** This module implements the interpreter of the Retrolix programming
    language. *)

open Error
open RetrolixAST

let error msg =
  global_error "retrolix execution" msg

(** ----------------------- *)
(** {1 Runtime definition } *)
(** ----------------------- *)

(** This exception is raised to stop the machine. *)
exception ExitNow

type data =
  | DUnit
  | DInt   of Mint.t
  | DBool of bool
  | DString of string
  | DChar of char
  | DLocation of Memory.location
  | DFun of function_identifier

let print_data m data =
  let max_depth = 5 in
  let rec print_value d v =
    if d >= max_depth then "..."
    else match v with
      | DUnit -> "()"
      | DInt x -> Mint.to_string x
      | DLocation l -> print_block (d + 1) l
      | DFun (FId f) -> "@" ^ f
      | DBool true -> "true"
      | DBool false -> "false"
      | DChar c -> "'" ^ Char.escaped c ^ "'"
      | DString s -> "\"" ^ String.escaped s ^ "\""
  and print_block d a =
    let b = Memory.dereference m a in
    let vs = Array.to_list (Memory.array_of_block b) in
    "[ " ^ String.concat "; " (List.map (print_value d) vs) ^ " ]"
  in
  print_value 0 data

let type_of = function
  | DUnit -> "unit"
  | DInt _ -> "int"
  | DLocation _ -> "location"
  | DFun _ -> "function_ptr"
  | DChar _ -> "char"
  | DString _ -> "string"
  | DBool _ -> "bool"

let coercion_error expectation v =
  error ("Expecting " ^ expectation ^ " get " ^ type_of v)

let as_int  = function DInt x -> x   | v -> coercion_error "int" v
let as_loc  = function DLocation x -> x | v -> coercion_error "location" v
let as_fun  = function DFun f -> f | v -> coercion_error "function_ptr" v

let from_unit ()    = DUnit
let from_int x      = DInt x
let from_location x = DLocation x
let from_fid x      = DFun x

let is_intermediate (Id x) = (x.[0] = 'X')

module IdMap = Map.Make (struct
    type t = identifier
    let compare = compare
end)

module RIdMap = Map.Make (struct
    type t = register
    let compare = compare
end)

module FIdMap = Map.Make (struct
    type t = function_identifier
    let compare = compare
end)

type runtime = {
  gvariables     : data IdMap.t;
  lvariables     : data IdMap.t;
  registers      : data RIdMap.t;
  mutable memory : data Memory.t;
  functions      : function_definition FIdMap.t
}

and function_definition = {
  formals : identifier list;
  body : block;
}

type observable = {
  new_variables : data IdMap.t
}

let initial_runtime () = {
  gvariables = IdMap.empty;
  lvariables = IdMap.empty;
  registers  = RIdMap.empty;
  memory     = Memory.create (640 * 1024);
  functions  = FIdMap.empty;
}

let print_runtime runtime =
  let idmap m =
    String.concat "," (List.map (fun (Id s, v) ->
      Printf.sprintf "%s = %s" s (print_data runtime.memory v)
    ) (IdMap.bindings m))
  in
  let ridmap m =
    String.concat "," (List.map (fun (RId s, v) ->
      Printf.sprintf "%s = %s" s (print_data runtime.memory v)
    ) (RIdMap.bindings m))
  in
  let gvariables = idmap
  and lvariables = idmap
  and registers = ridmap
  in
  Printf.sprintf "\
  gvariables = %s\n\
  lvariables = %s\n\
  registers = %s\n\
"
    (gvariables runtime.gvariables)
    (lvariables runtime.lvariables)
    (registers runtime.registers)

(** -------------------------- *)
(** {1 Instruction execution } *)
(** -------------------------- *)

let evaluate runtime0 (ast : t) =
  let extract_function_definition runtime = function
    | DValues _ -> runtime
    | DFunction (f, formals, body) ->
      { runtime with functions =
          FIdMap.add f { formals; body } runtime.functions
      }
    | DExternalFunction _ ->
      runtime
  in
  let rec program runtime ds =
    let runtime = List.fold_left extract_function_definition runtime ds in
    List.fold_left definition runtime ds
  and definition runtime = function
    | DValues (xs, b) ->
       let runtime =
         { runtime with
           gvariables = List.fold_left
                          (fun gvariables x -> IdMap.add x DUnit gvariables)
                          runtime.gvariables
                          xs;
         }
       in
       block runtime b
    | DFunction (f, xs, b) ->
      runtime
    | DExternalFunction _ ->
      runtime
  and block runtime b =
    let jump_table = Hashtbl.create 13 in
    let rec make = function
      | [(l, i)] ->
        Hashtbl.add jump_table l (i, None)
      | (l, i) :: ((l', _) :: _ as is) ->
        Hashtbl.add jump_table l (i, Some l');
        make is
      | [] -> assert false
    in
    make (snd b);
    let locals0 = runtime.lvariables in
    let locals = fst b in
    let start_label = fst (List.hd (snd b)) in
    let start = Hashtbl.find jump_table start_label in
    let runtime =
      List.fold_left (fun r x ->
          bind_local r x (DInt Mint.zero)
        ) runtime locals
    in
    let runtime = instruction runtime jump_table start_label start in
    { runtime with lvariables = locals0 }

  and instruction runtime jump_table l (i, next) =
    let jump l runtime =
      if not (Hashtbl.mem jump_table l) then
        let Label l = l in
        failwith (Printf.sprintf "Label %s not found" l)
      else
        instruction runtime jump_table l (Hashtbl.find jump_table l)
    in
    let continue runtime =
      match next with
        | None -> runtime
        | Some l -> jump l runtime
    in
    match i with
      | Call (f, rs, _) ->
         call runtime (rvalue runtime f) (List.map (rvalue runtime) rs)
         |> continue
      | Ret ->
        runtime
      | Assign (x, o, rs) ->
        assign runtime x (op l runtime o (List.map (rvalue runtime) rs))
        |> continue
      | Jump l ->
        jump l runtime
      | ConditionalJump (c, rs, l1, l2) ->
        if condition l c (List.map (rvalue runtime) rs) then
          jump l1 runtime
        else
          jump l2 runtime
      | Comment _ ->
        continue runtime
      | Switch (r, ls, default) ->
        begin match rvalue runtime r with
          | DInt x ->
            let x = Mint.to_int x in
            if  x < Array.length ls then
              jump ls.(x) runtime
            else
              begin match default with
                | None -> failwith "Non exhaustive switch."
                | Some l -> jump l runtime
              end
          | _ ->
            assert false (* By typing. *)
        end
      | Exit ->
        runtime
  and rvalue runtime = function
    | `Variable x ->
      (try
         IdMap.find x runtime.lvariables
       with Not_found ->
         (try
            IdMap.find x runtime.gvariables
          with Not_found ->
            let Id x = x in
            failwith (Printf.sprintf "Variable %s not found" x)
         )
      )
    | `Register x ->
      (try
         RIdMap.find x runtime.registers
       with Not_found ->
         DInt Mint.zero
      )
    | `Immediate l ->
      literal l
  and op l runtime o vs =
    match o, vs with
      | Copy, [ v ] -> v
      | Add, [ DInt x; DInt y ] ->
        DInt (Mint.add x y)
      | Mul, [ DInt x; DInt y ] ->
        DInt (Mint.mul x y)
      | Div, [ DInt x; DInt y ] ->
        DInt (Mint.div x y)
      | Sub, [ DInt x; DInt y ] ->
        DInt (Mint.sub x y)
      | _, _ ->
        assert false

  and condition (Label l) op vs =
    match op, vs with
    | GT, [ DInt x1; DInt x2 ] -> x1 > x2
    | LT, [ DInt x1; DInt x2 ] -> x1 < x2
    | GTE, [ DInt x1; DInt x2 ] -> x1 >= x2
    | LTE, [ DInt x1; DInt x2 ] -> x1 <= x2
    | EQ, [ DInt x1; DInt x2 ] -> x1 = x2
    | _,  vs ->
       failwith (
           Printf.sprintf "Line %s: Invalid comparison with %s\n"
             l
             (String.concat " " (List.map type_of vs))
         )

  and literal = function
    | LInt x -> DInt x
    | LFun f -> DFun f
    | LString s -> DString s
    | LChar c -> DChar c

  and assign runtime lvalue v =
    match lvalue with
      | `Variable x ->
        if IdMap.mem x runtime.lvariables then
          { runtime with lvariables = IdMap.add x v runtime.lvariables }
        else if IdMap.mem x runtime.gvariables then
          { runtime with gvariables = IdMap.add x v runtime.gvariables }
        else failwith "Assignment to an unbound variable."
      | `Register x ->
        { runtime with registers = RIdMap.add x v runtime.registers }

  and call runtime fv vs =
    match fv with
      | DFun f ->
         (try
            let fdef = FIdMap.find f runtime.functions in
            let runtime = List.fold_left2 bind_local runtime fdef.formals vs in
            block runtime fdef.body
          with Not_found ->
            external_function runtime vs f
         )
      | _ ->
        assert false

  and external_function runtime vs (FId f) =
    let module Arch : Architecture.S = (val Options.get_architecture ()) in
    let mk_reg r = `Register (RId (Arch.string_of_register r)) in
    let return value runtime = Arch.(
      assign runtime (mk_reg return_register) value
    )
    in
    let vs = Arch.(
      List.(map (rvalue runtime) (map mk_reg argument_passing_registers))
    ) @ vs
    in
    match f, vs with
      | "allocate_block", (DInt size :: _) ->
         let addr = Memory.allocate runtime.memory size (DInt Mint.zero) in
         return (DLocation addr) runtime
      | "write_block", (DLocation location :: DInt i :: v :: _) ->
         let block = Memory.dereference runtime.memory location in
         Memory.write block i v;
         return DUnit runtime
      | "read_block", (DLocation location :: DInt i :: _) ->
         let block = Memory.dereference runtime.memory location in
         return (Memory.read block i) runtime
      | "equal_char", (DChar c1 :: DChar c2 :: _) ->
         return (DInt (Int64.of_int (if c1 = c2 then 1 else 0))) runtime
      | "equal_string", (DString s1 :: DString s2 :: _) ->
         return (DInt (Int64.of_int (if s1 = s2 then 1 else 0))) runtime
      | ("observe_int" | "print_int"), (DInt i :: _) ->
         print_string (Mint.to_string i);
         flush stdout;
         return DUnit runtime
      | "print_char", (DChar i :: _) ->
         print_char i;
         return DUnit runtime
      | "print_string", (DString i :: _) ->
         print_string i;
         return DUnit runtime
      | "add_eight_int",
        (DInt i1 :: DInt i2 :: DInt i3 :: DInt i4
         :: DInt i5 :: DInt i6 :: DInt i7 :: DInt i8 :: _) ->
         let r =
           List.fold_left Mint.add Mint.zero [i1; i2; i3; i4; i5; i6; i7; i8]
         in
         return (DInt r) runtime
      | _ ->
         Printf.eprintf
           "NoSuchFunction or InvalidApplication of `%s' \
            (%d argument(s) provided : %s)."
           f
           (List.length vs)
           (String.concat " " (List.map type_of vs));
         return DUnit runtime

  and bind_local runtime x v =
    { runtime with lvariables = IdMap.add x v runtime.lvariables }
  in
  let extract_observable runtime =
    { new_variables =
        IdMap.filter
          (fun x _ -> not (IdMap.mem x runtime0.gvariables
                           || is_intermediate x))
          runtime.gvariables
    }
  in
  let runtime = program runtime0 ast in
  let observable = extract_observable runtime in
  (runtime, observable)

let print_observable runtime obs =
  String.concat "\n" (List.map (fun (Id k, v) ->
    Printf.sprintf "%s = %s" k (print_data runtime.memory v)
  ) (IdMap.bindings obs.new_variables))
