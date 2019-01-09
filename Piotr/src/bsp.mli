type color = Blue | Red | Black | Purple  (* couleurs *)
type label = { coord : int; colored : bool; color : color option} (* ligne / colonne *)
type bsp = R of color option | L of label * bsp * bsp (* bsp *)

(* 
  renvoie un bsp généré aléatoirement avec au moins une solution
  les paramètres sont (dans l'ordre) :
  - largeur de la fenêtre - hauteur de la fenêtre - hauteur du bsp
  le bsp avec juste une racine est considéré de hauteur 1.
*)
val random_bsp_naive : int -> int -> int -> bsp

(* 
  renvoie une copie du bsp prit en paramètre avec toute les 
  couleurs des rectangles à None
*)
val get_empty_bsp : bsp -> bsp


(* 
  renvoie une copie du bsp passé en paramètre avec la couleur
  du rectangle contenant le point du coupe (x, y) modifié à 
  new_color : color 
*)
val modify_color : bsp -> int * int -> color option -> bsp 

(* 
  renvoie la couleur de la ligne label dans le bsp bsp,
  le troisième parametre est un boolean valant true si
  c'est une ligne, false si c'est une colonne
  (à passer selon la profondeur)
*)
val get_line_color : bsp -> label -> bool -> color option

(* 
  prend le bsp du joueur et le bsp final (généré) en 
  paramètres et renvoie true si le joueur à gagné, 
  false sinon
*)
val is_win : bsp -> bool

(* 
  retourne la liste de listes de predicats pour chaque ligne/colonne
  du bsp combinée la liste des predicats des rectangles
*)
val get_lines_predicates : bsp -> (bool * int) list list