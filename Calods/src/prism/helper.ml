open Types
open Compiler
open PrismAst

module S = Compiler.Ast

module T = PrismAst

(* Explode position *)
let explode x =
  Position.value x

let explodes x =
  List.map explode x

(* Translate helpers *)

let create_variable ~env ~name ~ty ~x ~id  =
  let name = name ^ id in
  let env = { env with
              vars = (name, ty) :: env.vars;
              globals = name :: env.globals
            } in
  let bounds = bounds_values_from_type env ty in
  (env, GlobalVar (name, bounds, x))

let create_variables_from_array env name var_type values =
  List.fold_left
    (fun (env, acc, i) x ->
       let i_str = string_of_int i in
       let (env', gs') = create_variable ~env:env ~name:name ~ty:var_type ~id:i_str ~x:x in
         (env', gs'::acc, i+1)
      ) (env, [], 0) values
      


(* Explode array into multiple variables *)
let translate_global_array env var_name var_type values =
  let name = "_" ^ var_name ^ "_case" in
  let rec explode_array = function
    | v::vs ->
      let v = value_to_int env v in
      v :: explode_array vs
    | [] -> []
   
  in
  let values = explode_array values in
  let (env, gs, _) =  create_variables_from_array env name var_type values in
  (env, gs)

(* Explode empty array into multiple variables *)
let translate_empty_array env (var_name:string) var_type var_size =
  let name = "_" ^ var_name ^ "_case"  in
  let v = bounds_values_from_type env var_type |> fst in
  let vs = List.init var_size (fun _ -> v) in
  let (env, gs, _) = create_variables_from_array env name var_type vs in
  (env, gs)
    
(* Bind types in the environement, associate every value to an integer *)
let bind_types_to_int array ty_assoc ty vs =
  let rec binds idx next = function 
  | [] -> next
  | x::xs ->
    Array.set array idx (x, next);
    binds (idx+1) (next+1) xs
  in
  let next = binds 0 ty_assoc.next_int vs in
  { next_int = next;
    types = (ty, array)::ty_assoc.types
  }
       

let literal env name = function
  | S.Value ({value=v; _}) ->
    begin
      try value_to_int env v |> string_of_int
      with
        NotValue ->
        if is_arg env name v then
          "_arg_" ^ name ^ "_" ^ v
        else v
    end
  | S.ArrayValue ({value=n; _}, {value=v; _}) ->
    "_" ^ n ^ "_case" ^ v

let rec rw_on_cmp env = function
  | S.And (c, c') | S.Or (c, c') ->
    rw_on_cmp env c || rw_on_cmp env c'
  | S.Equal ({value=v; _ }, {value=v'; _}) | S.NonEqual ({value=v; _}, {value=v';_}) ->
    let v = literal env "" v in
    let v' = literal env "" v' in
    is_global env v || is_global env v'
  | _ -> false


(* Counts how many distinct states we have in an instruction list *)
let distinct_state =
  let rec aux acc = function
    | [] -> 0
    | (T.Instruction (_, T.State (_, x, _), _, _))::xs ->
      if List.mem x acc then
        aux acc xs
      else
        1 + (aux (x::acc) xs)
    | _ -> assert false
  in
  (fun e -> aux [] e)


(* Return list of boolean declaration 'canGo' for every process *)
let can_go_process =
  List.fold_left
    (fun acc (S.Process (n, _, _, _)) ->
       T.GlobalVar ("_"^n^"_canGo", (0, 1), 0)::acc) []
    

let state ?(args=None) ~l ~id () =
  State (l, id, args)

let state_next ?(args=None) ?(next_id=None) ~l ~id () =
  let id = 
  match next_id with
  | Some id -> id
  | _ -> id
  in
  state ~l:l ~id:id ~args:args ()

let loop conf instrs =
  (* We make it loop on the last action *)
  let st = state ~l:conf.label ~id:conf.id () in
  let instrs = instrs @ [T.Instruction (None, st, T.IEmpty, Some st)] in
  instrs

(* Return 2 instructions
   if canGo = True then go next
   else loop
*)
let add_can_go conf =
  let can_go_true = T.Eq ("_" ^ conf.name^"_canGo", "1") in
  let st_true = state ~l:conf.label ~id:conf.id ~args:(Some can_go_true) () in
  let st_true_next = state ~l:conf.label ~id:(conf.id+1) () in

  ({conf with id=conf.id+1},  
  [
    T.Instruction (None, st_true, T.IEmpty, Some st_true_next) ;
  ])
  

(* Change last next state id to i *)
let change_next_id id last l =
  List.map
    (function
      | T.Instruction (l, T.State (n, i, s), is, Some (T.State (n', _, s'))) when i=(last-1)->
        T.Instruction (l, T.State (n, i, s), is, Some (T.State (n', id, s')))
      | x -> x
    ) l
        
(* Extract arguments of processes into global variables *)
let translate_arg name env = function
  | S.Arg({value=x;_}, {value=ty; _}) ->
    let bounds = bounds_values_from_type env ty in
    T.GlobalVar ("_arg_" ^ name ^ "_" ^x, bounds, fst bounds)

let extract_args env ps =
  List.fold_left
    (fun acc (S.Process (name, args, _, _)) ->
         List.map (fun x -> translate_arg name env x) (explodes args) @ acc
    ) [] ps
    
(* Translate a declaration into a local variable *)
and translate_declaration env = function
  | S.DeclareVar ({value=name; _}, {value=ty; _}) ->
    let env = { env with vars = (name, ty)::env.vars } in
    let bounds = bounds_values_from_type env ty in
    (env, T.LocalVar (name, bounds, fst bounds))

(* Translate declarations and arguments into T.local variables *)
let locals_variables_from_process env name decls =
  let locals_variables name =
    [ T.LocalVar ("_active_"^name, (0,1), 0);
      T.LocalVar ("_decide_"^name, (0, how_many_types env.ty_assoc.types), 0)      
    ]
  in
  let iterate_on_objects env f =
    List.fold_left
      (fun (env, decls) x ->
         let env', decl = f env x in
         (env', decl::decls)
      )
      (env, [])
  in
  (* Translate every S.local variables *)
  let (env, locals) = iterate_on_objects env (translate_declaration) (explodes decls) in
  
  (env, locals_variables name @ locals)

