(*
 * Chaboche - Marais
 * CALODS -2019
 *)

module type PID = sig
  type t
  val create : unit -> t
  val clear_pid : t -> unit
  val new_pid : t -> string -> string
  val get_pid : t -> string -> string
  val show : t -> unit
end

module Pids : PID = struct
  type t = (string, string) Hashtbl.t

  let count = ref 1
  let fresh_pid t p =
    if Hashtbl.mem t p then
      begin
        count := !count + 1;
        Format.sprintf "%s_%d" p !count
      end
    else p


  let create () = Hashtbl.create 127

  let clear_pid t =
    Hashtbl.clear t

  let new_pid t p =
    let pid = fresh_pid t p in
    Hashtbl.add t pid p; pid

  let get_pid t pid =
    Hashtbl.find t pid

  let show t =
    Hashtbl.iter (fun k v -> Format.printf "%s -> %s\n" k v) t
end
