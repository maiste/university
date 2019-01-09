(* Code extrait de:
   SAT-MICRO: petit mais costaud !
   par Sylvain Conchon, Johannes Kanig, Stéphane Lescuyer
*)

module type VARIABLES = sig
  type t
  val compare : t -> t -> int
end

module Make (V : VARIABLES) = struct

  type literal = bool * V.t

  module L = struct
    type t = literal
    let compare (b1,v1) (b2,v2) =
      let r = compare b1 b2 in
      if r = 0 then compare v1 v2
      else r

    let mk_not (b,v) = (not b, v)
  end

  module S = Set.Make(L)

  exception Unsat
  exception Sat of S.t

  type t = { gamma : S.t ; delta : L.t list list }

  let rec assume env f =
    if S.mem f env.gamma then env
    else bcp { gamma = S.add f env.gamma ; delta = env.delta }

  and bcp env =
    List.fold_left
      (fun env l -> try
          let l = List.filter
              (fun f ->
                 if S.mem f env.gamma then raise Exit;
                 not (S.mem (L.mk_not f) env.gamma)
              ) l
          in
          match l with
          | [] -> raise Unsat (* conflict *)
          | [f] -> assume env f
          | _ -> { env with delta = l :: env.delta }
        with Exit -> env)
      { env with delta = [] }
      env.delta

  let rec unsat env = try
      match env.delta with
      | [] -> raise (Sat env.gamma)
      | ([_] | []) :: _ -> assert false
      | (a :: _ ) :: _ ->
        begin try unsat (assume env a) with Unsat -> () end ;
        unsat (assume env (L.mk_not a))
    with Unsat -> ()

  let solve delta = try
      unsat (bcp { gamma = S.empty ; delta }) ; None
    with
    | Sat g -> Some (S.elements g)
    | Unsat -> None

end
