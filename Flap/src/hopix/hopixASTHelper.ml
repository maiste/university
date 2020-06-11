open HopixAST

let fresh_identifier =
  let count = ref (-1) in
  fun () -> incr count; Id ("id" ^ string_of_int !count)
