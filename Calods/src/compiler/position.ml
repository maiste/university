(*
 * Chaboche - Marais
 * CALODS 2019
 *)

(* Position *)
type 'a position = {
    pos  : Lexing.position ;
    value : 'a  ;
}

(* Create a position *)
let make pos value =
  {
    pos  = pos  ;
    value = value ;
  }

(* Return the content of the position *)
let value { value = v ; _ } = v

let values vs = List.map value vs

let w_pos x =
  make Lexing.dummy_pos x
