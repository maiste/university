(** This module implements an optimized compilation of pattern-matching.

    It is based on the article

    "Compiling Pattern Matching to Good Decision Trees"

    written by Luc Maranget
    and published in the proceedings of the ML workshop 2005.

*)
open Position
open HobixUtils

module S = HopixAST
module T = HobixAST

(** A path is a sequence of indices which allow for accessing
    a component which may be deeply nested inside a block. In reverse order. *)
type path = int list

(** Each identifier bound by the pattern will finally be associated to
    a path. *)
type binding = T.identifier * path

(** A pattern-matching matrix has a row for each possible
    case and as many columns as the number of components of
    the matched value. *)
type matrix = row list

and row = S.pattern list * binding list * T.expression

(** [nb_columns m] returns the number of columns of [m]. *)
let nb_columns = function
  | [] ->
     0
  | (ps, _, _) :: rows ->
     let n = List.length ps in
     assert (List.for_all (fun (ps, _, _) -> List.length ps = n) rows);
     n

(** [string_of_path occ] produces a human-readable version of [occ]. *)
let string_of_path occ =
  String.concat "." (List.rev_map string_of_int occ)

(** [string_of_bindings bs] produces a human-readable version of [bs]. *)
let string_of_bindings bs =
  String.concat ", " (
      List.map (fun (T.Id x, p) ->
          Printf.sprintf "%s = %s" x (string_of_path p)
        ) bs)

(** [string_of_matrix m] produces a human-readable version of [m]. *)
let string_of_matrix m =
  let csizes = Array.make (nb_columns m) 0 in
  let string_of_pattern i p =
    let s = HopixPrettyPrinter.(to_string pattern p) in
    csizes.(i) <- max csizes.(i) (String.length s);
    s
  in
  let complete_pattern i p =
    p ^ String.make (csizes.(i) - String.length p) ' '
  in
  let string_of_expression e =
    HobixPrettyPrinter.(to_string expression e)
  in
  let b = Buffer.create 13 in
  List.map (fun (ps, bs, e) ->
      (List.mapi string_of_pattern ps,
       string_of_bindings bs,
       string_of_expression e)) m
  |> List.iter (fun (ps, bs, e) ->
         Buffer.add_string b (
             String.concat " " (List.mapi complete_pattern ps)
             ^ " -> " ^ bs ^ " in " ^ e ^ "\n"
           )
       );
  Buffer.contents b

(** We may observe if a value is a tagged value with a specific
    constructor or is equal to a given literal. *)
type observation =
  | CTag of S.constructor
  | CLit of S.literal

module ObsSet = Set.Make (struct
  type t = observation * int
  let compare = compare
end)

(** [head_constructors m] returns the list of observations of the
   first column of [m] without repetition. Each observation comes with
   its arity. A literal has an arity of 0 and constructor has for arity
   the number of arguments it is applied to. This function assumes
   that the patterns in the matrix are well-typed, hence we can deduce
   the arity of the constructors directly from their application. *)
let head_constructors m : (observation * int) list =
  let rec extract x =
    match x with
    | (S.PTaggedValue (c, _, ps)) ->
       let c = Position.value c in
       ObsSet.singleton (CTag c, List.length ps)
    | (S.PLiteral l) ->
       let l = Position.value l in
       ObsSet.singleton (CLit l, 0)
    | S.PAnd xs -> assert false
    | S.PTypeAnnotation _ -> assert false
    | _ -> ObsSet.empty in
  let aux acc ((xs,_,_) : row) =
    ObsSet.union acc (extract (List.hd xs))
  in ExtStd.List.of_seq (ObsSet.to_seq (List.fold_left aux ObsSet.empty m))

(** Replace an S.identifier by an T.identifier *)
let identifier (S.Id id) = T.Id id

