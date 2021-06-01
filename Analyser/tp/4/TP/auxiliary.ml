open Whilennh
open Set

(**** Further auxiliary functions for the classic data-flow analysis ****)
(**** See [NNH, pag. 36, pag. 84] ****)



(* Computes the inital labels of statements *)
let rec init_stm = function
  | Skip l -> l
  | Ass (_, _, l) -> l
  | While (_, l, _) -> l
  | If (_, l, _, _) -> l
  | Seq (s1, _) -> init_stm s1
  (* We do not support Bind/Call *)
  | Bind _ | Call _-> assert false

let rec init (p:program) = match p with
  | Program (_, stm) -> init_stm stm


module Finals = Set.Make (Int)

(* Computes the final labels of statements *)
let rec final_stm = function
  | Call (_, _, _, lc, lr) -> Finals.singleton lr
  | Skip l -> Finals.singleton l
  | Ass (_, _, l) -> Finals.singleton l
  | While (_, l, _) -> Finals.singleton l
  | If (_, _, s1, s2) -> Finals.union (final_stm s1) (final_stm s2)
  | Seq (_, s2) -> final_stm s2
  | Bind _ -> assert false

let rec final (p:program) = match p with
  | Program (_, stm) -> final_stm stm

(* 
   Computes the edges - aka flows - between labels in a program.
   Keep in mind that there two different kinds of pairs of labels.
 *)

(* Flow is a set of (label1 * label2) *)
module Flow = Set.Make
    (struct
      type t = int * int
      let compare = compare
    end)

let rec flow_stm = function
  | Skip l -> Flow.empty
  | Ass (_, _, l) -> Flow.empty
  | While (b, l, s) ->
    (* flow s ∪ {(l, init s)} ∪ { (l' , l) | l' ∈ final s } *)
    let flow_s = flow_stm s in
    let snd = Flow.singleton (l, init_stm s) in
    let third =
      Finals.fold
        (fun l' acc -> Flow.add (l', l) acc)
        (final_stm s) Flow.empty
    in
    Flow.union flow_s (Flow.union snd third)
  | If (_, l, s1, s2) ->
    (* flow s1 ∪ flow s2 ∪ {(l, init s1), (l, init s2)} *)
    Flow.union
      (Flow.of_list [(l, init_stm s1); (l, init_stm s2)])
      (Flow.union (flow_stm s1) (flow_stm s2))
  | Seq (s1, s2) ->
    (* flow s1 ∪ flow s2 ∪ {(l, init s2)) | l ∈ final s1 } *)
    let flow_s1 = flow_stm s1 in
    let flow_s2 = flow_stm s2 in
    let final_s1 = final_stm s1 in
    let init_s2 = init_stm s2 in
    let third =
      Finals.fold
        (fun l acc -> Flow.add (l, init_s2) acc)
        final_s1 Flow.empty
    in
    Flow.union flow_s1 (Flow.union flow_s2 third)

  (* We do not support Bind/Call *)
  | Bind _ | Call _ -> assert false


let rec flow (p:program) =
  let flow_proc = function
    | Proc (_, _, _, s, ln, lx) ->
      (* {(ln , init s)} ∪ flow s ∪ {(l, lx) | l ∈ final s} *)
      let fst = Flow.singleton (ln, init_stm s) in
      let snd = flow_stm s in
      let third =
        Finals.fold
          (fun l acc -> Flow.add (l, lx) acc)
          (final_stm s) Flow.empty
      in
      Flow.union fst (Flow.union snd third)
  in
  match p with
  | Program (procs, stm) ->
    (* Compute the union of every flow proc *)
    List.fold_left
      (fun acc p -> Flow.union (flow_proc p) acc)
      Flow.empty procs
    |>
    Flow.union (flow_stm stm)

let inv (x, y) = (y, x)

let flow_stm_inv stm =
  flow_stm stm |> Flow.map inv

(* Compute the flow a program and reverse it to get flow(-1) *)
let flow_inv (p:program) =
  flow p |> Flow.map inv

