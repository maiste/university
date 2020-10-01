(*
 * Chaboche - Marais
 * CALODS - 2019
 *)


open Types
open Helper

(* Get arg name based on the process and arg position *)
let get_arg ps n i =
  let rec find_ps = function
    | S.Process (n', args, _, _)::_ when (n'=n)->
      let args = explodes args in
      begin match List.nth args i with
        | S.Arg ({value=arg_name; _}, _) ->
          arg_name
      end
    | _::xs ->
      find_ps xs
    | _ ->
      assert false
  in
  find_ps ps

(* Create prism affectation of variable n with v *)
let set_variable conf n v =
  let curr = state ~l:conf.label ~id:conf.id () in
  let next = state_next ~l:conf.label  ~id:(conf.id+1) ~next_id:conf.next_id () in
  ({conf with id=conf.id+1},
   T.Instruction (None, curr, T.Affectation (n, v), Some next)
  )

let set_variable_foreach conf n vs =
  {conf with id=conf.id+1},
  List.fold_right
    (fun v acc ->
       let (_, set) = set_variable conf ("_forall_"^n) (string_of_int v) in
       set::acc
    ) vs []

let translate_sets env conf ps n args =
  let (sets, conf, _) =
    List.fold_left
      (fun (acc, conf, i) v ->
         let var_to_set = "_arg_" ^ n ^ "_" ^ get_arg ps n i in
         let f_set = set_variable conf var_to_set in
         let direct_value = match v with
           | S.ArrayValue _ -> true
           | S.Value ({value=v; _}) -> is_value env v
         in
         let (conf, set) =
           if direct_value then
             f_set (literal env "" v)
           else
             f_set ("_forall_" ^ literal env "" v)
         in (acc@[set], conf, i+1)
      ) ([], conf, 0) args
  in
  conf, sets

let rec translate_call env conf ps = function
  | S.CallProcess ({value=n;_}, Some args) ->
    let args = explodes args in
    translate_sets env conf ps n args
  | S.CallProcess _ ->
    conf, []
  | S.ForAll (_, _, {value=c;_}) ->
    translate_call env conf ps c
  | S.Parallel ({value=c1;_},{value=c2;_})
  | S.SeqI ({value=c1;_},{value=c2;_}) ->
    let (conf, sets) = translate_call env conf ps c1 in
    let (conf', sets') = translate_call env conf ps c2 in
    conf', sets@sets'

let extract_forall_vars env (S.Main c) =
  let rec extract c = match Compiler.Position.value c with
    | S.CallProcess _ ->
      []
    | S.Parallel (c, c')
    | S.SeqI (c, c') ->
      extract c @ extract c'
    | S.ForAll (names, {value=ty; _}, c) ->
      List.map (fun x -> (x, ty)) names @ extract c
  in
  let vars = extract c in
  List.map
    (fun (x, ty) ->
       let bounds = bounds_values_from_type env ty in
       T.LocalVar ("_forall_"^ x, bounds, fst bounds)
    ) vars

let let_go_ps conf ps =
  let afct p =
    match p with
    | S.Process (p, _, _, _) ->
      T.Affectation ("_" ^ p ^ "_canGo", "1")
  in
  let rec aux ps =
    match ps with
    | [p] -> afct p
    | p::ps -> T.Seq (afct p, aux ps)
    | [] -> assert false
  in
  let st_bef = state ~l:conf.label ~id:conf.id () in
  let st_aft = state ~l:conf.label ~id:(conf.id+1) () in
  [T.Instruction (None, st_bef, aux ps, Some st_aft)]
