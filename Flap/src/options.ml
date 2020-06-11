(** Options *)

open ExtStd
open ExtStd.Pervasives

let error msg =
  Error.global_error
    "during analysis of options"
    msg

let make_string_option what kind =
  let language = ref "" in
  let get () =
    if !language = "" then
      error (Printf.sprintf "You should specify the %s %s using '--%s'."
               kind what kind);
    !language
  in
  let set = ( := ) language in
  let is_set () = !language <> "" in
  get, set, is_set

let (get_source_language, set_source_language, is_source_language_set) =
  make_string_option "language" "source"

let (get_target_language, set_target_language, is_target_language_set) =
  make_string_option "language" "target"

type mode = Interactive | Batch

let mode = ref Batch

let set_mode = ( := ) mode

let get_mode () = !mode

let (get_input_filename, set_input_filename, is_input_filename_set) =
  make_string_option "filename" "input"

let using : string list ref = ref []
let insert_using x = using := x :: !using
let get_using () = !using

let set_interactive_mode = function
  | true -> set_mode Interactive
  | false -> set_mode Batch

let set_running_mode, get_running_mode = Ref.as_functions false
let set_verbose_mode, get_verbose_mode = Ref.as_functions false
let set_dry_mode, get_dry_mode         = Ref.as_functions false
let set_benchmark, get_benchmark       = Ref.as_functions false
let set_unsafe, get_unsafe             = Ref.as_functions false
let set_show_types, get_show_types     = Ref.as_functions false
let set_infer_types, get_infer_types   = Ref.as_functions false
let set_check_types, get_check_types   = Ref.as_functions true
let set_verbose_eval, get_verbose_eval = Ref.as_functions false
let set_use_sexp_in, get_use_sexp_in   = Ref.as_functions false
let set_use_sexp_out, get_use_sexp_out = Ref.as_functions false
let set_scripts_dir, get_scripts_dir   = Ref.as_functions "/bin"
let set_include_dir, get_include_dir   = Ref.as_functions "/usr/include"
let set_output_file, get_output_file   = Ref.as_functions ""
let set_fast_match, get_fast_match     = Ref.as_functions false
let set_backend, get_backend           = Ref.as_functions "x86-64"
let set_regalloc, get_regalloc         = Ref.as_functions "naive"
let set_debug_mode, get_debug_mode     = Ref.as_functions false

let get_architecture () : (module Architecture.S) =
  match get_backend () with
  | "x86-64" -> (module X86_64_Architecture)
  | s -> error (Printf.sprintf "`%s' is not a valid architecture." s)

type regalloc_variant = Naive | Realistic

let get_regalloc_variant () =
  match get_regalloc () with
    | "naive" -> Naive
    | "realistic"  -> Realistic
    | s -> error (
      Printf.sprintf "`%s' is not a valid register allocation strategy." s
    )
