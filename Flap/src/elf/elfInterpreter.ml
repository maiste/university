(** This module implements the interpreter for X86-64 programs. *)

open Error
open ElfAST

type runtime = unit

type observable = {
    exit_status : Unix.process_status;
    stdout      : string;
    stderr      : string;
}

let initial_runtime () = ()

let show_runtime runtime = ()

let evaluate (_ : runtime) (buf : t) =
  (* 1. Generate a temporary .s file.
     2. Call gcc to generate an executable linked with runtime.o
     3. Execute this program, capturing its stdout/stderr
   *)
  let fn = Filename.chop_extension (Options.get_input_filename ()) ^ ".elf" in
  let oc = open_out fn in
  Buffer.output_buffer oc buf;
  close_out oc;
  ExtStd.Unix.add_exec_bits fn;
  let exit_status, stdout, stderr =
    ExtStd.Unix.output_and_error_of_command ("./" ^ fn)
  in
  (), { exit_status; stdout; stderr; }

let print_observable (runtime : runtime) (obs : observable) =
  Printf.sprintf
    "Process exited with status %s.\nSTDOUT:\n%s\nSTDERR:\n%s\n\n"
    (ExtStd.Unix.string_of_process_status obs.exit_status)
    obs.stdout
    obs.stderr
