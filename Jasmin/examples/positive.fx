
/****** SOURCE OCAML:
type little_endian =
 | Double of little_endian      (* ajout d'un digit 0 en bas *)
 | SuccDouble of little_endian  (* ajout d'un digit 1 en bas *)
 | One                          (* digit 1 final *)

let six = Double (SuccDouble One)

let rec to_int = function
 | Double n -> 2 * to_int n
 | SuccDouble n -> 1 + 2 * to_int n
 | One -> 1

let test6 = to_int six

let rec succ = function
 | Double n -> SuccDouble n
 | SuccDouble n -> Double (succ n)
 | One -> Double One

let test7 = to_int (succ six)
*******/

/* convention : Double a tag 0, SuccDouble a tag 1, One a tag 2 */

val six = [0,[1,[2]]]

def to_int (n) =
  if n[0] == 0 then
   2 * to_int (n[1])
  else if n[0] == 1 then
   1 + 2 * to_int (n[1])
  else
   1

val test6 = to_int(six)
val _ = print_int(test6)
val _ = print_string(" ")

def succ (n) =
  if n[0] == 0 then
   [1,n[1]]
  else if n[0] == 1 then
   [0,succ(n[1])]
  else
   [0,[2]]

val test7 = to_int(succ(six))
val _ = print_int(test7)
val _ = print_string("\n")
/* answer: 7 */
