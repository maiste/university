open Whilennh
open Auxiliary
open Mf


(* Functions from labels to values of type 'a. *)
module LabelMap = Map.Make(Label)


(* Monotone Frameworks *)
module SolverGenerator (MF: MF) = struct
  include MF
  let initAnalysis labels ext iota bot =
    List.fold_right (
        fun l map -> let v = if List.mem l ext 
                             then iota 
                             else bot 
                     in
                     LabelMap.add l v map)
      labels
      LabelMap.empty

  let rec result labels f analysis =
    List.fold_right (fun label (mfpIn, mfpOut) ->
        let analysis_l = LabelMap.find label analysis in
        let mfpIn = LabelMap.add label analysis_l mfpIn
        and mfpOut = LabelMap.add label (f label analysis_l) mfpOut in
        (mfpIn, mfpOut))
      labels
      (LabelMap.empty, LabelMap.empty)

  let rec updateInfo w flow f analysis =
     match w with
    | [] -> analysis
    | (x, y) :: tail ->
      let analysis_x = LabelMap.find x analysis in
      let analysis_y = LabelMap.find y analysis in
      let f_on_x = f x analysis_x in
      if MF.leq f_on_x analysis_y then
        (* En réalité le else dans les slides *)
        updateInfo tail flow f analysis
      else
        (* Du coup le if then ici *)
        let new_analysis_y = MF.lub analysis_y f_on_x in
        let analysis = LabelMap.add y new_analysis_y analysis in
        let add = List.filter (fun (fst,_) -> fst=y) flow in
        updateInfo (add@tail) flow f analysis

  let solveMF (stm : stm) =
    let labels = labels_stm stm in
    let flow = MF.flow stm in
    let f = f stm in
    let analysis = initAnalysis (LabelSet.elements labels) (ext stm) (iota stm) (bot stm) in
    let analysis = updateInfo flow flow f analysis in
    result (LabelSet.elements labels) f analysis
end
