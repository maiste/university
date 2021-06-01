(*

PROJECT part 1.1:

Use (and adapt) the declaration of the generic framework,
that is the interface of the modules MF and BVF, 
to define the INTRAprocedural part of the

- RD: reaching definition analysis      >= 1pt
- AE: available expression analysis     >= 2pt
- CP: constant propagation analysis     >= 2pt
- sign analysis (Exercice 2.14 [NNH])   >= 3pt
- examples and documentation:           >= 2pt

*)



open Whilennh
open Auxiliary
open Mf
open Mfsolver
open Graph


(* Here are two WHILE programs that you can use to test your functions *)

let prog1 = Seq (Ass ('x', Cnt 5, 1),
               Seq (Ass ('y', Cnt 1, 2),
                    While (Gt (Var 'x', Cnt 1), 3,
                                Seq (Ass ('y', Mult (Var 'x', Var 'y'), 4),
                                     Ass ('x', Minus (Var 'x', Cnt 1), 5)))))


let prog2 = Seq (Ass ('x', Plus (Var 'z', Var 'b'), 1),
               Seq (Ass ('y', Mult (Var 'z', Var 'b'), 2),
                    While (Gt (Var 'y', Plus (Var 'z', Var 'b')), 3,
                           Seq (Ass ('z', Plus (Var 'z', Cnt 1), 4),
                                Ass ('x', Plus (Var 'z', Var 'b'), 5)))))

let prog3 =
  If (
    Gt (Var 'a', Var 'b'),
    1,
    Seq (
      Ass ('x', Minus (Var 'b', Var 'a'), 2),
      Ass ('y', Minus (Var 'a', Var 'b'), 3)
    ),
    Seq (
      Ass ('y', Minus (Var 'b', Var 'a'), 4),
      Ass ('x', Minus (Var 'a', Var 'b'), 5)
    )
  )

let prog4 =
  Seq (
    Ass ('x', Cnt 2, 1),
    Seq (
      Ass ('y', Cnt 4, 2),
      Seq (
        Ass ('x', Cnt 1, 3),
        Seq (
          If (
            Gt (Var 'y', Var 'x'),
            4,
            Ass ('z', Var 'y', 5),
            Ass ('z', Mult (Var 'y', Var 'y'), 6)
          ),
          Ass ('x', Var 'z', 7)
        )
      )
    )
  )

let prog5 =
  Seq (
    Ass ('x', Cnt (-3), 1),
    Seq (
      Ass ('y', Cnt (-4), 2),
      If (
        Gt (Var 'y', Var 'x'),
        3,
        Ass('z', Mult (Var 'x', Var 'y'), 4),
        Ass('z', Plus (Var 'x', Var 'y'),5)
      )
    )
  )

let prog6 =
    Seq (
    Ass ('x', Cnt 0, 1),
    Seq (
    Ass ('y', Cnt 1, 2),
    Seq (
    Ass('z', Cnt (-1), 3),
    Seq (
    Ass ('a', Mult (Var 'x', Var 'y'), 4),
    Seq (
    Ass ('b', Mult (Var 'y', Var 'z'), 5),
    Seq (
    Ass ('c', Mult (Var 'x', Var 'z') ,6),
    Seq (
    Ass ('d', Minus (Var 'x', Var 'y') , 7),
    Seq (
    Ass ('e', Minus (Var 'y', Var 'z') , 8),
    Seq (
    Ass ('f', Minus (Var 'x', Var 'z'), 9),
    Seq (
    Ass ('g', Plus (Var 'x', Var 'y') , 10),
    Seq (
    Ass ('h', Plus (Var 'y', Var 'z'), 11),
    Ass ('i', Plus (Var 'x', Var 'z') , 12)
    )))))))))))


module SolverRD = SolverGenerator(RDAnalysis)
module SolverAE = SolverGenerator(AEAnalysis)
module SolverSI = SolverGenerator(SignAnalysis)
module SolverCP = SolverGenerator(CPAnalysis)
(* Optionals *)
module SolverVB = SolverGenerator(VB)
module SolverLV = SolverGenerator(LV)


let exec analysis prog =
  let generate_res f t =
    LabelMap.fold (fun l know acc ->
        acc ^ (string_of_int l) ^ ": " ^ (f know) ^ ";\n"
      ) t ""
  in
  let clean_string rin rout =
    "FEntry:\n" ^ rin ^ "\nFExit:\n" ^ rout ^ "\n"
  in
  let (result) =
    match analysis with
    | `RD ->
      let (t1, t2) = SolverRD.solveMF prog in
      let resin = generate_res SolverRD.string_of_knowledge t1 in
      let resout = generate_res SolverRD.string_of_knowledge t2 in
      clean_string resin resout
    | `AE ->
      let (t1, t2) = SolverAE.solveMF prog in
      let resin = generate_res SolverAE.string_of_knowledge t1 in
      let resout = generate_res SolverAE.string_of_knowledge t2 in
      clean_string resin resout
    | `SI ->
      let (t1, t2) = SolverSI.solveMF prog in
      let resin = generate_res SolverSI.string_of_knowledge t1 in
      let resout = generate_res SolverSI.string_of_knowledge t2 in
      clean_string resin resout
    | `CP ->
      let (t1, t2) = SolverCP.solveMF prog in
      let resin = generate_res SolverCP.string_of_knowledge t1 in
      let resout = generate_res SolverCP.string_of_knowledge t2 in
      clean_string resin resout
    | `VB ->
      let (t1, t2) = SolverVB.solveMF prog in
      let resin = generate_res SolverVB.string_of_knowledge t2 in
      let resout = generate_res SolverVB.string_of_knowledge t1 in
      clean_string resin resout
    | `LV ->
      let (t1, t2) = SolverLV.solveMF prog in
      let resin = generate_res SolverLV.string_of_knowledge t2 in
      let resout = generate_res SolverLV.string_of_knowledge t1 in
      clean_string resin resout
  in
  print_string "\nProg:\n"; print_stm prog; print_string "\n";
  print_string "\nResult of the analysis\n";
  print_string result


let tests = [
  `RD ;
  `AE ;
  `SI ;
  `CP ;
  `VB ;
  `LV ;
]

let test_progs = [
  "Program_1", prog1 ;
  "Program_2", prog2 ;
  "Program_3", prog3 ;
  "Program_4", prog4 ;
  "Program_5", prog5 ;
  "Program_6", prog6 ;
]

let run_on_type t =
  List.iter (
    fun (name, prog) ->
      Printf.printf "[START %s]" name ;
      exec t prog;
      Printf.printf "[END %s]\n\n" name ;
  ) test_progs

let run_all_tests () =
  List.iter (
    function
      | `AE -> print_string "\n=== [AE] ===\n" ; run_on_type `AE
      | `RD -> print_string "\n=== [RD] ===\n" ; run_on_type `RD
      | `SI -> print_string "\n=== [SI] ===\n" ; run_on_type `SI
      | `CP -> print_string "\n=== [CP] ===\n" ; run_on_type `CP
      | `VB -> print_string "\n=== [VB] ===\n" ; run_on_type `VB
      | `LV -> print_string "\n=== [LV] ===\n" ; run_on_type `LV
  ) tests

let export_all_flows () =
  List.iter (
    fun (name, prog) ->
      dot_from_program name (Program ([], prog))
  ) test_progs

(* This is the "main" function. *)
let () =
  export_all_flows ();
  run_all_tests ()
