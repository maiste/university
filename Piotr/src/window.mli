(* Interface de gestion des fenêtres *)

(* = TYPE = *)
type size = int*int

type coord = {
  x : int ;
  y : int ;
  x' : int ;
  y' : int ;
}

(* Exception d'arret de boucle *)
exception Quit

(* Fabrique une fenêtre de bonne taille *)   
val make_window : size -> unit 

(* Ferme la fenêtre courante *)
val close : unit -> unit

(* Fabrique des coordonnées *)
val make_coord : int -> int -> int -> int -> coord

(* Test si des contenus sont l'un dans l'autre *)
val is_in : coord -> (int*int) -> bool

(* Dessine un rectangle à coord *)
val draw_rect_at : coord -> Bsp.color option -> unit 

(* Dessine une ligne à coord de couleurs Bsp *)
val draw_line_at : coord -> Bsp.color option -> unit

(* Dessine la fenêtre du bsp *)
val draw_content : size -> Bsp.bsp -> unit
