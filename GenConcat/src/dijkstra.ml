(**
 * Manage the search in the graph
 * CHABOCHE - MARAIS
 *)

open Expression

let mem =
  let x = Hashtbl.create 64 in
  Hashtbl.add x 0 (0.0, Some 0); x

module I = struct
  type t = int
  let compare a b =
    let (x, _) = Hashtbl.find mem a in
    let (y, _) = Hashtbl.find mem b in
    compare x y
end

(** Define a new queue *)
module Queue = Psq.Make (I) (I)

(** Dijkstra Init -> (Distance hashtable, priority queue) *)
let init vs =
  (* Distance = inf if x is not the source *)
  let init_distance mem x =
    let v = if x = 0 then (0.0, Some 0)
      else (Pervasives.infinity, None)
    in Hashtbl.add mem x v
  in
  (* Empty prio queue and distance table *)
  let queue = ref Queue.empty in
  (* Iter to add distance = inf and prio queue *)
  List.iter (fun v -> init_distance mem v; queue := Queue.add v v !queue) vs;
  queue

(**
   for each neighbor v of u:
     alt ← dist[u] + length(u, v)
       if alt < dist[v]
         dist[v] ← alt
         prev[v] ← u
         Q.decrease_priority(v, alt)
*)
let update_distances u vs q =
  let update (v, _, w) =
    let (alt, _) = Hashtbl.find mem u in
    let alt = alt +. w in
    let (dist_v, _) = Hashtbl.find mem v in
    if alt < dist_v then
      let _ = Hashtbl.replace mem v (alt, Some u) in
      let x = Queue.remove v !q in
      q := Queue.add v v x
  in
  List.iter update vs

let extract_path d end_n =
  let path = ref [] in
  let s = ref end_n in
  while !s <> 0 do
    path := !s :: !path;
    match Hashtbl.find d !s with
    | _ , None -> failwith "No path"
    | _, Some s' -> s := s'
  done;
  path := 0 :: !path;
  !path

let exprs_from_path hes path =
  let rec aux prev = function
    | [] -> []
    | curr::xs ->
      let l = Hashtbl.find hes prev in
      let x = List.find_opt (fun (x, _, _) -> x = curr) l in
      match x with
      | None -> assert false
      | Some (_, t, _) ->
        Tag.choose t :: (aux curr xs)
  in
  let prev = List.hd path in
  aux prev (List.tl path)

let dijkstra vs hes =
  try
    let queue = init vs in
    let _ =
      while not (Queue.is_empty !queue) do
        let x = Queue.pop !queue in
        match x with
        | None -> ()
        | Some ( (u, _), queue') ->
            queue := queue';
          let voisins = Hashtbl.find hes u in
        update_distances u voisins queue
      done
    in
    let end_n = (List.length vs) - 1 in

    (* Extract path *)
    let path = extract_path mem end_n in

    (* Expressions from path *)
    let exprs = exprs_from_path hes path in
    exprs
  with
  | _ -> failwith "Aucun chemin\n"
