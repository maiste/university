(**
 * Manage graph representation and problem transformation
 * CHABOCHE - MARAIS
 *)

open Expression
open Regexp



(** Signature to define a graph *)
module type GRAPH =
sig
  module VSet : Set.S
  module EMap : Map.S
  type t
  val create : unit -> t
  val add_vertex : VSet.elt -> t -> t
  val add_edge : (EMap.key * VSet.elt * Tag.t) -> t -> t
  val add_vertexes : VSet.elt list -> t -> t
  val add_edges : (EMap.key * VSet.elt * Tag.t) list -> t -> t
  val print : (EMap.key -> (VSet.elt * Tag.t) list -> unit) -> t -> unit
  val get_vertex_neigh : EMap.key -> t -> (VSet.elt * Tag.t) list
  val get_opt_edge : EMap.key -> VSet.elt -> t -> (EMap.key * VSet.elt * Tag.t) option
  val get_vertexes : t -> VSet.t
end

(** Fonctor that builds a graph from an Edge Map and a Vertex Set *)
module Make (VSet : Set.S) (EMap : Map.S) :
GRAPH with module VSet = VSet and module EMap = EMap =
struct
  module VSet = VSet
  module EMap = EMap

  type t = VSet.t * ((VSet.elt* Tag.t) list) EMap.t

  let create () = (VSet.empty, EMap.empty)

  let add_vertex v (vs,es) = (VSet.add v vs, es)

  let add_edge (v1, v2, tag) (vs, es) =
    let edges =
      try
       (v2,tag)::(EMap.find v1 es)
      with Not_found ->  [(v2, tag)]
    in (vs, EMap.add v1 edges es)

  let add_vertexes vs' (vs, es) = (VSet.of_list vs' |> VSet.union vs, es)

  let add_edges es g =
    List.fold_left (fun acc e -> add_edge e acc) g es

  let print printer ((_, es) : t) =
    EMap.iter (fun node edge -> printer node edge) es

  let get_vertex_neigh v ((_,es) : t) =
    try
      EMap.find v es
    with Not_found -> []

  let get_opt_edge v1 v2 (_,es) =
    try
      Some (v1, v2, EMap.find v1 es |> List.assoc v2)
    with Not_found -> None

  let get_vertexes (vs, _) = vs
end

(** Define vertexes as a set of int *)
module VSet = Set.Make(struct
  type t = int
  let compare = compare
end)

(** Define an edge as a map of int list*)
module EMap = Map.Make(struct
  type t = VSet.elt
  let compare = compare
end)

(** Define a Graph type *)
module Graph = Make(VSet)(EMap)



(** Signature for the problem solver *)
module type PROBLEM = sig
  type graph
  val build : string list -> string -> string -> graph
  val intersect : graph -> graph ->  graph
  val print_graph : graph -> unit
  val extract_program :  graph -> graph -> expression list
  val extract : (string * string) list -> expression list
end

