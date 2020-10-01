(*
 * Chaboche - Marais
 * CALODS 2019
 *)

(* Module to convert to prism *)
module type PrismTraduction = sig

  (* Specify wether we double global variable modification or not *)
  val double : bool -> unit
    
  (* Convert a (file * ast) to a prism file *)
  val calods_to_prism : string -> Compiler.Ast.t -> unit

end

(* Implement the traduction *)
module Prism : PrismTraduction = struct

  open Compiler
  module Calods = Compile.Calods

  let double d =
    CalodsToPrism.double := d
    
  let calods_to_prism file ast =
    (* Create path where the .pm is written *)
    let (out_pm, out_pps) = File.path_to file in
    (* Convert Calods Ast to Prism Ast *)
    let (env, prism_ast) = CalodsToPrism.translate_calods ast in
    (* Convert Prism Ast to string *)
    let (prism_str, pps_str) = PrismToStr.tostr_pm env prism_ast in
    (* Write string in .pm *)
    File.write_str_in_file out_pm prism_str;
    File.write_str_in_file out_pps pps_str

end
