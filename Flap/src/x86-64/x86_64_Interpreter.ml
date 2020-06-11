(** This module implements the interpreter for X86-64 programs. *)

open Error
open X86_64_AST

let error msg =
  global_error "X86-64 execution" msg

type runtime = unit

type observable = {
    exit_status : Unix.process_status;
    stdout      : string;
    stderr      : string
}

let initial_runtime () = ()

let show_runtime runtime = ()

let evaluate runtime (ast : t) =
  (* 1. Generate a temporary .s file.
     2. Call gcc to generate an executable linked with runtime.o
     3. Execute this program, capturing its stdout/stderr
   *)
  let fn = Filename.temp_file "flap" ".s" in
  let oc = open_out fn in
  PPrint.ToChannel.compact oc (X86_64_PrettyPrinter.program ast);
  close_out oc;
  (),
  {
    exit_status = Unix.WEXITED 0;
    stdout = Printf.sprintf "Generated assembly file in %s\n" fn;
    stderr = "";
  }

let print_observable (runtime : runtime) (obs : observable) =
  Printf.sprintf
    "Process exited with status %s.\n\nSTDOUT:\n\n%s\nSTDERR:\n%s\n\n"
    (ExtStd.Unix.string_of_process_status obs.exit_status)
    obs.stdout
    obs.stderr
