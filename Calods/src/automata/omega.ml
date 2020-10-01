open Nba
open State
open Transition

type omega =
  | Omega of reg
  | OmegaConcat of reg * omega
  | OmegaChoice of omega * omega

and reg =
  | Epsilon
  | Alpha of string
  | Choice of reg * reg
  | Concat of reg * reg
  | Multiple of reg

let omega_print =
  let rec omega_to_string = function
    | Omega r ->
      let s = reg_to_string r in
      "(" ^ s ^ "ω)"
    | OmegaConcat (r, o) ->
      let s1 = reg_to_string r in
      let s2 = omega_to_string o in
      "(" ^ s1 ^ " . " ^ s2 ^ ")"
    | OmegaChoice (o1, o2) ->
      let s1 = omega_to_string o1 in
      let s2 = omega_to_string o2 in
      "(" ^ s1 ^ " + " ^ s2 ^ ")"

  and reg_to_string = function
    | _ -> "reg"

  in
  (fun e -> Printf.printf "%s\n" (omega_to_string e))

let id = ref 0

let fresh_id () =
  let id' = !id in
  id := !id + 1;
  id'

let look_fresh_id () =
  !id


let print_list l =
  Printf.printf "[";
  List.iter (fun (x,y,z) -> Printf.printf "(%d,%d,%s);" x y z) l;
  Printf.printf "]\n"

let rec prev_id = function
  | [(_, s, _)] -> s
  | _x::xs -> prev_id xs
  | [] -> assert false

let extract_states l =
  List.fold_right
    (fun (s0,s1,_) acc ->
       let acc =
         if List.mem s0 acc then
           acc
         else
           s0 :: acc
       in
       let acc =
         if List.mem s1 acc then
           acc
         else
           s1 :: acc
       in
       acc
    ) l []

let omega_to_nba =
  let rec omega_to_nba_aux state = function
    | Omega r ->
      let x = match state with
        | None -> fresh_id ()
        | Some x -> x
      in
      let l = reg_to_nba x (Some x) r in
      (extract_states l, l)

    | OmegaConcat (r, o) ->
      let x =  match state with
        | None -> fresh_id ()
        | Some x -> x
      in
      let r = reg_to_nba x None r in
      let y = prev_id r in
      let (acpts, ts) = omega_to_nba_aux (Some y) o in
      (acpts, r @ ts )

    | OmegaChoice (o1, o2) ->
      let x = fresh_id () in
      let (acpts, l1) = omega_to_nba_aux (Some x) o1 in
      let (acpts', l2) = omega_to_nba_aux (Some x) o2 in
      (acpts @ acpts', l1 @ l2) 

  and reg_to_nba state loop = function
    | Concat (r1, r2) ->
      (* Compile r1 starting at state *)
      let l1 = reg_to_nba state None r1 in

      (* New state for r2 is the end of r1 *)
      let s2 = prev_id l1 in

      (* Compile r2 starting at end of r1 *)
      let l2 = reg_to_nba s2 loop r2 in

      l1 @ l2

    | Alpha s ->
      let next = match loop with
        | Some x -> x
        | None -> fresh_id ()
      in
      [(state, next, s)]

    | Epsilon ->
      let next = match loop with
        | Some x -> x
        | None -> fresh_id ()
      in
      [(state, next, "ε")]

    | Choice (r1, r2) ->
      (* Compile r1 starting at state *)
      let l1 = reg_to_nba state loop r1 in
      let l2 = reg_to_nba state loop r2 in
      l1 @ l2

    | Multiple r ->
      reg_to_nba state (Some state) r

  in
  let _initials = [0] in
  (fun e ->
     let (accepts, transitions) = omega_to_nba_aux None e in
     let initials = [0] in
     let states = List.init (look_fresh_id ()) (fun i -> i)  in

     let to_state =
       List.map (fun s -> S.create s)
     in

     let sch =
       Scheduler.create
         (to_state states)
         (List.map
            (fun (s1,s2,t) ->
               T.create (S.create s1, S.create s2, t)) transitions
         )
         (to_state initials)
         (to_state accepts)
     in
     sch
  )
