open RetrolixAST
open RetrolixUtils

let activated = ref false

module Source = Retrolix

let shortname = "cf"

let longname = "constant folding"

(** {2 The Analysis Itself} *)

module ConstantDomain =
  struct
    let global_variables = ref []

    module D =
      struct
        type t =
          | Bot
          | Const of RetrolixAST.literal
          | Top

        let print x =
          match x with
          | Bot ->
             PPrint.string "Bot"
          | Const l ->
             RetrolixPrettyPrinter.literal l
          | Top ->
             PPrint.string "Top"

        let equal =
          Stdlib.(=)

        let compare =
          Stdlib.compare

        let le x y =
          match x,y with
          | Bot,_ -> true
          | Const x,Top -> true
          | Top,Top -> true
          | _ -> false

        let bot =
          Bot

        let lub x y =
          match x with
          | Bot -> y
          | Top -> x
          | Const x' ->
             match y with
             | Bot -> x
             | Const y' ->
                if x' = y'
                then x
                else Top
             | Top -> y
      end

    module DV = RetrolixDataflowUtils.PerLValueProperty(D)
    include DV

    let caller_save =
      X86_64_Architecture.(List.map string_of_register caller_saved_registers)

    (* This function sets to [Top] every lvalue that may have been modified by
       an opaque function call: caller-saved registers, global variables. *)
    let clobber_registers_and_globals x =
      let aux x y =
        match x with
        | `Register (RId x) ->
           if List.mem x caller_save then D.Top else y
        | `Variable x ->
           if List.mem x !global_variables then D.Top else y in
      LValueMap.mapi aux x

    let transfer_rval x rv =
      match rv with
      | `Immediate imm -> D.Const imm
      | #lvalue as lv ->
         match LValueMap.find_opt lv x with
         | Some x -> x
         | None -> D.Top (* This handles the case of function parameter *)

    let calcop = function
      | Copy -> assert false
      | Add | Or -> Mint.add
      | Mul | And -> Mint.mul
      | Div -> Mint.div
      | Sub -> Mint.sub

    let op_lit f x y =
      match x,y with
      | LInt x, LInt y -> D.Const (LInt (f x y))
      | _ -> D.Bot

    let transfer (lab, insn) x =
      match insn with
      | Comment _ | Ret | Jump _ | Exit | ConditionalJump _ | Switch _ ->
         x
      | Assign (lval, Copy, [rval]) ->
         LValueMap.add lval (transfer_rval x rval) x
      | Assign (lval, op, [r1;r2]) ->
         let res = match op,(transfer_rval x r1, transfer_rval x r2) with
           | _,(Bot,_ | _,Bot) -> D.Bot
           | (Mul | And),(Const (LInt 0L),_ | _, Const (LInt 0L)) -> Const (LInt 0L)
           | Div,(_,Const (LInt 0L)) -> D.Top (* Division par 0. *)
           | Div,(Const (LInt 0L),_) -> Const (LInt 0L)
           | _,(Top,_ | _,Top) -> D.Top
           | _,(Const x, Const y) -> op_lit (calcop op) x y in
         LValueMap.add lval res x
      | Call _ ->
         clobber_registers_and_globals x
      | _ -> assert false
  end

module ConstantAnalysis = RetrolixDataflowEngines.Default(ConstantDomain)

(** {2 Putting Everything Together} *)

let error lab msg =
  Printf.eprintf "%sundefined behavior (%s)\n"
    (ExtPPrint.to_string (RetrolixPrettyPrinter.label 0) lab)
    msg;
  exit 1

let analyze ((locals, _) as blocks) =
  let open ConstantDomain in
  let set_all v x xs =
    List.fold_left
      (fun acc x -> let x = `Variable x in LValueMap.add x ConstantDomain.D.Bot acc) x xs in
  let init = set_all D.Bot LValueMap.empty locals in
  let init = set_all D.Top init !ConstantDomain.global_variables in
  let init = `Input init in
  let direction = `Forward in
  ConstantAnalysis.analyze ~init ~direction blocks

let cmp = function
  | GT -> ( > )
  | LT -> ( < )
  | GTE -> ( >= )
  | LTE -> ( <= )
  | EQ -> ( = )

let rewrite sol (lab, insn) =
  let open ConstantDomain in
  let _,r = sol lab in
  match insn with
  | Comment _ | Ret | Jump _ | Exit | Call _ -> insn
  | Switch (x,ys,y) ->
     let x = transfer_rval r x in
     begin match x with
     | D.Const (LInt x) -> begin
         try Jump (Array.get ys (Mint.to_int x))
         with
           Invalid_argument _ ->
            match y with
            | Some l -> Jump l
            | None -> failwith "Constant switch to an unknown destination" end
     | _ -> insn end
  | ConditionalJump (cond,[x1;x2],lt,lf) ->
     let x1' = transfer_rval r x1 in
     let x2' = transfer_rval r x2 in
     begin match x1',x2' with
     | D.Const x, D.Const y ->
        if cmp cond x y
        then Jump lt
        else Jump lf
     | D.Const x,_ ->
        ConditionalJump (cond,[`Immediate x;x2],lt,lf)
     | _,D.Const y ->
        ConditionalJump (cond,[x1;`Immediate y],lt,lf)
     | _ -> insn end
  | Assign (lval, _, _) ->
     begin match LValueMap.find lval r with
     | D.Const x -> Assign (lval, Copy, [`Immediate x])
     | _ -> insn end
  | _ -> assert false

let translate p =
  ConstantDomain.global_variables := RetrolixUtils.global_variables p;
  RetrolixUtils.transform_blocks analyze rewrite p
