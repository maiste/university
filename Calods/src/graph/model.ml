(*
 * Chaboche - Marais
 * CALODS - 2019
 *)


open Compiler.Environment
module Pid = Pid.Pids


(** Manage nodes name  **)
let generator = ref 0
let graph_generator = ref 0

let fresh_name () =
  let name = "node" ^ (string_of_int !generator) in
  generator := !generator + 1;
  name

let fresh_graph_name () =
  let name = "graph_" ^ (string_of_int !graph_generator) ^".dot" in
  graph_generator := !graph_generator + 1;
  name


(** Manage Graph **)
type node = {
  proc_state: (string*string) list ;
  global_state: (string*string) list ;
  local_state: (string*string*string) list ;
  decide_state: (string*string) list;
}

type proc_name = string
type vertex = (proc_name * node * node)

type graph = {
  nodes : (node, node) Hashtbl.t ;
  vertexes : (vertex, vertex) Hashtbl.t ;
}


(** Create graph *)
let create_graph n =
  {
    nodes = Hashtbl.create n ;
    vertexes = Hashtbl.create (2*n) ;
}

let create_node () =
  {
    proc_state = [] ;
    global_state = [] ;
    local_state = [] ;
    decide_state = [] ;
  }

(* Converter an environment into a node *)
let env_to_node env =
  let n = create_node () in
  let convert_proc n =
    StrMap.fold (
      fun p l n ->
        { n with proc_state = (p,string_of_int l)::n.proc_state }
    ) env.proc_lines n
  in
  let convert_global n =
    StrMap.fold (
    fun id v n ->
        let v = tuple_of_variable v in
        { n with global_state = (id, v)::n.global_state }
    ) env.global_vars n
  in
  let convert_local n =
    PairMap.fold (
      fun (p, id) v n ->
        let v = tuple_of_variable v in
        { n with local_state = (p,id,v)::n.local_state }
    ) env.local_vars n
  in
  let convert_decide n =
    StrMap.fold (
      fun p v n ->
        { n with decide_state = (p,v)::n.decide_state }
    ) env.decide_vars n
  in
   convert_proc n
  |> convert_global
  |> convert_local
  |> convert_decide


(** Insert function
  * Return true if the node or the vertex already
  * exists
  *)
let insert_node g n =
  if Hashtbl.mem g.nodes n then g
  else
    begin
      Hashtbl.add g.nodes n n ;
      g
    end

let insert g v =
  let _, n1, n2 = v in
  let g = insert_node g n1 in
  let g = insert_node g n2 in
  if Hashtbl.mem g.vertexes v then g
  else
    begin
      Hashtbl.add g.vertexes v v ;
      g
    end

let is_node_in g n = Hashtbl.mem g.nodes n

let get_proc state =
  if List.length state <> 0 then
  "<TR><TD>" ^ (
    List.fold_left (
      fun acc (proc, state) -> acc^proc^":"^state^" "
      ) "" state
    )
  ^ "</TD></TR>\n"
  else ""

let get_globals state =
  if List.length state <> 0 then
  "<TR><TD>" ^ (
    List.fold_left (
      fun acc (name, value) -> acc^name^":"^value^" "
      ) "" state
    )
  ^ "</TD></TR>\n"
  else ""

let get_locals state =
  if List.length state <> 0 then
  "<TR><TD>" ^ (
    List.fold_left (
      fun acc (proc, name, value) ->
        acc ^ proc ^ "(" ^ name ^ "):"^value^" "
    ) "" state
    )
  ^ "</TD></TR>\n"
  else ""

let get_decide state =
  if List.length state <> 0 then
  "<TR><TD>" ^ (
    List.fold_left (
      fun acc (proc, value) ->
        acc ^ proc ^ ":"^value^" "
      ) "" state
    )
  ^ "</TD></TR>\n"
  else ""

let node_to_dot node_table (n : node) =
  let name = fresh_name () in
  Hashtbl.add node_table n name;
  let proc_state = get_proc n.proc_state in
  let global_state = get_globals n.global_state in
  let local_vars = get_locals n.local_state in
  let decide_vars = get_decide n.decide_state in
  name
    ^ " [shape=none, margin=0, label=<\n"
    ^ "<TABLE BORDER=\"0\" CELLBORDER=\"1\">"
    ^ proc_state ^ global_state ^ local_vars ^ decide_vars
    ^ "</TABLE>>]\n"

let write_nodes g channel =
  let node_table = Hashtbl.create 2048 in
  Hashtbl.iter (
    fun _ value ->
      node_to_dot node_table value
        |> output_string channel
  ) g.nodes ;
  node_table

let write_vertexes node_table g channel =
  Hashtbl.iter (
    fun _ (p, n1, n2) ->
      let start = Hashtbl.find node_table n1 in
      let stop = Hashtbl.find node_table n2 in
      start ^ " -> " ^ stop ^ "[label=\"" ^ p ^ "\"]\n"
        |> output_string channel 
    ) g.vertexes

let export_to_graphviz g s =
  let channel = open_out s in
  let _ = output_string channel "digraph Calods {\n" in
  let node_table = write_nodes g channel in
  begin
    write_vertexes node_table g channel ;
      output_string channel "}\n" ;
      close_out channel
  end
