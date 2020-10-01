module type C = sig
  type t
  type ast

  val create : ast -> t
  val init_env : t -> t
  val check : t -> unit
end



module Checker : C with type ast = Ast.program =
struct
  type ast = Ast.program
  type t = Ast.program

  type env = {
    types : (Ast.ty * Ast.value list) list ;
    procs : (Ast.name * Ast.ty list) list ;
  }

  let env = ref { types = [] ; procs = [] }


  let create ast = ast

  let init_env ast =
    let get_types acc d =
      let d = Position.value d in
      match d with
      | Ast.DefineType (t, vs) ->
          let vs = List.map Position.value vs in
          (t,vs)::acc
    in
    let get_procs acc = function
      | Ast.Process (n, args,_,_) ->
        let args = List.map (
          fun arg ->
            let (Ast.Arg (_, ty)) = Position.value arg in
            Position.value ty
        ) args
        in (n, args)::acc
    in
    let data, procs =
      match ast with
      | Ast.Program (Header (d,_), p, _, _, _) -> d, p
    in
    let types = List.fold_left get_types [] data in
    let procs = List.fold_left get_procs [] procs in
    begin
      env := { !env with types = types };
      env := { !env with procs = procs };
      ast
    end


  let check_existing_type value =
    let rec aux = function
      | [] ->
          Error.type_error_np value "known"
      | (_, values)::vs ->
          if List.mem value values then ()
          else aux vs
    in aux !env.types

  let check_existing_proc name =
    match List.assoc_opt name !env.procs with
    | Some _ -> ()
    | None ->
        Error.undifined_error "processus in pps" name

  let check_is_in_type ty value =
    match List.assoc_opt ty !env.types with
    | Some values ->
        if List.mem value values |> not then
          Error.type_error_np value ty
        else ()
    | None -> ()

  let check_proc_args name values =
    let rec right_types = function
      | [], [] -> ()
      | ty::tys, ty'::tys' ->
          check_is_in_type ty ty';
          right_types (tys, tys')
      | _ ->
          let error =
            Format.sprintf "with size %d" (List.length values)
          in Error.type_error_np "size" error
    in
    match List.assoc_opt name !env.procs with
    | None ->
        Error.undifined_error "pps" name
    | Some tys ->
        right_types (tys, values)


  module DecideSet = Set.Make(struct
    type t = bool list
    let compare = compare
  end)

  let translate_into_bool_list p =
    let to_bool = function
      | Ast.Exist _ -> true
      | Ast.Tauto _ -> false
    in
    List.map to_bool p

  let compare_pps p1 p2 =
    match p1, p2 with
    | Ast.Tauto n1, Ast.Tauto n2 -> compare n1 n2
    | Ast.Tauto n1, Ast.Exist (n2, _) -> compare n1 n2
    | Ast.Exist (n1, _), Ast.Tauto n2 -> compare n1 n2
    | Ast.Exist (n1,_ ), Ast.Exist (n2, _) -> compare n1 n2

  let check_in_set set p =
    if DecideSet.mem p set then
      Error.decide_error ()
    else ()


  let check_entry e =
    let rec aux = function
      | [] -> ()
      | (name, values)::args ->
          check_existing_proc name;
          check_proc_args name values;
          aux args
    in aux e

  let check_proc_decides p =
    let rec aux = function
      | [] -> ()
      | Ast.Exist (n,v) :: ps ->
          check_existing_proc n;
          check_existing_type v;
          aux ps
      | _::ps -> aux ps
    in aux p

  let check_output o =
    let rec aux set = function
      | [] -> ()
      | p::ps ->
          let p = List.sort compare_pps p in
          let p' = translate_into_bool_list p in
          check_in_set set p';
          check_proc_decides p;
          aux (DecideSet.add p' set) ps
    in aux DecideSet.empty o

  let check (Ast.Program (_, _, _, _, pps)) =
    let rec aux = function
      | [] -> ()
      | (e, o):: eos ->
          check_entry e;
          check_output o;
          aux eos
    in
    match pps with
    | None -> ()
    | Some pps -> aux pps

end
