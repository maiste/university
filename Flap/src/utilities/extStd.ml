(** This module extends some modules of the standard library. *)

module Ref = struct

  let as_functions default =
    let r = ref default in
    (fun x -> r := x), (fun () -> !r)

end

module List = struct

  include List

  exception EmptyListHasNoMin
  let min_assoc_list xs =
    let rec aux k' v' = function
      | [] ->
         (k', v')
      | (k, v) :: xs ->
         if v < v' then aux k v xs else aux k' v' xs
    in
    match xs with
    | [] -> raise EmptyListHasNoMin
    | (k, v) :: xs -> aux k v xs

  exception InvalidSwap

  let rec swap i x' xs =
    match i, xs with
    | 0, x :: xs ->
       x, x' :: xs
    | n, x :: xs ->
       let y, xs' = swap (i - 1) x' xs in
       y, x :: xs'
    | _, _ ->
       raise InvalidSwap

  let rec range start stop =
    if stop < start then [] else start :: range (start + 1) stop

  let asymmetric_map2 f =
    let rec aux accu xs ys =
      match xs, ys with
        | xs, [] ->
          (List.rev accu, xs, [])
        | [], ys ->
          (List.rev accu, [], ys)
        | x :: xs, y :: ys ->
          aux (f x y :: accu) xs ys
    in
    aux []

  let repeat k v =
    let rec aux accu k =
      if k = 0 then accu else aux (v :: accu) (k - 1)
    in
    aux [] k

  let repeatf k f =
    let rec aux accu k =
      if k = 0 then accu else aux (f () :: accu) (k - 1)
    in
    aux [] k

  let rec uniq = function
    | [] -> []
    | [x] -> [x]
    | x :: ((y :: _) as xs) -> if x = y then uniq xs else x :: uniq xs

  (** [index_of p l] returns the index of the first element [x] of [l]
      such [p x = true]. Raise [Not_found] otherwise. *)
  let index_of : ('a -> bool) -> 'a list -> int =
    fun p l ->
      let rec aux i = function
        | [] -> raise Not_found
        | x :: xs -> if p x then i else aux (succ i) xs
      in
      aux 0 l

   (** [all_distinct ls] returns true if all the elements of [ls]
       are distinct. *)
  let all_distinct ls =
    let ls = List.sort compare ls in
    let rec aux = function
      | [] | [_] -> true
      | x :: y :: ys -> x <> y && aux (y :: ys)
    in
    aux ls

  let all_equal ls =
    let rec aux = function
      | [] | [_] -> true
      | x :: y :: ys -> x = y && aux (y :: ys)
    in
    aux ls

  let transpose xs =
    assert (all_equal (List.map length xs));
    let rec aux rows =
      match rows with
      | [] -> assert false
      | [] :: _ -> []
      | rows ->
         List.(
          let row', rows = map (fun l -> (hd l, tl l)) rows |> split in
          row' :: aux rows
         )
    in
    aux xs

  let unique_value ls =
    match uniq ls with
      | [x] -> Some x
      | _ -> None

  let foldmap f init =
    let rec aux (accu, ys) = function
      | [] ->
        (accu, List.rev ys)
      | x :: xs ->
        let accu, y = f accu x in
        aux (accu, y :: ys) xs
    in
    aux (init, [])

  exception FoldMap2

  let foldmap2 f init l1 l2 =
    let rec aux (accu, ys) = function
      | [], [] ->
        (accu, List.rev ys)
      | x :: xs, z :: zs ->
        let accu, y = f accu x z in
        aux (accu, y :: ys) (xs, zs)
      | _, _ ->
         raise FoldMap2
    in
    aux (init, []) (l1, l2)

  let update_assoc k v l =
    let rec aux = function
      | [] -> [(k, v)]
      | ((k', v') as x) :: l -> if k = k' then (k, v) :: l else x :: aux l
    in
    aux l

  module Monad : sig
      type 'a t
      val return : 'a -> 'a t
      val ( >>= ) : 'a t -> ('a -> 'b t) -> 'b t
      val take_one : 'a list -> 'a t
      val fail : 'a t
      val and_try : 'a t -> 'a t -> 'a t
      val run : 'a t -> 'a list
  end = struct
      type 'a t = 'a list
      let return x = [x]
      let ( >>= ) x f = List.(flatten (map f x))
      let fail = []
      let and_try a b = a @ b
      let run x = x
      let take_one x = x
  end

  let last l = List.(hd (rev l))

end

let update
    (find : 'k -> 'c -> 'v)
    (add : 'k -> 'v -> 'c -> 'c)
    (k : 'k) (m : 'c)
    (default : 'v)
    (f : 'v -> 'v)
: 'c =
  try
    let v = find k m in
    add k (f v) m
  with Not_found ->
    add k (f default) m

module Random = struct

  let int_in_range start stop =
    start + Random.int (stop - start + 1)

end

module Option = struct

  let map f = function
    | None -> None
    | Some x -> Some (f x)

  let iter f = function
    | None -> ()
    | Some x -> f x

  let fold f x acc =
    match x with
    | None -> acc
    | Some x -> f x acc

end

module Pervasives = struct

  let ( |< ) default e x =
    try e x with _ -> default

  let file_content filename =
    let cin = open_in filename in
    let b = Buffer.create 24 in
    let rec read () =
      try Buffer.add_channel b cin 1; read () with End_of_file -> ()
    in
    read ();
    close_in cin;
    Buffer.contents b

end

module Buffer = struct
  include Buffer

  let slurp ?(buffer_size = 4096) ic =
    let b = Buffer.create buffer_size in
    let rec loop () =
      match Buffer.add_channel b ic buffer_size with
      | () ->
         loop ()
      | exception _ ->
         b
    in
    loop ()

end

module Unix = struct

  open Unix

  let output_and_error_of_command ?(env = Unix.environment ()) cmd =
    let cin, cout, cerr = open_process_full cmd env in
    let stdin = Buffer.slurp cin in
    let stderr = Buffer.slurp cerr in
    let status = close_process_full (cin, cout, cerr) in
    status, Buffer.contents stdin, Buffer.contents stderr

  let output_of_command cmd =
    let status, stdin, _ = output_and_error_of_command cmd in
    status, stdin

  let string_of_process_status = function
    | WEXITED k -> Printf.sprintf "exited(%d)" k
    | WSTOPPED k -> Printf.sprintf "stopped(%d)" k
    | WSIGNALED k -> Printf.sprintf "signaled(%d)" k

  let add_exec_bits filename =
    let st = stat filename in
    chmod filename (st.st_perm lor 0o111)
end

module Hashtbl = struct

  let counting_table (type a) () : (a -> int) * (a -> unit) =
    let t = Hashtbl.create 13 in
    let get k = try Hashtbl.find t k with Not_found -> 0 in
    let incr k = Hashtbl.replace t k (get k + 1) in
    (get, incr)

end

module Array = struct

  let present_to_list a =
    List.(rev (fold_left (fun accu -> function
                   | None -> accu
                   | Some t -> t :: accu) [] (Array.to_list a)))

end

module Pair = struct

  let swap (x, y) = (y, x)

end

module type PrintableType = sig
  type t
  val print : t -> PPrint.document
end

module type OrderedPrintableType = sig
  include Map.OrderedType
  include PrintableType with type t := t
end

module OrderedPrintablePairs (T : OrderedPrintableType) = struct
  type t = T.t * T.t

  let compare (x1, y1) (x2, y2) =
    let n = T.compare x1 x2 in
    if n <> 0 then n else T.compare y1 y2

  let print (x, y) = PPrint.OCaml.tuple [T.print x; T.print y]
end

module Set (T : OrderedPrintableType) =
  struct
    module M = Set.Make(T)
    include M
    let print s =
      let open PPrint in
      surround_separate_map 2 1
        (string "{}")
        (string "{")
        (string "," ^^ break 1)
        (string "}")
        T.print
        (List.of_seq @@ M.to_seq s)
  end

module Map (T : OrderedPrintableType) =
  struct
    module M = Map.Make(T)
    include M
    let print value m =
      let open PPrint in
      let pp (k, v) = prefix 2 1 (T.print k ^^ string " =") (value v) in
      surround_separate_map 2 1
        (string "{}")
        (string "{")
        (string "," ^^ break 1)
        (string "}")
        pp
        (List.of_seq @@ M.to_seq m)
  end
