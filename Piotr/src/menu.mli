(* = Interface Menu= *)

(* Bouton de menu *)
type button = {
  name : string;
  action : unit -> bool;
  pos : Window.coord;
}

(* Liste de boutons qui forment un menu *)
type menu = button list

(* Créer un menu pour une fenêtre *)
val make_menu : Window.size -> (string * (unit->bool)) list -> menu

(* Affiche tous les boutons d'un menu dans la fenêtre *)
val print_all_buttons : Window.size -> menu -> unit

(* Boucle de menu qui prend un menu  et continue si le menu renvoie true *)
val menu_loop : Window.size -> menu -> unit 

