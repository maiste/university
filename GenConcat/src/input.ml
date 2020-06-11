(**
 * Manage string recuperation in file
 * CHABOCHE - MARAIS
 *)

exception NoInputFile

let split_in_tuple str =
  let str = String.split_on_char '\t' str in
  match str with
  | i::o::[] -> (i,o)
  | _ -> failwith "Wrong file, no tabulation to separate input and output"

(** Read a string list from [file] source *)
let read_from file =
  let input = open_in file in
  let rec aux acc =
    try
      let line = input_line input |> String.trim |> split_in_tuple in
      aux (line::acc)
    with End_of_file ->
      begin
        close_in input ;
        acc |> List.rev
      end
  in aux []
