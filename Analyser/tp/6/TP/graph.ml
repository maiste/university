open Whilennh
open Auxiliary

(*
  Define a set to represente nodes where a node is represented
  by its label.
*)
module Node = Set.Make(struct
  type t = int
  let compare = compare
end)

(*
  Define a set to represente edges between nodes. An edge is a tuple
  between to label.
*)
module Edge = Set.Make(struct
  type t = int * int
  let compare = compare
end)

(*
  Map to store the association between a label and the code
  associated with.
*)
module Code = Map.Make(struct
  type t = int
  let compare = compare
end)



(*
  Type to store the graph while going through the flow.
*)
type env = {
  nodes : Node.t ;
  edges : Edge.t;
}

(*
  Create a virgin env.
*)
let init_env = {
  nodes = Node.empty ;
  edges = Edge.empty ;
}

(*
  Initialize a virgin code set.
*)
let init_code =
  Code.empty




(*
  Compute the union of the map c1 and c2 with the collision
  function which is supposed to not be called.
*)
let code_union c1 c2 =
  Code.union (
    fun _ v _ -> (Some v)
  ) c1 c2

(*
  Get a PP code of the statement associated with a label.
*)
let rec get_code code stm =
  let sentence = string_of_stm ~code:true stm in
  match stm with
  | Skip l | Ass (_, _, l) ->
      Code.add l sentence code
  | While (_, l, s) ->
      let code = Code.add l sentence code in
      get_code code s
  | If (_, l, s1, s2) ->
      let code = Code.add l sentence code in
      let c1 = get_code code s1 in
      let c2 = get_code code s2 in
      code_union c1 c2
  | Seq (s1, s2) ->
      let c1 = get_code code s1 in
      let c2 = get_code code s2 in
      code_union c1 c2
  | Call _ | Bind _ -> assert false



(*
  Fill the environment with the statement from the program.
*)
let build_env (Program(_, stm)) =
  let flow = flow_stm stm |> Flow.elements in
  let env = init_env in
  let code = init_code in
  let code = get_code code stm in
  let rec build_env_aux env = function
    | [] -> env
    | ((src, dst) as x) :: xs ->
        let env = {env with nodes = Node.add src env.nodes} in
        let env = {env with nodes = Node.add dst env.nodes} in
        let env = {env with  edges = Edge.add x env.edges } in
        build_env_aux env xs
  in
  (code, build_env_aux env flow)

(*
  Export to graphviz file.
*)
let export_env file code env =
  let intro = "digraph Flow" ^ file ^ "{\n" in
  let outro = "}\n" in
  let file = file ^ ".dot" in
  let chan = open_out file in
  let write_node =
    fun n -> (
      let label = Code.find n code in
      let label = (string_of_int n) ^ "[label=\"" ^  label ^ "\"];\n" in
      output_string chan label
    )
  in
  let write_edge =
    fun (src, dst) ->
      output_string chan (string_of_int src ^ "->" ^ string_of_int dst ^ ";\n")
  in
  begin
    output_string chan intro ;
    Node.iter write_node env.nodes;
    Edge.iter write_edge env.edges;
    output_string chan outro ;
    close_out chan
  end

(*
  Build the environment and export to graphviz.
*)
let dot_from_program file p =
  let (code, env) = build_env p in
  export_env file code env
