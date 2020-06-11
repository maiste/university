(** This module implements a compiler from Retrolix to X86-64 *)

(** In more details, this module performs the following tasks:
   - turning accesses to local variables and function parameters into stack
     loads and stores ;
   - generating initialization code and reserving space in the .data section for
     global variables ;
   - reserving space in the .data section for literal strings.
 *)

(* TODO tail recursion *)

let error ?(pos = Position.dummy) msg =
  Error.error "compilation" pos msg

(** As in any module that implements {!Compilers.Compiler}, the source
    language and the target language must be specified. *)
module Source = Retrolix
module Target = X86_64
module S = Source.AST
module T = Target.AST

module Str = struct type t = string let compare = Pervasives.compare end
module StrMap = Map.Make(Str)
module StrSet = Set.Make(Str)

(** {2 Low-level helpers} *)

let scratchr = X86_64_Architecture.scratch_register

let scratch = `Reg scratchr
let rsp = `Reg X86_64_Architecture.RSP
let rbp = `Reg X86_64_Architecture.RBP
let rdi = `Reg X86_64_Architecture.RDI

(** [align n b] returns the smallest multiple of [b] larger than [n]. *)
let align n b =
  let m = n mod b in
  if m = 0 then n else n + b - m

(** {2 Label mangling and generation} *)

let hash x = string_of_int (Hashtbl.hash x)

let label_for_string_id id =
  ".S_" ^ string_of_int id

let label_of_retrolix_label (s : string) =
  s

let label_of_function_identifier (S.FId s) =
  label_of_retrolix_label s

let data_label_of_global (S.Id s) =
  label_of_retrolix_label s

let init_label_of_global (xs : S.identifier list) =
  ".I_" ^ hash xs

let label_of_internal_label_id (id : T.label) =
  ".X_" ^ id

let fresh_label : unit -> T.label =
  let r = ref 0 in
  fun () -> incr r; label_of_internal_label_id (string_of_int !r)

let fresh_string_label : unit -> string =
  let r = ref 0 in
  fun () -> let n = !r in incr r; label_for_string_id n

(** {2 Environments} *)

type environment =
  {
    externals : S.FIdSet.t;
    (** All the external functions declared in the retrolix program. *)
    globals : S.IdSet.t;
    (** All the global variables found in the Retrolix program, each with a
        unique integer. *)
    data_lines : T.line list;
    (** All the lines to be added to the .data section of the complete file. *)
  }

let make_environment ~externals ~globals () =
  let open T in

  let data_lines =
    S.IdSet.fold
      (fun ((S.Id id_s) as id) lines ->
        Label (data_label_of_global id)
        :: Instruction (Comment id_s)
        :: Directive (Quad [Lit Mint.zero])
        :: lines
      )
      globals
      []
  in

  let data_lines =
    S.FIdSet.fold
      (fun (S.FId f) lines -> Directive (Extern f) :: lines)
      externals
      data_lines
  in

  {
    externals;
    globals;
    data_lines;
  }

