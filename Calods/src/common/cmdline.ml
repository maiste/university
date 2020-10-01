(*
 * Chaboche - Marais
 * CALODS 2019
 *)

(** Module to manage cmdline entries *)

open Cmdliner
open Compiler
open Graph
open Prism

module Calods = Compile.Calods
module IlodsPrinter = Graph.IlodsPrettyPrinter
module IlodsCompiler = Graph.CalodsToIlods

(* Man description *)
let name = "calods"
let version = "0.1"
let licence = "Licence MIT"
let description = "a simple language to describe distributed algorithms and check their behaviour."
let homepage = "https://gaufre.informatique.univ-paris-diderot.fr/maiste/chaboche-marais-plong-1920"
let bug_report = "https://gaufre.informatique.univ-paris-diderot.fr/maiste/chaboche-marais-plong-1920/issues"

let man = [
  `S Manpage.s_bugs ;
  `P ("Report bugs : " ^ bug_report);
  `S Manpage.s_authors ;
  `P "Valentin Chaboche <valentin.chb@gmail.com>" ;
  `Noblank ;
  `P "Etienne Marais <etiennemarais@maiste.fr>" ;
  `S "CONTRIBUTION" ;
  `P ("$(b, Licence): " ^ licence) ;
  `P ("$(b, Homepage): " ^ homepage) ;
]

let fetch_expand () =
  let doc = "Expand forall declaration in main." in
  Arg.(value & flag & info ["f"; "forall"] ~doc)

(* Create ast from file .cds *)
let convert_file_to_ast cds_file sch_file pps_file ?(expand=true) () =
  let ast = Calods.parse_filenames cds_file sch_file pps_file in
  if expand then
    Calods.expand_forall ast
  else
    ast

(* Display an ast *)
let display_ast cds_file sch_file pps_file =
    try
      let ast = convert_file_to_ast cds_file sch_file pps_file ~expand:false () in
      Calods.check ast;
      Ok (Calods.print_ast ast)
    with e -> Error.exception_to_cmd e

let generate_graph verbose ast_verbose cds_file sch_file =
  try
    let ast = convert_file_to_ast cds_file sch_file "" () in
    Calods.check ast ;
    let ast = IlodsCompiler.translate_prog ast in
    if ast_verbose then
      IlodsPrinter.print_ast ast
    else ();
    Ok (StatesGenerator.generate_all verbose ast)
  with e -> Error.exception_to_cmd e



(** Commands **)

(* Call display ast *)
let ast =
  let doc = "Display the ast extracted from the file." in
  let exits = Term.default_exits in
  let cds_file =
    let doc = "Source file." in
     Arg.(required & pos 0 (some string) None & info [] ~doc ~docv:"SRC")
  in
  let sch_file =
    let doc = "Scheduler file." in
     Arg.(value & opt string "" & info ["sch"; "scheduler"] ~doc ~docv:"SCH")
  in
  let pps_file =
    let doc = "Properties file." in
    Arg.(value & opt string "" & info ["pps"; "properties"] ~doc ~docv:"PPS")
  in
  Term.(term_result (const display_ast $ cds_file $ sch_file $ pps_file)),
  Term.info "ast" ~version ~doc ~exits ~man


(* Generate a graph *)
let graph =
  let doc = "Create .dot file(s) containing state graph(s) from the file SRC." in
  let exits = Term.default_exits in
  let cds_file =
    let doc = "Source file." in
    Arg.(required & pos 0 (some string) None & info [] ~doc ~docv:"SRC")
  in
  let sch_file =
    let doc = "Scheduler file." in
     Arg.(value & opt string "" & info ["sch"; "scheduler"] ~doc ~docv:"SCH")
  in
  let verbose =
    let doc = "Print environment evolution during graph generation." in
    Arg.(value & flag & info ["v"; "verbose"] ~doc)
  in
  let ast =
    let doc = "Print the ilods version of the program." in
     Arg.(value & flag & info ["a"; "ast"] ~doc)
  in
  Term.(term_result (const generate_graph $ verbose $ ast $ cds_file $ sch_file)),
  Term.info "graph" ~version ~doc ~exits ~man



(* Conver to Prism *)
let convert_prism expand d cds_file sch_file pps_file =
  try
    let ast = convert_file_to_ast cds_file sch_file pps_file ~expand:expand () in
    let _ = Calods.check ast in
    let d = not d in
    Prism.double d;
    Ok (Prism.calods_to_prism cds_file ast)
  with e -> Error.exception_to_cmd e

(* Call convert Prism *)
let prism =
  let doc = "Convert the ast extracted from the file to a .prism" in
  let exits = Term.default_exits in
  let cds_file =
    let doc = "Source file." in
    Arg.(required & pos 0 (some string) None & info [] ~doc ~docv:"SOURCE")
  in
  let sch_file =
    let doc = "Scheduler file." in
     Arg.(value & opt string "" & info ["sch"; "scheduler"] ~doc ~docv:"SCH")
  in
  let pps_file =
    let doc = "Properties file." in
    Arg.(value & opt string "" & info ["pps"; "properties"] ~doc ~docv:"PPS")
  in
  let double =
    let doc = "Double global variable modification. Use this flag to disable this option." in
    Arg.(value & flag & info ["d"; "double"] ~doc ~docv:"DOUBLE")
  in
  let expand = fetch_expand () in
  Term.(term_result (const convert_prism $ expand $ double $ cds_file $ sch_file $ pps_file)),
  Term.info "prism" ~version ~doc ~exits ~man

(* Default command *)
let default =
  let exits = Term.default_exits in
  Term.(ret (const (`Help (`Pager, None)))),
  Term.info name ~version ~doc:description ~exits ~man


(* List of usable commands *)
let cmds = [
    ast ;
    graph ;
    prism ;
]

(* Run cmdliner <=> main *)
let execute () =
  Term.(exit ~term_err:1 @@ eval_choice default cmds)
