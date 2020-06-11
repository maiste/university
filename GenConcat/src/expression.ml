(**
 * Manage expressions
 * CHABOCHE - MARAIS
 *)

(** Signature to manage position *)
module type POS = sig
  type t
  type regexp
  val compare : t -> t -> int
  val forward : int -> t
  val backward : int -> t
  val before : regexp -> t
  val after : regexp -> t
  val to_int : string -> t -> int
  val print : t -> unit
end

(** Module to manage position *)
module P : POS with type regexp = Regexp.regexp = struct
  type regexp = Regexp.regexp
  type t =
  | Forward of int
  | Backward of int
  | Before of regexp
  | After of regexp

  let compare = compare

  let forward i = Forward i

  let backward i = Backward i

  let before re = Before re

  let after re = After re

  let to_int s = function
    | Forward i -> i
    | Backward i -> String.length s - i
    | Before re ->
      fst (Regexp.get_regex_pos s re)
    | After re ->
      snd (Regexp.get_regex_pos s re)

  let print = function
    | Forward i ->
      Printf.printf "Forward %d" i
    | Backward i ->
      Printf.printf "Backward %d" i
    | Before re ->
      Printf.printf "Before( ";
      Regexp.print_regexp re;
      Printf.printf ")"
    | After re ->
      Printf.printf "After( ";
      Regexp.print_regexp re;
      Printf.printf ")"
end

(** Module to manage Pos_expression as a Set of positions *)
module Pos_Expression = Set.Make (P)



(** Signature for managing expressions *)
module type EXPRESSION = sig
  module P : POS
  type pos_expression
  type t
  val print : t -> unit
  val print_list : t list -> unit
  val to_solution : t list -> t list
  val const : string -> t
  val extract : pos_expression -> pos_expression -> t
  val evaluate_expr : string -> t -> string
  val evaluate_exprs : string -> t list -> string
  val check : t list -> (string * string) list -> unit
end

(** Define what an expression can be *)
type expression =
| Const of string
| Extract of Pos_Expression.t * Pos_Expression.t

(** Module to manage expressions *)
module Expr :
EXPRESSION with type pos_expression = Pos_Expression.t
and type t = expression =
struct

  module P = P
  type pos_expression = Pos_Expression.t
  type t = expression

  let print e =
    let print_pos p =
      Pos_Expression.iter
      (fun p -> Printf.printf "   * "; P.print p; Printf.printf ";\n") p;
    in
    match e with
    | Const s ->
      Printf.printf "<< Const(\"%s\") >>" s
    | Extract (p1, p2) ->
      begin
        Printf.printf "<<\nExtract ";
        Printf.printf "{\n";
        print_pos p1;
        Printf.printf "}, {\n";
        print_pos p2;
        Printf.printf "}\n>>"
      end

  let print_list lst =
    List.iter (fun e -> print e ; Printf.printf "\n") lst;
    Printf.printf "\n\n"

  let to_solution lst =
    let aux f = function
      | Extract (p1, p2) ->
        Extract (f p1, f p2)
      | e -> e
    in
    let f x = Pos_Expression.singleton (Pos_Expression.choose x) in
    List.map (aux f) lst


  let const s = Const s

  let extract p1 p2 = Extract (p1, p2)

  (** Evaluate expression s e -> s' *)
  let rec evaluate_expr s = function
  | Const s -> s
  | Extract (pe1, pe2) ->
    let i1 = evaluate_pos_expr s pe1 in
    let i2 = evaluate_pos_expr s pe2 in
    String.sub s i1 (i2 - i1)

  and evaluate_pos_expr s pos =
    let p = Pos_Expression.choose pos in
    P.to_int s p

  let evaluate_exprs s es =
    List.fold_right (fun e acc -> (evaluate_expr s e) :: acc) es []
    |> String.concat ""

  let check exprs lst =
    let check' (i, o) =
      if evaluate_exprs i exprs <> o then
        failwith "Not Valid"
    in
    List.iter check' lst
end



(** Module to manage Expressions in graph as a Set *)
module Tag = Set.Make (struct
  type t = Expr.t
  let compare = compare
end)



(** Examples expressions *)
let example1 () =
  let s1 = "Mr Smith junior" in
  let e1 = Expr.const "Hello" in
  let e2 =
    Expr.extract
      (Pos_Expression.singleton (P.forward 3))
      (Pos_Expression.singleton (P.backward 7))
  in
  let s2 = Expr.evaluate_exprs s1 [e1; e2] in
  Printf.printf "%s\n" s2

let example2 () =
  let s1 = "10/10/2017" in
  let e =
    Expr.extract
      (Pos_Expression.singleton (P.forward 3))
      (Pos_Expression.singleton (P.backward 5))
  in
  let s2 = Expr.evaluate_exprs s1 [e] in
  Printf.printf "%s\n" s2