let is_external env (f : S.rvalue) =
  match f with
  | `Immediate (S.LFun f) ->
     S.FIdSet.mem f env.externals
  | _ ->
     false

let is_global env f =
  S.IdSet.mem f env.globals

let register_string s env =
  let open T in
  let l = fresh_string_label () in
  l,
  { env with data_lines = Label l :: Directive (String s) :: env.data_lines; }

(* The following function is here to please Flap's architecture. *)
let initial_environment () =
  make_environment ~externals:S.FIdSet.empty ~globals:S.IdSet.empty ()

let register_globals global_set env =
  let open T in
  let globals, data_lines =
    S.IdSet.fold
      (fun ((S.Id id_s) as id) (globals, lines) ->
        S.IdSet.add id globals,
        Label (data_label_of_global id)
        :: Instruction (Comment id_s)
        :: Directive (Quad [Lit Mint.zero])
        :: lines
      )
      global_set
      (S.IdSet.empty, env.data_lines)
  in
  { env with globals; data_lines; }

(** {2 Abstract instruction selectors and calling conventions} *)

module type InstructionSelector =
  sig
    (** [mov ~dst ~srcl] generates the x86-64 assembly listing to copy
        [src] into [dst]. *)
    val mov : dst:T.dst -> src:T.src -> T.line list

    (** [add ~dst ~srcl ~srcr] generates the x86-64 assembly listing to store
        [srcl + srcr] into [dst]. *)
    val add : dst:T.dst -> srcl:T.src -> srcr:T.src -> T.line list

    (** [sub ~dst ~srcl ~srcr] generates the x86-64 assembly listing to store
        [srcl - srcr] into [dst]. *)
    val sub : dst:T.dst -> srcl:T.src -> srcr:T.src -> T.line list

    (** [mul ~dst ~srcl ~srcr] generates the x86-64 assembly listing to store
        [srcl * srcr] into [dst]. *)
    val mul : dst:T.dst -> srcl:T.src -> srcr:T.src -> T.line list

    (** [div ~dst ~srcl ~srcr] generates the x86-64 assembly listing to store
        [srcl / srcr] into [dst]. *)
    val div : dst:T.dst -> srcl:T.src -> srcr:T.src -> T.line list

    (** [andl ~dst ~srcl ~srcr] generates the x86-64 assembly listing to store
        [srcl & srcr] into [dst]. *)
    val andl : dst:T.dst -> srcl:T.src -> srcr:T.src -> T.line list

    (** [orl ~dst ~srcl ~srcr] generates the x86-64 assembly listing to store
        [srcl | srcr] into [dst]. *)
    val orl : dst:T.dst -> srcl:T.src -> srcr:T.src -> T.line list

    (** [conditional_jump ~cc ~srcl ~srcr ~ll ~lr] generates the x86-64 assembly
        listing to test whether [srcl, srcr] satisfies the relation described by
        [cc] and jump to [ll] if they do or to [lr] when they do not. *)
    val conditional_jump :
      cc:T.condcode ->
      srcl:T.src -> srcr:T.src ->
      ll:T.label -> lr:T.label ->
      T.line list

    (** [switch ~default ~discriminant ~cases] generates the x86-64 assembly
       listing to jump to [cases.(discriminant)], or to the (optional) [default]
       label when discriminant is larger than [Array.length cases].

       The behavior of the program is undefined if [discriminant < 0], or if
       [discriminant >= Array.length cases] and no [default] has been given. *)
    val switch :
      ?default:T.label ->
      discriminant:T.src ->
      cases:T.label array ->
      T.line list
  end

module type FrameManager =
  sig
    (** The abstract data structure holding the information necessary to
        implement the calling convention. *)
    type frame_descriptor

    (** Generate a frame descriptor for the function with parameter [params] and
        locals [locals]. *)
    val frame_descriptor :
      params:S.identifier list ->
      locals:S.identifier list ->
      frame_descriptor

    (** [location_of fd v] computes the address of [v] according to the frame
        descriptor [fd]. Note that [v] might be a local variable, a function
        parameter, or a global variable. *)
    val location_of : frame_descriptor -> S.identifier -> T.address

    (** [function_prologue fd] generates the x86-64 assembly listing to setup
        a stack frame according to the frame descriptor [fd]. *)
    val function_prologue : frame_descriptor -> T.line list

    (** [function_epilogue fd] generates the x86-64 assembly listing to setup a
        stack frame according to the frame descriptor [fd]. *)
    val function_epilogue : frame_descriptor -> T.line list

    (** [call fd ~kind ~f ~args] generates the x86-64 assembly listing to setup
        a call to the function at [f], with arguments [args], with [kind]
        specifying whether this should be a normal or tail call.  *)
    val call :
      frame_descriptor ->
      kind:[ `Normal | `Tail ] ->
      f:T.src ->
      args:T.src list ->
      T.line list
  end

(** {2 Code generator} *)

(** This module implements an x86-64 code generator for Retrolix using the
    provided [InstructionSelector] and [FrameManager].  *)
module Codegen(IS : InstructionSelector)(FM : FrameManager) =
  struct
    let translate_label (S.Label l) =
      label_of_retrolix_label l

    let translate_variable fd v =
      `Addr (FM.location_of fd v)

    let translate_literal lit env =
      match lit with
      | S.LInt i ->
         T.Lit i, env

      | S.LFun f ->
         T.Lab (label_of_function_identifier f), env

      | S.LChar c ->
         T.Lit (Mint.of_int @@ Char.code c), env

      | S.LString s ->
         let l, env = register_string s env in
         T.Lab l, env

    let translate_register (S.RId s) =
      X86_64_Architecture.register_of_string s

    let translate_lvalue fi lv =
      match lv with
      | `Variable v ->
         translate_variable fi v
      | `Register reg ->
         `Reg (translate_register reg)

    let translate_rvalue fi rv env =
      match rv with
      | `Immediate lit ->
         let lit, env = translate_literal lit env in
         `Imm lit, env
      | (`Variable _ | `Register _) as lv ->
         translate_lvalue fi lv, env

    let translate_rvalues fi rvs env =
      List.fold_right
        (fun rv (rvs, env) ->
          let rv, env = translate_rvalue fi rv env in
          rv :: rvs, env)
        rvs
        ([], env)

    let translate_label_to_operand (S.Label l) =
      `Imm (T.Lab l)

    let translate_cond cond =
      match cond with
      | S.GT -> T.G
      | S.LT -> T.L
      | S.GTE -> T.GE
      | S.LTE -> T.LE
      | S.EQ -> T.E

    let translate_instruction fd ins env : T.line list * environment  =
      let open T in
      let open X86_64_Architecture in
      begin match ins with
      | S.Call (f, args, is_tail) ->
         let kind = if is_tail then `Tail else `Normal in
         let f, env = translate_rvalue fd f env in
         let args, env = translate_rvalues fd args env in
         FM.call fd ~kind ~f ~args,
         env

      | S.Assign (dst, op, args) ->
         let dst = translate_lvalue fd dst in
         let args, env = translate_rvalues fd args env in
         let inss =
           match op, args with
           | S.Add, [ srcl; srcr; ] ->
              IS.add ~dst ~srcl ~srcr
           | S.Sub, [ srcl; srcr; ] ->
              IS.sub ~dst ~srcl ~srcr
           | S.Mul, [ srcl; srcr; ] ->
              IS.mul ~dst ~srcl ~srcr
           | S.Div, [ srcl; srcr; ] ->
              IS.div ~dst ~srcl ~srcr
           | S.And, [ srcl; srcr; ] ->
              IS.andl ~dst ~srcl ~srcr
           | S.Or, [ srcl; srcr; ] ->
              IS.orl ~dst ~srcl ~srcr
           | S.Copy, [ src; ] ->
              IS.mov ~dst ~src
           | _ ->
              error "Unknown operator or bad arity"
         in
         inss, env

      | S.Ret ->
         FM.function_epilogue fd @ insns [T.Ret],
         env

      | S.Jump l ->
         insns
           [
             T.jmpl (translate_label l);
           ],
         env

      | S.ConditionalJump (cond, args, ll, lr) ->
         let cc = translate_cond cond in
         let srcl, srcr, env =
           match args with
           | [ src1; src2; ] ->
              let src1, env = translate_rvalue fd src1 env in
              let src2, env = translate_rvalue fd src2 env in
              src1, src2, env
           | _ ->
              failwith "translate_exp: conditional jump with invalid arity"
         in
         IS.conditional_jump
           ~cc
           ~srcl ~srcr
           ~ll:(translate_label ll) ~lr:(translate_label lr),
         env

      | S.Switch (discriminant, cases, default) ->
         let discriminant, env = translate_rvalue fd discriminant env in
         let cases = Array.map translate_label cases in
         let default = ExtStd.Option.map translate_label default in
         IS.switch ?default ~discriminant ~cases,
         env

      | S.Comment s ->
         insns
           [
             Comment s;
           ],
         env

      | S.Exit ->
         IS.mov ~src:(liti 0) ~dst:rdi
         @ FM.call fd ~kind:`Normal ~f:(`Imm (Lab "exit")) ~args:[],
         env
      end


    let translate_labelled_instruction fi (body, env) (l, ins) =
      let ins, env = translate_instruction fi ins env in
      List.rev ins @ T.Label (translate_label l) :: body,
      env

    let translate_labelled_instructions fi env inss =
      let inss, env =
        List.fold_left (translate_labelled_instruction fi) ([], env) inss
      in
      List.rev inss, env

    let translate_fun_def ~name ?(desc = "") ~params ~locals gen_body =
      let open T in

      let fd = FM.frame_descriptor ~params ~locals in

      let prologue = FM.function_prologue fd in

      let body, env = gen_body fd in

      Directive (PadToAlign { pow = 3; fill = 0x90; }) :: Label name
      :: (if desc = ""
          then prologue
          else Instruction (Comment desc) :: prologue)
      @ body,
      env

    let translate_block ~name ?(desc = "") ~params (locals, body) env =
      translate_fun_def
        ~name
        ~desc
        ~params
        ~locals
        (fun fi -> translate_labelled_instructions fi env body)

    let translate_definition def (body, env) =
      match def with
      | S.DValues (xs, block) ->
         let ids = ExtPPrint.to_string RetrolixPrettyPrinter.identifiers xs in
         let name = init_label_of_global xs in
         let def, env =
           translate_block
             ~name
             ~desc:("Initializer for " ^ ids ^ ".")
             ~params:[]
             block
             env
         in
         def @ body, env

      | S.DFunction ((S.FId id) as f, params, block) ->
         let def, env =
           translate_block
             ~desc:("Retrolix function " ^ id ^ ".")
             ~name:(label_of_function_identifier f)
             ~params
             block
             env
         in
         def @ body, env

      | S.DExternalFunction (S.FId id) ->
         T.(Directive (Extern id)) :: body,
         env

    let generate_main env p =
      let open X86_64_Architecture in
      let open T in

      let body =
        List.rev
          [
            Directive (PadToAlign { pow = 3; fill = 0x90; });
            Label "main";
            Instruction (Comment "Program entry point.");
            Instruction (andq ~src:(liti (-16)) ~dst:rsp);
            Instruction (subq ~src:(`Imm (Lit 8L)) ~dst:rsp);
          ]
      in

      (* Call all initialization stubs *)
      let body =
        let call body def =
          match def with
          | S.DValues (ids, _) ->
             let l = init_label_of_global ids in
             Instruction (T.calld (Lab l)) :: body
          | S.DFunction _ | S.DExternalFunction _ ->
             body
        in
        List.fold_left call body p
      in

      let body =
        T.insns
          [
            T.calld (Lab "exit");
            T.movq ~src:(liti 0) ~dst:rdi;
          ]
        @ body
      in

      Directive (Global "main") :: List.rev body

    (** [translate p env] turns a Retrolix program into a X86-64 program. *)
    let rec translate (p : S.t) (env : environment) : T.t * environment =
      let env = register_globals (S.globals p) env in
      let pt, env = List.fold_right translate_definition p ([], env) in
      let main = generate_main env p in
      let p = T.data_section :: env.data_lines @ T.text_section :: main @ pt in
      T.remove_unused_labels p, env
  end

(** {2 Concrete instructions selectors and calling conventions} *)

module InstructionSelector : InstructionSelector =
  struct
    open T

    let r15 = `Reg X86_64_Architecture.R15

    let run_through_15 ~(dst : dst) f =
      Instruction (f ~dst:r15) ::
      (if dst = r15 then [] else
      [Instruction (movq ~src:r15 ~dst)])

    let mov ~(dst : dst) ~(src : src) =
      match src, dst with
      | `Reg x , `Reg y  when x = y -> []
      | `Addr x, `Addr y when x = y -> []
      | (`Imm _ | `Reg _), _ | _,`Reg _ -> [Instruction (movq ~src ~dst)]
      | _ -> run_through_15 ~dst (movq ~src)

    let mov_and_run_through_15 f ~dst ~srcl ~srcr =
      mov ~src:srcr ~dst:r15
      @ run_through_15 ~dst (f ~src:srcl)

    let add =
      mov_and_run_through_15 addq

    let sub ~dst ~srcl ~srcr =
      mov_and_run_through_15 subq ~dst ~srcl:srcr ~srcr:srcl

    let mul =
      mov_and_run_through_15 imulq

    let andl =
      mov_and_run_through_15 andq

    let orl =
      mov_and_run_through_15 orq

    (* Compare src2 to src1 *)
    let cmp ~src1 ~src2 =
      match src2 with
      | `Reg _ -> [Instruction (cmpq ~src1 ~src2)]
      | _ ->
         [ Instruction (movq ~src:src2 ~dst:r15)
         ; Instruction (cmpq ~src1 ~src2:r15)
         ]

    let div ~dst ~srcl ~srcr =
      let rax = `Reg X86_64_Architecture.RAX in
      mov ~src:srcl ~dst:rax
      @ mov ~src:srcr ~dst:r15
      @ [ Instruction cqto
        ; Instruction (idivq ~src:r15)]
      @ mov ~dst ~src:rax

    let jump_if_cmp ~cc ~srcl ~srcr ~ll =
      cmp ~src1:srcr ~src2:srcl
      @ [ Instruction (jccl ~cc ~tgt:ll) ]

    let conditional_jump ~cc ~srcl ~srcr ~ll ~lr =
      jump_if_cmp ~cc ~srcl ~srcr ~ll
      @ [ Instruction (jmpl ~tgt:lr) ]

    let switch ?default ~discriminant ~cases =
      let add_default =
        match default with
        | None -> []
        | Some ll ->
           jump_if_cmp
             ~cc:AE ~srcl:discriminant
             ~srcr:(`Imm (Lit (Mint.of_int (Array.length cases)))) ~ll in
      let lab_switch = fresh_label () in
      let source_addr =
        addr
          ~offset:(Lab lab_switch)
          ~base:R15
          ~scale:`Four
          ()
      in
      add_default
      @ mul ~srcl:discriminant ~srcr:(`Imm (Lit (Mint.of_int 8))) ~dst:r15
      @ mov ~src:(`Addr source_addr) ~dst:r15
      @ [ Instruction (jmpi ~tgt:r15)
        ; Label lab_switch
        ; Directive (Quad (List.map (fun x -> Lab x) (Array.to_list cases)))]
  end

module FrameManager(IS : InstructionSelector) : FrameManager =
  struct
    open T

    type frame_descriptor =
      {
        param_count : int;
        (** Number of parameters. *)
        locals_space : int;
        (** Amount of space dedicated to local variables in the stack frame. *)
        stack_map : Mint.t S.IdMap.t;
        (** Maps stack-allocated variable names to stack slots expressed as
            frame-pointer relative offsets. *)
      }

    (** [empty_frame fd] returns [true] if and only if the stack frame described
        by [fd] is empty. *)
    let empty_frame fd =
      fd.param_count = 0 && fd.locals_space = 0

    (** [stack_usage_after_prologue fd] returns the size, in bytes, of the stack
        space after the function prologue. *)
    let stack_usage_after_prologue fd =
      Mint.size_in_bytes
      + (if empty_frame fd then 0 else 1) * Mint.size_in_bytes
      + fd.locals_space

    let frame_descriptor ~params ~locals =
      let stack, _ =
        List.fold_left (
          fun (stack, count) id ->
            S.IdMap.add id (Mint.of_int count) stack, count - 8
          ) (S.IdMap.empty, - 8) locals
      in
      let stack, _ =
        List.fold_left (
          fun (stack, count) id ->
            S.IdMap.add id (Mint.of_int count) stack, count + 8
        ) (stack, 16) params
      in
      {
        param_count = List.length params ;
        locals_space = (List.length locals) * 8 ;
        stack_map = stack ;
      }

    let location_of fd id =
      match S.IdMap.find_opt id fd.stack_map with
      | Some o -> {
          offset = Some (Lit o) ;
          base = Some(RBP);
          idx = None ;
          scale=`One
      }
      | None -> {
          offset=Some (Lab (data_label_of_global id));
          base=Some (RIP);
          idx=None;
          scale=`One;
      }

    let function_prologue fd =
      let offset =
        if fd.locals_space <> 0 then
          [Instruction (subq ~src:(liti fd.locals_space) ~dst:rsp)]
        else []
      in
      [ Instruction (pushq ~src:rbp)
      ; Instruction (movq ~src:rsp ~dst:rbp)
      ] @ offset

    let function_epilogue fd =
      let offset =
        if fd.locals_space <> 0 then
          [Instruction (addq ~src:(liti fd.locals_space) ~dst:rsp)]
        else []
      in
       offset @ [Instruction (popq ~dst:rbp)]

      let get_stack_offset args =
        8 * (List.length args) |> T.liti

      let call fd ~kind ~f ~args =
        match kind with
        | `Tail when (List.length args <= 6) ->
            (* Appel recursif terminal : vide la pile et rappelle f *)
            function_epilogue fd @ [Instruction (jmpdi ~tgt:f)]
        | _ ->
           let args =
             List.rev_map (fun elt -> Instruction (pushq ~src:elt)) args in
           let maybe_align =
             if List.length args mod 2 = 1
             then None
             else Some (subq ~src:(liti 8) ~dst:rsp, addq ~src:(liti 8) ~dst:rsp) in
           let maybe_f_lst f = function
             | None  -> []
             | Some x -> [Instruction (f x)] in
           maybe_f_lst fst maybe_align
           @ args
           @ [ Instruction (calldi ~tgt:f);
               Instruction (addq ~src:(get_stack_offset args) ~dst:rsp)
             ]
           @ maybe_f_lst snd maybe_align

  end

module CG =
  Codegen(InstructionSelector)(FrameManager(InstructionSelector))

let translate = CG.translate
