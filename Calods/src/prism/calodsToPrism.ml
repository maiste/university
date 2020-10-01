(*
 * Chaboche - Marais
 * CALODS - 2019
 *)

open Types
open Helper
open Instruction
open Callable
open Properties

(* Source Ast *)
module S = Compiler.Ast

(* Target Ast *)
module T = PrismAst

(* Automata *)
module Sch = Automata.Nba.Scheduler
module State = Automata.State.S
module Transition = Automata.Transition.T
  
let double =
  ref true

(* Basic environment *)
let empty_types = {
  types = [];
  next_int = 0;
}

let empty_env = {
  vars = [];
  globals = [];
  args = [];
  ty_assoc = empty_types;
}

(* Translate Calods.Ast to Prism.Ast *)
let rec translate_calods =
  let translate_program env = function
    | S.Program (h, ps, main, sch, props) ->
      let can_go = can_go_process ps in
      let (env, globals) = translate_header env h in
      let ps' = List.fold_left
          (fun acc x ->
             let proc = translate_process env x in
             proc::acc
          ) [] ps
      in
      let globals = globals @ extract_args env ps  in
      let main = translate_main env ps main in
      let sch = translate_scheduler env sch in
      let pps = properties env ps ps' main props in
      (env, T.Program (globals@can_go, ps', main, sch, pps))
  in
  let env = empty_env in
  (fun e -> translate_program env e)

and translate_header env = function
  | S.Header (datas, gs) ->
    let gs = match gs with | Some gs -> gs | None -> [] in
    let env = List.fold_left
        (fun env x -> translate_data env x)
        env (explodes datas) in
    let (env, gs) = List.fold_left
        (fun (env, acc) x ->
           let (env', gs') = translate_global env x in (env', gs'@acc))
        (env, []) (explodes gs) in
    (env, gs)

and translate_global env = function
  | S.Array (name, {value=ty; _}, vs) ->
    translate_global_array env name ty (explodes vs)
  | S.EmptyArray (name, {value=ty; _}, {value=i; _}) ->
    translate_empty_array env name ty (int_of_string i)
  | S.GlobalVar (name, {value=ty; _}, v) ->
    let v = value_to_int env (explode v) in
    let (env, gl) = create_variable ~env:env ~name:name ~ty:ty ~x:v ~id:"" in
    (env, [gl])

and translate_data env = function
  | S.DefineType (ty, vs) ->
    let array = Array.make (List.length vs) ("", 0) in
    { env with
      ty_assoc = bind_types_to_int array env.ty_assoc ty (explodes vs)
    }

and translate_process env =
  let translate env = function
    | S.Process (name, args, decls, instrs) ->
      let conf = config ~name:name ~label:name ~env:env ~d:!double () in
      let env = 
        { env with
          args = (name,
                  List.map (fun (S.Arg ({value=v; _}, _)) -> v ) (explodes args)
                 ) :: env.args
        }
      in
      let (env, decls) = locals_variables_from_process env name decls in
      let (conf, can_go) = add_can_go conf in
      let (_, instrs) = List.fold_left
          (fun (conf, acc) x ->
             let (conf', acc') = translate_instruction env conf x in
             (conf', acc@acc')
          ) (conf, []) (explodes instrs) in

      let instrs = can_go @ instrs in
      T.Process (name, None, decls, instrs)
  in
  (fun e -> translate env e)


and translate_main env ps m = match m with
  | S.Main ({value=c; _}) ->
    let name = "Init" in
    let id = "i" in
    let conf = config ~name:name ~label:id ~env:env ~d:false () in
    let (conf, forall_vars) = translate_callable env conf c in
    let decls = extract_forall_vars env m in
    let (conf, set_args) = translate_call env conf ps c in
    let (allows_ps) = let_go_ps conf ps in
    T.Init (name, Some id, decls, forall_vars @ set_args @ allows_ps)

and translate_callable env conf = function
  | S.ForAll (names, {value=ty;_}, _) ->
    translate_forall env conf ty names
  | _ ->
    conf, []

and translate_forall env conf ty names =
  let vs = values_of_ty env ty |> Array.to_list |> List.map (fun (x,_) -> value_to_int env x) in

  let (conf, sets) =
    List.fold_left
      (fun (conf, acc) x ->
         let (conf', sets) = set_variable_foreach conf x vs in
         (conf', acc@sets)
      ) (conf, []) names
  in
  (conf, sets)

and translate_scheduler _env = function
  | None -> None
  | Some sch ->
    (* Module will start at id = 0 *)
    let l = "id" in
    let x = 0 in

    let transitions = Sch.get_transitions sch in
    let initials = Sch.get_initials sch in

    let to_initials = List.fold_right
        (fun st acc ->
           let st = State.get st in
           let curr = state ~l:l ~id:x () in
           let next = state ~l:l ~id:(st+1) () in
           T.Instruction (None, curr, IEmpty, Some next) :: acc
        ) initials []
    in

    let to_transitions = List.fold_right
        (fun ts acc ->
           let st1, st2, proc = Transition.get ts in
           let x = State.get st1 in
           let y = State.get st2 in
           let curr = state ~l:l ~id:(x+1) () in
           let next = state ~l:l ~id:(y+1) () in
           T.Instruction (Some proc, curr, IEmpty, Some next) :: acc
        ) transitions []
    in
    Some (Scheduler ("Scheduler", Some l, [], to_initials @ to_transitions))
  
