(*
 * Chaboche - Marais
 * CALODS - 2019
 *)


(** Modules **)

open IlodsAst
module Pid = Pid.Pids
module Env = Compiler.Environment
module G = Model




(** Utils **)

let find_proc ps p =
  List.find (
      fun (n,_,_) -> p = n
  ) ps

let keep_proc c ps =
  match c with
  | Call (n, _) -> find_proc ps n
  | _ -> assert false (* By construction *)

let procs_called pid ps cs =
  List.fold_right (
      fun c acc ->
        let (n,a,i) = keep_proc c ps in
        (Pid.new_pid pid n ,a,i)::acc
    ) cs []

let isolate_call c =
  match c with
  | Call (n,ls) -> (n, ls)
  | _ -> assert false

let find_call pid cs p =
  let p = Pid.get_pid pid p in
  List.find (
    fun c ->
      let (n,_) = isolate_call c in
      n = p
  ) cs

let var_to_val = function
  | Env.EVariable v -> v
  | _ -> assert false (*By construction *)

let eval_lit env p l =
  let l =
  match l with
  | Value v -> Env.get_variable_of env p v None
  | ArrayValue (n,v) -> Env.get_variable_of env p n (Some v)
  in
  match l with
  | Some v -> v
  | None -> assert false (* By checking *)

let insert_node env g =
  let g' = G.insert_node !g (G.env_to_node env) in
  g := g'

let insert_vertex g p e1 e2 =
  let n1 = G.env_to_node e1 in
  let n2 = G.env_to_node e2 in
  let g' = G.insert !g (p,n1,n2) in
  g := g'

let not_inside c q g  =
  let not_visit =
    Queue.fold (
      fun b elt -> (c <> elt) && b
    ) true q
  in
  let not_visited = not (G.is_node_in (!g) (G.env_to_node c)) in
  not_visit && not_visited



(** Setup **)

let update_globals env g =
  List.fold_left (
    fun env e ->
      match e with
      | EmptyArray (n,v) ->
          let v = Env.empty_array (int_of_string v) in
          Env.add_global_var env n v
      | Array (n,v) ->
          let v = Env.array_of v in
          Env.add_global_var env n v
      | GlobalVar (n, v) ->
          let v = Env.var_of v in
          Env.add_global_var env n v
  ) env g

let rec update_locals_of env p is =
  let line = Env.get_line env p in
  match is.(line) with
  | Declare n, Next ->
      let env = Env.add_local_var env p n (Env.EVariable "") in
      let env = Env.move_to_next env p in
      update_locals_of env p is
  | _ -> env

let update_locals env ps =
  List.fold_left (
      fun env (p,_, is) ->
        update_locals_of env p is
  ) env ps

let update_args_of env p args ls =
  List.fold_left2 (
    fun env a l ->
      let l = eval_lit env p l in
      Env.add_local_var env p a l
  ) env args ls

let update_args pid env ps cs =
  List.fold_left (
    fun env (p,args,_) ->
      let (_, ls) = find_call pid cs p |> isolate_call in
      update_args_of env p args ls
    ) env ps



(** Environment **)

let verbose_mode v c =
  if v then Env.display c
  else ()

let init_line env ps =
  List.fold_right (
      fun (n,_,_) env ->
      Env.update_proc_line env n 0
    ) ps env 

let generate_env pid g ps cs =
  let env = Env.empty_env in
  let env = init_line env ps in
  let env = update_globals env g in
  let env = update_args pid env ps cs in
  update_locals env ps


(** Execution **)

let rec eval_compare env p c =
  match c with
  | And (c1, c2) ->
      let c1 = eval_compare env p c1 in
      let c2 = eval_compare env p c2 in
      c1 && c2
  | Or (c1, c2) ->
      let c1 = eval_compare env p c1 in
      let c2 = eval_compare env p c2 in
      c1 || c2
  | Equal (l1, l2) ->
      let l1 = eval_lit env p l1 in
      let l2 = eval_lit env p l2 in
      l1 = l2
  | NonEqual (l1, l2) ->
      let l1 = eval_lit env p l1 in
      let l2 = eval_lit env p l2 in
      l1 <> l2
  | Bool b -> b


let eval_proc env p is =
  let line = Env.get_line env p in
  match is.(line) with
  | Assign (n, l), Next ->
      let v = eval_lit env p l in
      let env = Env.update_variable_of env p n v in
      Some (Env.move_to_next env p)
  | AssignArray (n,pos, l), Next ->
      let v = eval_lit env p l |> var_to_val in
      let env = Env.update_array_of env p n pos v in
      Some(Env.move_to_next env p)
  | Decide (l), Finish ->
      let v = eval_lit env p l |> var_to_val in
      Some (Env.decide_proc env p v)
  | Jump (c, Next), Goto i ->
      if eval_compare env p c then
        Some (Env.move_to_next env p)
      else
        Some (Env.update_proc_line env p i)
  | Move, Goto i ->
      Some (Env.update_proc_line env p i)
  | Empty, EOF ->
      None
  | _ -> assert false

let rec eval_main = function
  | Parallel cs -> [cs]
  | Call _ as c -> [[c]]
  | SeqI cs ->
     List.fold_right (
         fun e acc ->
           let call = eval_main e in
           call @ acc
       ) cs []
  | _ -> assert false (* By construction *)



(** Generator **)

let one_step env p is =
  if (Env.has_decide env p) then Some (env)
  else
      eval_proc env p is

let successor env ps =
  let l = Queue.create () in
  List.iter (
      fun (p,_, is) ->
      match one_step env p is with
        | None -> ()
        | Some e -> Queue.push (p,e) l
  ) ps ; l

let exec_one_conf verbose g ps cs  =
  let pid = Pid.create () in
  let graph = ref (G.create_graph 2048) in
  let ps = procs_called pid ps cs in
  let env = generate_env pid g ps cs in
  let toVisit = Queue.create () in
  Queue.push env toVisit;
  while (Queue.is_empty toVisit) |> not do
    let c = Queue.pop toVisit in
    verbose_mode verbose c;
    insert_node c graph;
    let suc = successor c ps in
    while (Queue.is_empty suc) |> not do
      let p, c' = Queue.pop suc in
      if not_inside c' toVisit graph then
        Queue.push c' toVisit
        else ();
        insert_vertex graph p c c'
    done;
  done ; !graph


let generate_all verbose ((_,g), ps, m) =
  let cs =  eval_main m in
  List.iter (
      fun c ->
        let graph = exec_one_conf verbose g ps c in
        G.fresh_graph_name ()
        |>  G.export_to_graphviz graph
    ) cs
