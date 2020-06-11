(*
 * Manage Regexp
 * CHABOCHE - MARAIS
 *)


(** Define types *)
type class_expr =
  | AlphaNum
  | Alpha
  | Num
  | Lower
  | Upper
  | Special of char

type token =
  | Plus of class_expr
  | PlusComp of class_expr

type ini = EmptyI | Start

type fin = EmptyF | End

type regexp = ini * token list * fin

(** Class Set *)
module ClassSet = Set.Make (struct
  type t = class_expr
  let compare = compare
end)

(** Char Set *)
module AsciiSet = Set.Make (
struct
  type t = char
  let compare = compare
end)


(** Printing function *)
let print_ini = function
  | EmptyI -> Printf.printf ""
  | Start -> Printf.printf "^ "

let print_fin = function
  | EmptyF -> Printf.printf ""
  | End -> Printf.printf "$"

let print_class = function
  | AlphaNum -> Printf.printf "alphanum"
  | Alpha -> Printf.printf "alpha"
  | Num -> Printf.printf "num"
  | Lower -> Printf.printf "lower"
  | Upper -> Printf.printf "upper"
  | Special c -> Printf.printf "%c" c

let print_token = function
  | Plus c ->
      Printf.printf "Plus("; print_class c; Printf.printf ")"
  | PlusComp c ->
      Printf.printf "PlusComp("; print_class c; Printf.printf ")"

let print_regexp (ini, tokens, fin) =
  print_ini ini;
  List.iter (fun t -> print_token t ; Printf.printf " ") tokens;
  print_fin fin

let rec print_regexps = function
  | [] -> ()
  | [e] -> print_regexp e
  | e::es -> print_regexp e ; Printf.printf ";" ; print_regexps es



(** Get the size of [re] *)
let taille_re re =
  match re with
  | None -> 0
  | Some ((_, ts, _), _, _) -> List.length ts

  (** Return true if [c] is in [class_expr] *)
let rec in_class class_expr c =
  let c' = Char.code c in
  match class_expr with
  | Num -> c' >= 48 && c' <= 57
  | Lower -> c' >= 97 && c' <= 122
  | Upper -> c' >= 65 && c' <= 90
  | Alpha -> in_class Lower c || in_class Upper c
  | AlphaNum -> in_class Alpha c || in_class Num c
  | Special x -> c' = (Char.code x)

(** Add elements in case they are both needed in [set] *)
let conditional_add set =
  let set =
    if ClassSet.mem Lower set && ClassSet.mem Upper set then
      ClassSet.add Alpha set
    else set
  in
  let set =
    if ClassSet.mem Num set && ClassSet.mem Alpha set then
      ClassSet.add AlphaNum set
    else set
  in set

(** Return [s] filled with the class that match the caracter [c]*)
let match_with_classes c s =
  let classes = [
    Num ;
    Lower ;
    Upper ;
    Special '/' ;
    Special '_' ;
    Special '\\' ;
    Special '-'
  ] in
  let aux acc cls =
    if in_class cls c then ClassSet.add cls acc
    else acc
  in
  List.fold_left aux s classes

let next k = function
  | `Forward -> k + 1
  | `Backward -> k - 1

let stop k n = function
  | `Forward -> k = n
  | `Backward -> k < 0

let start k = function
  | `Forward -> 0
  | `Backward -> k - 1

let is_in_token c = function
  | Plus class_exp -> in_class class_exp c
  | PlusComp class_expr -> not (in_class class_expr c)

(** Return the first position where the [token] matches in [s].
    [d] specifies the direction *)
let first_pos_token d token s =
  let n = String.length s in
  let rec aux k =
    if stop k n d then k
    else
      let c = String.get s k in
      if is_in_token c token then k
      else aux (next k d)
  in
  aux (start n d)

(** Return the most char the [token] can match in [s]
    following the direction [d] *)
let in_token d token s =
  let n = String.length s in
  let rec aux k =
    if stop k n d then 0
    else
      let c = String.get s k in
      if is_in_token c token then 1 + aux (next k d)
      else 0
  in
  aux (start n d)


let inc_pos k (be, af) = function
  | `Backward ->
      if be <> -1 then (be-1, af)
      else (k, k+1)
  | `Forward ->
      if be <> -1 then (be, af+1)
      else (k, k+1)

(** Return the before and the after of [re] in [word] *)
let get_regex_pos word re =
  let len = String.length word in
  let k, move, sens, nb_try, re =
    match re with
    | (_, re, End) ->
        len - 1, -1, `Backward, 1, List.rev re
    | (_, re, _) -> 0, 1, `Forward, 0, re
  in
  let rec aux nb_try k (be, af) = function
    | [] -> (be, af)
    | t::r as re ->
      if k < 0 || k >= len then (be, af)
      else begin
        let c = String.get word k in
        let match_token = is_in_token c t in
        match match_token, nb_try with
        | true, 1 | true, 2 ->
            aux 1 (k+move) (inc_pos k (be, af) sens) re
        | true, 0 ->
            aux 1 (k+move) (k, k+1) re
        | false, 0 ->
            aux 0 (k+move) (be, af) re
        | false, 1 ->
            aux 2 k (be, af) r
        | _ -> (be, af)
      end
  in aux nb_try k (-1, -1) re

(** Return a set filled with all classes in the string [s] *)
let get_classes s =
  let set = ClassSet.empty in
  let len = String.length s in
  let rec iterator acc k =
    if k < len then
        let acc = match_with_classes (String.get s k) acc in
        iterator acc (k+1)
    else acc
  in
  iterator set 0