(** Module to solve problems *)
module Problem : PROBLEM with type graph = Graph.t =
struct
  type graph = Graph.t

  (** Generate all vertexes from a string [s] *)
  let gen_from_str s =
    let size = String.length s in
    let rec aux pos =
      if pos <= size then
        pos :: (aux (pos+1))
      else []
    in aux 0

  let const o v1 v2 =
    let start = v1 in
    let len = v2 - start in
    let s = String.sub o start len in
    Expr.const s

  (** Tell if a string [sub] of size [len] is equal to
      a part of [str] *)
  let is_sub len str sub =
    let str = String.sub str 0 len in
    if str = sub then true
    else false

  (** Return the forward and the backward of a position *)
  let pos_in_pos pos global_len len =
    let p1 =
      Pos_Expression.empty
      |> Pos_Expression.add (P.forward pos)
      |> Pos_Expression.add (P.backward (global_len-pos))
    in
    let p2 =
      Pos_Expression.empty
      |> Pos_Expression.add (P.forward (pos+len))
      |> Pos_Expression.add (P.backward (global_len-(pos+len)))
    in
    (p1, p2)

  let start k = function
    | `Forward -> 0
    | `Backward -> k

  let stop global_len i k = function
    | `Forward -> k >= i
    | `Backward -> not (k <= global_len)

  let get i k = function
    | `Forward -> (k, i)
    | `Backward -> (i, k)

  (** Add all expression until the end *)
  let fold_until global_len sens f pos acc =
    let rec aux k acc =
      if stop global_len pos k sens then acc
      else
        if pos >= 0 && pos < global_len then
          let x = RE.get (get pos k sens) in
          let acc = List.fold_left f acc x
          in aux (k + 1) acc
        else acc
    in
    aux (start pos sens) acc

  (** Add in [p1] and [p2] the regex that refers to [i] and [j]
      thanks to mem *)
  let pos_in_regex global_len (mem1, mem2) (p1, p2) i j =
    let add_if_ok pos p (re, beg, aft) =
      let p = if pos = beg then Pos_Expression.add (P.before re) p else p in
      if pos = aft then Pos_Expression.add (P.after re) p else p
    in
    let add_all_elements (p1, p2) elt =
      let p1 = add_if_ok i p1 elt in
      let p2 = add_if_ok j p2 elt in
      (p1, p2)
    in
    let p1 =
      if Hashtbl.mem mem1 i then Hashtbl.find mem1 i
      else
        let res = fold_until global_len `Forward (add_if_ok i) i p1 in
        let res = fold_until global_len `Backward (add_if_ok i) i res in
        Hashtbl.add mem1 i res; res
    in
    let p2 =
      if Hashtbl.mem mem2 j then Hashtbl.find mem2 j
      else
        let res = fold_until global_len `Forward (add_if_ok j) j p2 in
        let res = fold_until global_len `Backward (add_if_ok j) j res in
        Hashtbl.add mem2 j res; res
    in
    List.fold_left add_all_elements (p1,p2) (RE.get (i, j))

  (** Iter on a string [str] of global size [global_len]. Try
    to fill [acc] with by extracting of [sub] string in [str] *)
  let rec fold mem acc global_len sub_len pos str sub =
    let str_len = String.length str in
    if str_len >= sub_len then
      let str, pos, acc =
        match is_sub sub_len str sub with
        | true ->
            let p1_2 = pos_in_pos pos global_len sub_len in
            let p1, p2 = pos_in_regex  global_len mem p1_2 pos (pos+sub_len) in
            (String.sub str (sub_len) (str_len-sub_len)),
            (pos+sub_len),
            (Tag.add (Expr.extract p1 p2) acc)
        | false ->
          (String.sub str 1 (str_len-1)),
          (pos+1),
          acc
      in fold mem acc global_len sub_len pos str sub
    else acc

  (** Fill the edge list [acc] with an edge from [v1] to [v2]
      using a memory [mem] to reduce computing *)
  let fill mem acc i o v1 v2 =
    let global_len = String.length i in
    let sub_str = String.sub o v1 (v2 - v1) in
    let sub_len = String.length sub_str in
    let tag =
      fold mem Tag.empty global_len sub_len 0 i sub_str
      |> Tag.add (const o v1 v2)
    in (v1,v2,tag)::acc

  (** Annotate graph edges with [fill] function *)
  let annotations i o vertexes =
    let mem = (Hashtbl.create 1024, Hashtbl.create 1024) in
    let rec aux acc = function
      | [] -> acc
      | v1::vs ->
          let acc =
            List.fold_left (fun acc v2 ->
              if compare v1 v2 >= 0 then acc
              else fill mem acc i o v1 v2
            ) acc vs
          in aux acc vs
    in aux [] vertexes

  (** Build a DAG to represente [i] to [o] and use a [context] *)
  let build context i o =
    let vertexes = gen_from_str o in
    let _ = RE.init i context in
    let e = annotations i o vertexes in
    Graph.create ()
    |> Graph.add_vertexes vertexes
    |> Graph.add_edges e

  module IV = Map.Make(struct
      type t = int
      let compare = compare
    end)

  (** Product a map and a vertex list from to vertex list [vs1] [vs2].
      Each node in the new list refers to a tuple in m *)
  let product_vertex vs1 vs2 =
    let t = (Graph.VSet.cardinal vs2) - 1 in
    let node x y (m, acc) =
      let n = x*(t+1)+y in
      let acc = n::acc in
      let m = IV.add n (x,y) m in
      (m, acc)
    in
    let m = IV.empty in
    Graph.VSet.fold
      (fun x (m, acc) ->
        Graph.VSet.fold (node x) vs2 (m, acc)
      ) vs1 (m, [])

  (** Intersect of two edges *)
  let intersect_edge s1 s2 e1 e2 =
    let (_, _, t1) = e1 in
    let (_, _, t2) = e2 in
    let inter_expr exp1 exp2 acc =
      match exp1, exp2 with
      | Const w, Const w' when w=w'->
        Tag.add (Const w) acc
      | Extract (p1, q1), Extract (p2, q2) ->
        let p = Pos_Expression.inter p1 p2 in
        let p' = Pos_Expression.inter q1 q2 in
        if Pos_Expression.cardinal p > 0 && Pos_Expression.cardinal p' > 0 then
          Tag.add (Extract (p, p')) acc
        else acc
      | _ -> acc
    in
    let tag =
      Tag.fold
        (fun exp acc ->
           Tag.fold (inter_expr exp) t2 acc
        ) t1 Tag.empty
    in
    if Tag.cardinal tag > 0 then Some (s1, s2, tag)
    else None

  (** Check if there's an edge from v1 -> v2 and v1' -> v2' *)
  let add_edge m s1 s2 g1 g2 =
    let find x y g =
      Graph.get_opt_edge x y g
    in
    let (i1,i2) = IV.find s1 m in
    let (j1,j2) = IV.find s2 m in
    match (find i1 j1 g1, find i2 j2 g2) with
    | Some e1, Some e2 ->
      intersect_edge s1 s2 e1 e2
    | _ -> None

  (** Returns the right side node from the edges *)
  let get_nodes_from_edges es =
    List.fold_right
      (fun x acc -> match x with
         | (_, y, _) ->
           y :: acc
      ) es []

  let remove_colored colored next =
    let f x = VSet.mem x colored in
    List.fold_left
       (fun acc x -> if f x then acc else x :: acc) [] next

  let add_color colored to_add =
    List.fold_left
      (fun colored x -> VSet.add x colored) colored to_add

  let intersect_edges m vs g1 g2 =
    let edges_v_to_vi from acc v_to =
      match add_edge m from v_to g1 g2 with
      | Some x -> x :: acc
      | None -> acc
    in
    let rec aux colored acc = function
      | [] -> acc
      | l ->
        let es =
          (* For every v from l:
               For every v' from vs:
                 add edges (v -> v') to the acc
          *)
          List.fold_left
            (fun acc from ->
               (List.fold_left (edges_v_to_vi from) [] vs) @ acc
            ) [] l
        in
        let next = get_nodes_from_edges es in
        let next = remove_colored colored next in
        aux (add_color colored next) (es@acc) next
    in
    let puit = [0] in
    let colored = VSet.(empty |> add 0) in
    aux colored [] puit

  (** Return the intersection of [g1] and [g2] *)
  let intersect g1 g2 =
    let vs = Graph.get_vertexes g1 in
    let vs' = Graph.get_vertexes g2 in
    (* Map with 0 -> (0, 0), and the vertexes *)
    let (m, vs) = product_vertex vs vs' in
    let es = intersect_edges m vs g1 g2 in
    Graph.create ()
    |> Graph.add_vertexes vs
    |> Graph.add_edges es


  let print_graph g =
    let printer v1 edges =
      List.iter (
        fun (v2, tag) ->
          Printf.printf "(%d)" v1;
          Printf.printf " -> ";
          Printf.printf "(%d)" v2;
          Printf.printf " -> ";
          Tag.iter (fun e -> Expr.print e) tag;
          Printf.printf "\n";
      ) (List.rev edges)
    in
    Graph.print printer g

  (** Keep the simpliest expression for each edge in [g] *)
  let choose_tags mem g =
    (* Return top value from e1, e2 (simplest value) *)
    let top (e1:expression) (e2:expression) : (expression * float) =
      match e1, e2 with
      | _, Const _ -> (e2, 5.0)
      | _ -> (e1, 10.0)
    in
    let choose_tag v1 v2 tag =
      let pick_expr t =
        Tag.find_first
          (function
            | Const _ -> true
            | Extract (p1, p2) ->
              Pos_Expression.cardinal p1 > 0 &&
              Pos_Expression.cardinal p2 > 0
          ) t
      in
      (* default value *)
      let tw = (pick_expr tag, 0.0) in
      (* search top value in the set *)
      let (t, w) = Tag.fold (fun e (t,_) -> top t e) tag tw in
      (v1, v2, (Tag.singleton t, w))
    in
    Graph.VSet.iter
      (fun v1 ->
        List.iter (
          fun (v2, e) ->
            let (v1, v2, (t, w)) = choose_tag v1 v2 e in
            let l = Hashtbl.find mem v1 in
            Hashtbl.add mem v1 ((v2, t, w)::l)
        ) (Graph.get_vertex_neigh v1 g)
      ) (Graph.get_vertexes g)

  let build' strs (i,o) =
     build strs i o

  (** Init the memory for Dikjstra *)
  let init_mem mem g =
    let add mem x = Hashtbl.add mem x [] in
    Graph.VSet.iter (add mem) (Graph.get_vertexes g)

  (** Extract the expression from the graph [g] *)
  let extract_expression g =
    let mem = Hashtbl.create 256 in
    init_mem mem g;
    choose_tags mem g;
    Dijkstra.dijkstra (Graph.VSet.elements (Graph.get_vertexes g)) mem

  (** Extract an expression from the intersection of [g1] and [g2] *)
  let extract_program g1 g2 =
    let graph = intersect g1 g2 in
    extract_expression graph

  let print_problem lst =
    let print (x, y) = Printf.printf "%s\t%s\n" x y in
    List.iter print lst;
    Printf.printf "--------\n\n"

  (** Extract an expression from a [wording] *)
  let extract wording =
    let _ = print_problem wording in
    let context = (List.map fst wording) in
    let graph = build' context (List.hd wording) in
    let graph =
      List.fold_left (
        fun acc (i, o) ->
          let g2 = build context i o in
          intersect acc g2
      ) graph (List.tl wording)
    in extract_expression graph

end




(** Graph examples *)
let g1 =
  let open Expression in
  let v0 = (0) in
  let v1 = (1) in
  let v2 = (2) in
  let v3 = (3) in
  let g = Graph.create () |> Graph.add_vertexes [v0 ; v1 ; v2 ; v3] in
  let _s_in = "abad" in
  let e01_1 = Expr.const "d" in
  let e01_2 = Expr.extract
    (Pos_Expression.of_list [P.forward 3; P.backward 1])
    (Pos_Expression.of_list [P.forward 4; P.backward 0])
  in
  let e02_1 = Expr.const "dx" in
  let e03_1 = Expr.const "dxa" in
  let e12_1 = Expr.const "x" in
  let e13_1 = Expr.const "xa" in
  let e23_1 = Expr.const "a" in
  let e23_2 = Expr.extract
    (Pos_Expression.of_list [P.forward 0; P.backward 4])
    (Pos_Expression.of_list [P.forward 1; P.backward 3])
  in
  let e23_3 = Expr.extract
      (Pos_Expression.of_list [P.forward 2; P.backward 2])
      (Pos_Expression.of_list [P.forward 3; P.backward 1])
  in
  Graph.add_edge (v0,v1,(Tag.of_list [e01_1; e01_2])) g
  |> Graph.add_edge (v0, v2, (Tag.singleton e02_1))
  |> Graph.add_edge (v0, v3, (Tag.singleton e03_1))
  |> Graph.add_edge (v1, v2, (Tag.singleton e12_1))
  |> Graph.add_edge (v1, v3, (Tag.singleton e13_1))
  |> Graph.add_edge (v2, v3, (Tag.of_list [e23_1; e23_2; e23_3]))

let example () =
  Printf.printf "====== EX 1 ======\n";
  Problem.print_graph g1;
  Printf.printf "===================\n"



let g2 =
  let open Expression in
  let v0 = (0) in
  let v1 = (1) in
  let v2 = (2) in
  let v3 = (3) in
  let v4 = (4) in
  let g = Graph.create () |> Graph.add_vertexes [v0;v1;v2;v3;v4] in
  let e01_1 = Expr.const "g" in
  let e01_2 = Expr.extract
      (Pos_Expression.of_list [P.forward 3; P.backward 2])
      (Pos_Expression.of_list [P.forward 4; P.backward 1]) in
  let e02_1 = Expr.const "gh" in
  let e02_2 = Expr.extract
    (Pos_Expression.of_list [P.forward 3; P.backward 2])
    (Pos_Expression.of_list [P.forward 5; P.backward 0]) in
  let e03_1 = Expr.const "ghx" in
  let e04_1 = Expr.const "ghxe" in
  let e12_1 = Expr.const "h" in
  let e12_2 = Expr.extract
    (Pos_Expression.of_list [P.forward 4; P.backward 1])
    (Pos_Expression.of_list [P.forward 5; P.backward 0]) in
  let e13_1 = Expr.const "hx" in
  let e14_1 = Expr.const "hxe" in
  let e23_1 = Expr.const "x" in
  let e24_1 = Expr.const "xe" in
  let e34_1 = Expr.const "e" in
  let e34_2 = Expr.extract
    (Pos_Expression.of_list [P.forward 0; P.backward 5])
    (Pos_Expression.of_list [P.forward 1; P.backward 4]) in
  let e34_3 = Expr.extract
    (Pos_Expression.of_list [P.forward 2; P.backward 3])
    (Pos_Expression.of_list [P.forward 3; P.backward 2]) in
  Graph.add_edge (v0, v1, (Tag.of_list [e01_1; e01_2])) g
  |> Graph.add_edge (v0, v2,(Tag.of_list [e02_1; e02_2]))
  |> Graph.add_edge (v0, v3, (Tag.singleton e03_1))
  |> Graph.add_edge (v0, v4, (Tag.singleton e04_1))
  |> Graph.add_edge (v1, v2, (Tag.of_list [e12_1; e12_2]))
  |> Graph.add_edge (v1, v3, (Tag.singleton e13_1))
  |> Graph.add_edge (v1, v4, (Tag.singleton e14_1))
  |> Graph.add_edge (v2, v3, (Tag.singleton e23_1))
  |> Graph.add_edge (v2, v4, (Tag.singleton e24_1))
  |> Graph.add_edge (v3, v4, (Tag.of_list [e34_1; e34_2; e34_3]))

let example2() =
  Printf.printf "====== EX 2 ======\n";
  Problem.print_graph g2;
  Printf.printf "======================\n\n"

let example3 () =
  Printf.printf "====== EX 3 ======\n";
  Problem.build ["abad"] "abad" "dxa" |> Problem.print_graph;
  Printf.printf "======================\n\n"

let example4 () =
  Printf.printf "====== Ex 4 G1 ======\n";
  Problem.print_graph g1;
  Printf.printf "======================\n\n";
  Printf.printf "====== Ex 4 G2 ======\n";
  Problem.print_graph g2;
  Printf.printf "======================\n\n";
  Printf.printf "====== Ex 4 Intersection ======\n";
  Problem.intersect g1 g2 |> Problem.print_graph;
  Printf.printf "======================\n\n"

let example5 () =
  Printf.printf "====== EX 5 ======\n";
  let exprs = Problem.extract_program g1 g2
  in Expr.print_list exprs;
  Printf.printf "======================\n\n"
