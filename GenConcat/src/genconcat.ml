(**
 * Executable for genconcat
 * CHABOCHE - MARAIS
 *)

let args () =
  match Array.length Sys.argv with
  | 2 ->
      Input.read_from Sys.argv.(1)
  | 3 ->
      let _ =
        try
          int_of_string Sys.argv.(2)
          |> Regexp.RE.set_limit
        with Failure _ ->
          Printf.printf "[Warning] limit set to 5\n"
      in Input.read_from Sys.argv.(1)
  | _ ->
      Printf.printf "Wrong argument: ./genconcat file [limit : int]\n";
      raise Input.NoInputFile

let _ =
  let lst = args () in
  let exprs = Graph.Problem.extract lst in
  let exprs = Expression.Expr.to_solution exprs in
  let _ = Expression.Expr.check exprs lst in
  Expression.Expr.print_list exprs