(** Return classes in a the string list *)
let classes_for_ss = function
  | [] -> assert false
  | x::xs ->
      let set = List.fold_left
        (fun acc x ->
          ClassSet.union acc (get_classes x)
        ) (get_classes x) xs
      in
      conditional_add set 

(** Signature of the regexp generator *)
module type REGEXP = sig
  val set_limit : int -> unit
  val init : string -> string list -> unit
  val print: unit -> unit
  val get : (int * int) -> (regexp * int * int) list
end

(** Module to generate Regexp *)
module RE : REGEXP = struct
  let classes = ref (ClassSet.empty)

  let matrix = ref ((Array.make_matrix 0 0 []), "")

  let limit = ref 8

  (** Generate token for [c] *)
  let tokens_for_c c =
    let create_token c c_e =
      if in_class c_e c then Plus (c_e)
      else PlusComp (c_e)
    in
    List.map (create_token c) (ClassSet.elements !classes)

  (** Concat t to ts *)
  let concat_tokens d t re =
    let ts = match re with
      | None -> []
      | Some ( (_, ts, _), _, _ ) -> ts
    in
    match d with
    | `LR -> ts @ [t]
    | `RL -> t :: ts

  (** New positions (bef, aft) based on previous re
      `LR -> new afer
      `RL -> new before
  *)
  let new_pos d s bef aft i j t =
    let n = String.length s in
    match d with
    | `LR ->
      if bef = -1 && aft = -1 then
        let x = first_pos_token `Forward t s in
        let s' = String.sub s x (n - x) in
        (x, x + in_token `Forward t s')
      else
        let s' = String.sub s aft (n - aft) in
        (bef, aft + in_token `Forward t s')
    | `RL ->
      if bef = -1 && aft = -1 then
        (n - in_token `Backward t s, j)
      else
        let s' = String.sub s 0 (i+1) in
        (bef - in_token `Backward t s', aft)

  (** Add a new token to previous re, add only if it's pertinent in the acc *)
  let add d re i j acc t =
    let s = snd !matrix in
    let (bef, aft) =
      match re with
      | None -> -1, -1
      | Some (_, bef, aft) -> bef, aft
    in
    let (bef', aft') = new_pos d s bef aft i j t in
    let ts = concat_tokens d t re in
    match d with
    | `RL ->
      if bef <> bef' then ((EmptyI, ts, End), bef', j) :: acc
      else acc
    | `LR ->
      if (bef = bef' && aft = aft') || not (bef' = i || aft' = j) then acc
      else ((EmptyI, ts, EmptyF), bef', aft') :: acc

  (** Iterate on every re in the previous cell *)
  let adds d to_add i j re =
    if taille_re re < !limit then
      let add = add d re i j in
      match d, re with
      | `RL, Some (((_, _, End), bef, _) as x) ->
        if bef <= i then [x]
        else List.fold_left add [] to_add
      | `LR, Some ((_, _, aft) as x) ->
        if aft >= j then [x]
        else List.fold_left add [] to_add
      | _, None ->
        List.fold_left add [] to_add
      | _ -> []
    else []

  (** Create re for cell (i, j),
      based on cells: (i, k < j) and (k > i, j) *)
  let fill_cell n (i, j) =
    let s = snd !matrix in
    let do_left () =
      let to_add = tokens_for_c (String.get s (j-1)) in
      let left =
        if j = 0 then []
        else (fst !matrix).(i).(j-1)
      in
      let add = adds `LR to_add i j in
      List.concat (List.map (fun x -> add (Some x)) left) @
      add None
    in
    let do_right () =
      if j = n then
        let right =
          if i = n - 1 then []
          else (fst !matrix).(i+1).(j)
        in
        let to_add = tokens_for_c (String.get s i) in
        let add = adds `RL to_add i j in
        let first_case = if i = n-1 then add None else [] in
        List.concat (List.map (fun x -> add (Some x)) right) @
        first_case
      else []
    in
    (fst !matrix).(i).(j) <- do_left () @ do_right ()

  (** Fill the matrix from (n, n+1) to (0, 0) *)
  let fill () =
    let n = String.length (snd !matrix) in
    let rec aux i j =
      if i = 0 && j = n + 1 then ()
      else if j = n+1 then aux (i-1) i
      else
        let _ = fill_cell n (i, j) in
        aux i (j+1)
    in
    aux (n-1) n

  (** Set a new limit [l] if [l] > 0 *)
  let set_limit l =
    if l > 0 then
      limit := l
    else
      failwith "Negative limite"

  (** Init the set of classes in the [context] and create a matrix 
      for [s] *)
  let init s context =
    try
      let n = String.length s in
      let _ = if ClassSet.is_empty !classes then
          classes := classes_for_ss context
      in
      matrix := (Array.make_matrix n (n+1) ([]), s);
      fill ()
    with
    | _ -> ()


  let print () =
    let print_case i j t' =
      let f x  = match x with
        | (t, bef, aft) ->
          Printf.printf "\t(";
          print_regexp t; Printf.printf ", %d, %d)" bef aft;
          Printf.printf "\n"
      in
      let _ = Printf.printf "(%d, %d):\n" i j in
      let _ = List.iter f t' in
      Printf.printf "\n";
    in
    Array.iteri (fun i t -> Array.iteri (print_case i) t) (fst !matrix)

  (** Return all regexps from [i] to [j] in the matrix *)
  let get (i, j) =
    (fst !matrix).(i).(j)

end
