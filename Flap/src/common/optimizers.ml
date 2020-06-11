(** Optimizers. *)

open Languages
open Compilers

(** An optimizer rewrites programs from a language to try
    to improve their efficiency.

    An optimizer has a name and its application is optional.
*)
module type Optimizer = sig

  val shortname : string

  val longname : string

  val activated : bool ref

  module Source : Language

  val translate : Source.ast -> Source.ast

end

let optimizers : (module Optimizer) list ref =
  ref []

let register (module O : Optimizer) =
  optimizers := (module O) :: !optimizers

let find_optimizers source =
  List.filter (fun (module O : Optimizer) ->
      !O.activated && O.Source.name = source
    ) !optimizers

let optimize
      (type t)
      (module Source : Language with type ast = t) (ast : t) =
  List.fold_left (fun ast (module O : Optimizer) ->
      (* Kids, do not do that at home. *)
      Obj.magic (O.translate (Obj.magic ast))
    ) ast (find_optimizers Source.name)

let optimizing_compiler (module C : Compiler) =
  (module struct

    module Source = C.Source
    module Target = C.Target

    type environment = C.environment
    let initial_environment = C.initial_environment

    let translate source env =
      let source = optimize (module Source) source in
      C.translate source env

  end : Compiler)