(** Replace the value by inserting a new [pos] into the [id] path. *)
let replace_binding id path bs =
  if List.exists (fun (id',_) -> id = id') bs
  then List.map (fun ((id', _) as b) -> if id = id' then (id, path) else b) bs
  else (id,path)::bs

(** [specialize occ c arity m] returns the matrix [m] in which rows that
    do not match the observation [c] are removed and the others
    have new columns to match the subcomponents of [c].
    - [m] must have at least one row and one column.
    - [arity] is the arity of [c].
    - [occ] is the path to the scrutinee's component matched by the
      first column of [m].
*)
let specialize occ c arity (m : matrix) : matrix =
  let spec p ps bs e =
    match p,c with
    | S.PTaggedValue (c, _, ps'), CTag c' when value c = c' ->
       let ps' = List.map value ps' in
       Some (ps' @ ps, bs, e)
    | S.PLiteral l, CLit l' when value l = l' ->
       Some (ps, bs, e)
    | S.PVariable v , _ ->
       let id = identifier (Position.value v) in
       Some (ps,replace_binding id occ bs, e)
    | S.PWildcard, _ ->
       Some (ExtStd.List.repeat arity (S.PWildcard) @ ps, bs, e)
    | S.PTypeAnnotation _,_ -> assert false
    | _, _ -> None in
  let aux (ps,bs,e) =
    let p,ps = List.hd ps, List.tl ps in
    spec p ps bs e
  in ExtStd.List.filter_map aux m

(** [default occ m] returns the default matrix of [m], that is the matrix
    corresponding to the remaining tests to do if the default case of the
    first column of [m] has been chosen. *)
let default occ (m : matrix) : matrix = (* Todo why occ is a parameter ? *)
  let rec pred = function
    | HopixAST.PWildcard -> true
    | HopixAST.POr xs -> List.exists (fun x -> pred (value x)) xs
    | HopixAST.PAnd xs -> List.for_all (fun x -> pred (value x)) xs
    | HopixAST.PTypeAnnotation _ -> assert false
    | _ -> false in
  let filterpred (xs,y,z) =
    if pred (List.hd xs)
    then Some (List.tl xs,y,z)
    else None in
  ExtStd.List.filter_map filterpred m

(** [split n] returns the list of occurrences [occ.0; occ.1; ..;
    occ.(n - 1)]. *)
let split n occ =
  List.(init n (fun i -> i::occ))

(** [replace_and_get i x xs] replace the ith elements of [xs] by [x] and
    return the original ith element of [xs] *)
let replace_and_get i x xs =
  let rec aux i xs =
    match xs with
    | [] -> assert false
    | y::ys ->
       if i = 0 then (y,x::ys)
       else
         let resy,ys' = aux (i-1) ys in
         resy,y::ys'
  in aux i xs

(** [swap_columns i m] returns a new matrix which is [m] in which
    column 0 and column i have been exchanged. *)
let swap_columns i (m : matrix) =
  if i = 0 then m else
    let swap_on_one_line l =
      match l with
      | [] -> assert false
      | x::xs ->
         let x,xs = replace_and_get (i-1) x xs in
         x::xs
  in List.map (fun (xs,y,z) -> swap_on_one_line xs, y, z) m

(** [swap_occurences i occs] returns a new list of occurrences in
    which the occ numbered 0 and the occ numbered i have been
    exchanged. *)
let swap_occurences i occs =
  match occs with
  | [] -> []
  | x::xs ->
      let x, xs = replace_and_get (i-1) x xs in
      x::xs

type decision_tree =
  | Fail
  | Dup    of int * decision_tree (* Dup (i,t) duplicates i times the head value *)
  | Leaf   of binding list * T.expression
  | Switch of path * (observation * int * decision_tree) list * decision_tree option
  | Swap   of int * decision_tree

let string_of_constructor = function
  | CTag (S.KId s) -> s
  | CLit l -> HopixPrettyPrinter.(to_string literal l)

(** [string_of_decision_tree t] produces a human-readable version of [t]. *)
let string_of_decision_tree t =
  let b = Buffer.create 13 in
  let offset = 2 in
  let show indent s =
    Buffer.add_string b (String.make (offset * indent) ' ' ^ s ^ "\n")
  in
  let rec aux prefix indent = function
  | Fail ->
     show indent (prefix ^ "fail")
  | Dup (i,t) ->
     aux ("dup" ^ string_of_int i ^ ":") (indent + 1) t
  | Leaf (bs, e) ->
     show indent (
         prefix
         ^ string_of_bindings bs
         ^ HobixPrettyPrinter.(to_string expression e))
  | Switch (occ, ts, default) ->
     show indent (prefix ^ string_of_path occ ^ "?");
     List.iter (fun (c, _, t) -> aux (string_of_constructor c) (indent + 1) t) ts;
     begin match default with
     | None -> ()
     | Some t -> aux "default: " (indent + 1) t
     end
  | Swap (i, t) ->
     aux ("swap" ^ string_of_int i ^ ":") (indent + 1) t
  in
  aux "" 0 t;
  Buffer.contents b

let rec is_wildcard = function
    | S.PWildcard | S.PVariable _ -> true
    | S.PTypeAnnotation _ -> assert false
    | S.PAnd xs -> List.for_all (fun v -> is_wildcard (value v)) xs
    | _ -> false

(** Tell if a line is full of wildcard *)
let is_wildcard_line ps = List.for_all is_wildcard ps

let get_first_not_wildcard xs =
  let rec aux i = function
    | [] -> assert false
    | x::xs -> if not (is_wildcard x) then i else aux (i+1) xs in
  aux 0 xs

let bind_row occs x bs =
  let aux acc o p =
    match p with
    | S.PVariable v ->
       let id = identifier (Position.value v) in
       replace_binding id o acc
    | S.PTypeAnnotation _ -> assert false
    | _ -> acc
  in List.fold_left2 aux bs occs x

(** If there is a PAnd in the first column, return its position and its arity. *)
let has_pand_in_first_col (m:matrix) : (int * int) option =
  let aux (acc,i) (bs,_,_) =
    match List.hd bs with
    | S.PAnd xs -> (Some (i, List.length xs), i+i)
    | _ -> (acc,i+1) in
  fst (List.fold_left aux (None,0) m)

let dup_first_col_with_i i arity (m:matrix) : matrix =
  let aux j (ps,bs,e) =
    let ps' =
      if i = j
      then
        match List.hd ps with
        | S.PAnd xs -> List.map value xs
        | _ -> assert false
      else ExtStd.List.repeat arity (List.hd ps)
    in (ps' @ List.tl ps,bs,e)
  in List.mapi aux m

(** [decision_tree_of_matrix m] returns a decision tree that
    implements [m] efficiently. *)
let decision_tree_of_matrix (m : matrix) : decision_tree =
  let rec cc (occs : int list list) m =
    match m with
    | [] -> Fail
    | (x,bs,e)::xs ->
       if is_wildcard_line x
       then
         let bs = bind_row occs x bs in
         Leaf (bs,e)
       else
         let i = get_first_not_wildcard x in
         if i <> 0
         then Swap (i,cc (swap_occurences i occs) (swap_columns i m))
         else
           match has_pand_in_first_col m with
           | Some (i,arity) ->
              let occs = ExtStd.List.repeat arity (List.hd occs) @ List.tl occs in
              Dup (arity,cc occs (dup_first_col_with_i i arity m))
           | None ->
              let sigma1 = head_constructors m in
              let ak =
                List.map (fun (c,i) ->
                    let newoccs = List.tl (split (i+1) (List.hd occs)) @ List.tl occs in
                    c,i,cc newoccs (specialize (List.hd occs) c i m)) sigma1 in
              let def = cc (List.tl occs) (default (List.hd occs) m) in
              Switch ((List.hd occs),ak,Some def)
  in cc [[]] m

let lint i = HobixAST.(Literal (LInt (Mint.of_int i)))

let get_block path x =
  List.fold_right (
      fun p acc -> T.ReadBlock (acc, T.Literal (T.LInt (Int64.of_int p)))
    ) path x

let update_stack i x =
  List.(tl (init (i+1) (fun i -> get_block [i] x)))

let is_lit xs =
  match xs with
  | (CLit _,_,_)::_ -> true
  | _ -> false

let pat_lit e x =
  match x with
  | CTag _ -> assert false
  | CLit l ->
     let l = literal l in
     is_equal l e (HobixAST.Literal l)

let cascade_tests default xs =
  let aux (x,v) e =
    HobixAST.IfThenElse (x,v,e)
  in List.fold_right aux xs default

(** [compile_decision_tree index_of_constructor x t] returns an
    expression in Hobix which corresponds to the application of [t] to
    [x]. [index_of_constructor k] returns the integer which represents
    the constructor k in Hobix. *)
let compile_decision_tree (index_of_constructor : S.constructor -> int) x t =
  let fail = HobixAST.(Apply (Variable (Id "`/`"), [ lint 42; lint 0 ])) in
  let extract_ctag x =
    match x with
    | CTag c -> index_of_constructor c
    | _ -> assert false in
  let scrut = T.Id "___scrutinee" in
  let rec compile_aux xs t =
    match t with
    | Fail ->
       fail
    | Swap (i, dt) ->
       compile_aux (swap_occurences i xs) dt
    | Dup (arity,dt) ->
       compile_aux (ExtStd.List.repeat arity (List.hd xs) @ List.tl xs) dt
    | Leaf (bs, e) ->
       List.fold_right (
           fun (b, p) acc ->
           let vdef = T.SimpleValue (b, get_block p (T.Variable scrut)) in
           T.Define (vdef, acc)
         ) bs e
    | Switch (_, dts, default) ->
       match xs with
       | [] -> assert false
       | idx::xs ->
          if is_lit dts
          then (* Go back to a list of ifthenelse with literals *)
            let default = ExtStd.Option.fold (fun dt _ -> compile_aux xs dt) default fail in
            let dts = List.map (fun (l,_,t) -> pat_lit (get_block [] idx) l, compile_aux xs t) dts in
            cascade_tests default dts
          else
            let default =
              ExtStd.Option.map (fun dt -> compile_aux xs dt) default in
            let maxi = List.fold_left max 0 (List.map (fun (x,_,_) -> extract_ctag x) dts) in
            let exprs = Array.make (maxi + 1) None in
            List.iter
              (fun (o,arity, t) ->
                let xs = update_stack arity idx @ xs in
                Array.set exprs (extract_ctag o) (Some (compile_aux xs t))) dts;
            T.Switch (get_block [0] idx, exprs, default)
  in
  T.Define (T.SimpleValue (scrut,x), compile_aux [T.Variable scrut] t)

(** Remove type annotation in the pattern [p] *)
let clear_annot p =
  let rec aux = function
  | S.PTypeAnnotation (p, _) ->
     let p = Position.value p in
     aux p
  | S.PTaggedValue (c, _, ps) ->
     S.PTaggedValue (c, None, clear_position_list ps)
  | S.PRecord (lp, _) ->
     S.PRecord (clear_record_list lp, None)
  | S.PTuple ps ->
     S.PTuple(clear_position_list ps)
  | S.PAnd ps ->
     S.PAnd (clear_position_list ps)
  | S.POr ps ->
     S.POr (clear_position_list ps)
  | p -> p
  and clear_position_list ps = List.map (fun p -> (Position.map aux p)) ps
  and clear_record_list ps = List.map (fun (l,p) -> (l, Position.map aux p)) ps
  in aux p

(** Remove type annotations in the matrix [m]. *)
let remove_annotation (m : matrix) =
  List.map (fun (ps, b, e) -> (List.map clear_annot ps, b, e)) m

(** [translate branches x] returns an [Hobix] expression which implements
    an efficient pattern matching of [x] with the [branches]. *)
let translate (index_of_constructor : S.constructor -> int) bs x =
  let matrix = List.map (fun (p, e) -> ([p.value], [], e)) bs in
  let matrix = remove_annotation matrix in
  let decision_tree = decision_tree_of_matrix matrix in
  compile_decision_tree index_of_constructor x decision_tree
