(*
 * Chaboche - Marais
 * CALOD - 2019
 *)

let suffix_prism = ".pm"
let suffix_properties = "_properties"

let get_file_prefix file =
  let pre_dot = List.hd (String.split_on_char '.' file) in
  let post_slash = (String.split_on_char '/' pre_dot) in
  List.nth post_slash (List.length post_slash - 1)

let path_to file =
  let pref = get_file_prefix file in
  pref ^ suffix_prism, pref ^ suffix_properties

let write_str_in_file out str =
  if str <> "" then
    begin
      let fd = open_out out in
      Printf.fprintf fd "%s" str;
      close_out fd
    end
