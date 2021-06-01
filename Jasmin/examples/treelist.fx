
/* SOURCE OCAML:
  type 'a tree =
  | Node of 'a tree * 'a tree
  | Leaf of 'a;;

  let rec tolist t = match t with
  | Leaf v -> [v]
  | Node (a, b) -> tolist a @ tolist b;;

  let exemple = List.hd (tolist (Node (Leaf 1, Node (Leaf 2, Leaf 3))));;
*/

def nil () = [0]

def cons (x,l) = [1,x,l]

def leaf (a) = [0,a]

def node (g,d) = [1,g,d]

def concat (l1,l2) =
  if l1[0] == 0 then l2
  else cons (l1[1], concat (l1[2],l2))

def tolist (t) =
  if t[0] == 0 then cons(t[1],nil())
  else concat (tolist (t[1]), tolist (t[2]))

val ex = (tolist (node (leaf (7), node (leaf (5), leaf (9)))))[1]
val _ = print_int(ex)
val _ = print_string("\n")
/* answer: 7 */
