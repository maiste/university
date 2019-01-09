(** 
   Module de gestion des menus
   [auteurs] bello marais
*)

(* = PACKAGES = *)
open Graphics


(* = TYPE = *)

(* Type des contenus des menus *)
type button = {
  name : string;
  action : unit -> bool;
  pos : Window.coord;
}

(* Type des menus *)
type menu = button list


(* = METHODES = *)



(* Permet de fabriquer un bouton *)
let make_button s f coord =
  {
    name = s ;
    action = f;
    pos = coord;
  }

(* Permet de frabriquer un menu *)
let make_menu (h,l) list =
  let size = List.length list
  in let rec aux acc k iter = match iter with
      | [] -> acc
      | (s,f)::e ->
        aux (
          (make_button s f
             (Window.make_coord
                0 (k*h/size) l ((k+1)*h/size)
             ))::acc
        ) (k+1) e
  in aux [] 0 (List.rev list)


(* Dessine une chaine de caractères à la bonne position *)
let draw_string_at x y s (h,l) =
  begin 
    moveto (x +(l/6)) (y+ (h/3)) ;
    Format.sprintf "-bitstream-*-*-*-normal-*-%d-*-*-*-*-*-*-*"
      (l/15)
    |>  set_font ;
    set_color black ;
    draw_string s 
  end

(* Affiche un bouton *)
let print_button button (h,l) : unit  =
  begin
    Window.draw_rect_at button.pos (Some Bsp.Blue) ;
    let c = button.pos in
    Window.draw_line_at (Window.make_coord c.x c.y (c.x+l) c.y) (Some Bsp.Black);
    draw_string_at button.pos.x button.pos.y
      button.name (h,l) ;
    synchronize ();
  end

(* Affiche tous les boutons d'un menu *)
let print_all_buttons (h,l) list =
  let nb = List.length list
  in
  let rec aux iter = match iter with 
    | [] -> ()
    | f::e ->
      begin
        print_button f (h/nb,l);
        aux e
      end
  in aux list 

(* Execute l'action du menu en fonction du clique.
   Renvoie un booleen qui indique si le menu doit
   s'arrêter ou non *)
let execute_action list (x,y) : bool =
  let rec aux iter =
    match iter with
    | [] -> false
    | f::q ->
      if Window.is_in f.pos (x,y) then
        f.action ()
      else
        aux q
  in aux list 


(* Boucle de fonctionnement du menu *)
let rec menu_loop (h,l) menu = 
  let () = print_all_buttons (h,l) menu
  in
  let evt = wait_next_event [Button_down]
  in
  if not evt.button then menu_loop (h,l) menu
  else 
    let continue = execute_action menu (evt.mouse_x, evt.mouse_y)
    in if continue then 
      menu_loop (h,l) menu
    else ()
