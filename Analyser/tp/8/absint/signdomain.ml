open Whilery
open Lattices
open Lattice
open Absdomain


module Signs = Set.Make (struct
    type t = Sign.t
    let compare = compare
  end)

module SignAnalysis : Absdomain = struct
  (* SignAnalysis.t is a set of Sign, by construction it only
     contains Sign: Plus, Minus, Zero *)
  type t = Signs.t

  (* Top is the set with {Plus, Minus, Zero *)
  let top = Signs.of_list [Plus; Minus; Zero]
  (* Bot is the empty set *)
  let bot = Signs.empty

  (* Lub is the union between two set *)
  let lub t1 t2 = Signs.union t1 t2

  (* Glb is the intersection between two set *)
  let glb t1 t2 = Signs.inter t1 t2

  (* Leq is the subset of two set *)
  let leq t1 t2 = Signs.subset t1 t2

  (* String of SignAnalysis.t *)
  let to_string t =
    "{" ^ Signs.fold (fun s acc -> acc ^ " " ^ Sign.to_string s) t "" ^ " }"
  

  (* Apply f for each s' in t with s 

     example:
     * {Plus, Zero} Plus -> {Plus, Zero}
  *)
  let distribute (f: Sign.t -> Sign.t -> Sign.t) (t: t) (s: Sign.t) : t =
    Signs.fold (fun s' acc ->
        let new_s = f s s' in
        let new_s = match new_s with
          | Top -> top
          | x -> Signs.singleton x
        in
        Signs.union new_s acc
      ) t bot

  (* Apply f to each element in t1 on t2
     example:
     * {Plus, Zero} {Zero} -> {Zero} *)
  let distribute_all f t1 t2 =
    Signs.fold (fun s acc ->
        let new_t = distribute f t2 s in
        Signs.union new_t acc
      ) t1 bot

  (* We distribute Sign.Plus on every element from t1 on t2 *)
  let plus t1 t2 = distribute_all Sign.plus t1 t2

  (* We distribute Sign.Mult on every element from t1 on t2 *)
  let mult t1 t2 = distribute_all Sign.mult t1 t2

  (* We distribute Sign.Subt on every element from t1 on t2 *)
  let subt t1 t2 = distribute_all Sign.subt t1 t2

  (* By construction widen can not be called *)
  let widen t1 t2 =
    (* Signs.t is finite, widen is not called *)
    assert false

  (* Embed create a set with both types in a set *)
  let embed x y = Signs.of_list [Sign.create x; Sign.create y]

  (* Refine signs set to make sur the comparison r with n is true *)
  let filter r n signs =
    let vals = embed n n in (* embed with x=y returns a singleton *)
    let valn = Signs.choose vals in
    print_string ("[FILTER INTERVAL:] "^(to_string signs)^"\n");
    match r with
    | Csup ->
      print_string (("Condition to ensure  ")^(to_string signs)^" > "^(string_of_int n)^"\n");
      (* x > n *)
      begin
        let res =
          match valn with
          (* If valn is Plus, signs must be refined to {Plus} *)
          | Plus -> Signs.inter vals signs
          (* Every sign may valid Csup with Minus, no need to refine *)
          | Minus -> signs
          (* If valn is Zero, signs must be refined to {Plus} *) 
          | Zero -> Signs.inter (Signs.singleton Plus) signs
          (* Embed can't create a singleton with Bot or Top *)
          | _ -> assert false
        in
        print_string ("RESULT OF FILTER:"^(to_string res)^"\n");
        res
      end

    | Cinfeq ->
      print_string (("Condition to ensure  ")^(to_string signs)^" <= "^(string_of_int n)^"\n");
      (* x <= n *)
      begin
        let res =
          match valn with
          (* Evey sign may valid Cinfeq with Plus, no need to refine *)
          | Plus -> signs
          (* If valn is Minus, signs must be refined to {Minus} *)
          | Minus -> Signs.inter vals signs
          (* If valn is Zero, signs must be refined to {Minus, Zero} *)
          | Zero -> Signs.inter (Signs.of_list [Minus; Zero]) signs
          (* Embed can't create a singleton with Bot or Top *)
          | _ -> assert false
        in
        print_string ("RESULT OF FILTER:"^(to_string res)^"\n");
        res
      end
end
