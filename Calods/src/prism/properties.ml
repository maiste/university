open Helper
open Compiler
open PrismAst

module S = Compiler.Ast

module T = PrismAst

let id x = x

let get_prism_value env s =
  let l = S.Value (Position.w_pos s) in
  literal env "" l

let label_name_for_args env name args_values =
  let args_str = List.fold_left (fun acc x -> acc ^ (get_prism_value env x) ^ "_") "" args_values in
  let args_str = String.sub args_str 0 (String.length args_str - 1) in
  name ^ "_" ^ args_str

let label_name_for_decid name x =
  name ^ "_with_" ^ x

module Labels = Set.Make(struct
    type t = T.labels
    let compare = compare
  end)

let rec create_test env n i arg_values arg_names =
  match arg_values, arg_names with
  | [], [] -> T.EqualT ("i", string_of_int i)
  | (x::xs, y::ys) ->
    let next = create_test env n i xs ys in
    let arg = "_arg_" ^ n ^ "_" ^ y in
    T.AndT (T.EqualT (arg, get_prism_value env x), next)
  | _ -> assert false

let add_entry_labels env arg_names entries (T.Init (_, _, _, is)) =
  let empty = Labels.empty in
  let i = distinct_state is in
  let create_labels_for_args p_args =
    List.fold_left
      (fun acc (n, arg_values) ->
         let name = label_name_for_args env n arg_values in
         let test = create_test env n i arg_values (List.assoc n arg_names) in
         Labels.add (T.Label (name, test)) acc
      ) empty p_args
  in
  List.fold_left (fun acc x -> Labels.union (create_labels_for_args x) acc) empty entries |>
  Labels.elements

let find_pos_affect to_find acc x =
  match x with
  | T.Instruction (_, State(l, i, _), T.Affectation (x, v), _)
    when to_find=x ->
    (l, i, v) :: acc
  | _ -> acc

let add_decid_labels acc (T.Process (pn, _, _, is)) =
  let to_find = "_decide_" ^ pn in
  let decids = List.fold_left (find_pos_affect to_find) [] is in
  let rec create_test = function
    | [(l, i, _)] -> T.EqualT (l, string_of_int i)
    | (l, i, _)::xs -> T.OrT (T.EqualT (l, string_of_int i), create_test xs)
    | [] -> T.BoolT false
  in
  let test = create_test decids in
  (T.Label (pn^"_decid", test)) :: acc



let add_decid_output ps =
  let rec aux pn var x y =
    if x < y then
      let i = string_of_int x in
      let n = label_name_for_decid pn i in
      T.Label (n, T.EqualT (var, i)) :: aux pn var (x+1) y
    else []
  in
  let create_label acc x =
    match x with
    | T.Process (pn, _, decls, _) ->
      let to_find = "_decide_" ^ pn in
      List.fold_left
        (fun acc (T.LocalVar (name, (x, y), _)) ->
           if name = to_find then
             let labels = aux pn to_find x y in
             labels @ acc
           else acc
        ) [] decls
      @ acc
  in
  List.fold_left create_label [] ps


let rec extract_args_with_name = function
  | S.Process (name, args, _, _)::s ->
    let args = explodes args |> List.map (fun (S.Arg (n, _)) -> n) in
    (name, explodes args) :: extract_args_with_name s
  | [] -> []

let f x = F x
let g x = G x
let andf x y = AndF (x, y)
let orf x y = OrF (x, y)

let rec conj g f = function
    | [x] -> f x
    | x::xs -> g (f x) (conj g f xs)
    | _ -> assert false

let translate_entry env entry =
  let translate (n, arg_values) =
    LabelF (label_name_for_args env n arg_values)
  in
  conj andf translate entry

let get_who_decid output =
  let f acc = function
    | S.Tauto n -> NotLabelF (n^"_decid") :: acc
    | S.Exist (n, x) ->
      LabelF (n^"_decid") ::
      LabelF (label_name_for_decid n x) ::
      acc
  in
  List.fold_left f [] output

let translate_entry_output env (entry, output) =
  let left =
    translate_entry env entry
  in
  let right =
    List.map get_who_decid output |>
    List.map (conj andf id) |>
    conj orf (fun x -> f (g x))
  in
  ArrowF(left, right)

let create_formula env entries_outputs =
  let rec aux = function
    | [x] -> translate_entry_output env x
    | x::xs -> OrF (translate_entry_output env x, aux xs)
    | [] -> assert false
  in
  A (G (aux entries_outputs))

let properties env ps ps_prism main props =
  match props with
  | None -> None
  | Some l ->
    let args = extract_args_with_name ps in
    let entries = List.map fst l in
    let entry = add_entry_labels env args entries main in

    let decids = List.fold_left (add_decid_labels) [] ps_prism in
    let decids_outputs = add_decid_output ps_prism in

    let formula = create_formula env l in
    Some (entry @ decids @ decids_outputs, formula)
