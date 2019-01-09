(**
   Module de gestion de la fenêtre
   [auteurs] bello marais
*)


(* = PACKAGES = *)
open Graphics


(* = TYPES = *)
(* taille de la fenêtre *)
type size = int*int

(* Coordonnées dans la fenêtre *)
type coord = {
  x : int ;
  y : int ;
  x' : int ;
  y' : int ;
}

(* Exception pour signifier l'arrêt *)
exception Quit


(* = FONCTIONS = *)

(* Lance une fenêtre *)
let make_window (h,l) : unit =
  begin
    Printf.sprintf " %dx%d" h l |> open_graph ;
    auto_synchronize false ;
    set_line_width 2 ;
  end

(* Ferme la fenêtre *)
let close () : unit =
  close_graph ()

(* Fabrique de coordonnées *)
let make_coord x y x' y' =
  {
    x = x ;
    y = y ;
    x' = x' ;
    y' = y' ;
  }

(* Test si une coordonées est dans une "boîte" *)
let is_in box (x,y) =
  box.x <= x &&
  box.x' >= x &&
  box.y <= y &&
  box.y' >= y

(* Dessine un rectangle à la position fournie *)
let draw_rect_at (c : coord) (bc : Bsp.color option) : unit =
  let () =  match bc with
    | Some Red -> rgb 254 135 135 |> set_color 
    | Some Blue  -> rgb 182 223 254 |> set_color
    | Some Black -> set_color black
    | Some Purple -> rgb 196 28 196 |> set_color 
    | None -> set_color white
  in fill_rect c.x c.y (c.x'-c.x) (c.y'-c.y)

(* Dessine une ligne à la position indiquée *)
let draw_line_at (c : coord) (bc : Bsp.color option) : unit =
  let () = match bc with
    | Some Red -> set_color red
    | Some Blue -> set_color blue
    | Some Black -> set_color black
    | Some Purple -> rgb 196 28 196 |> set_color 
    | None -> set_color white 
  in
  begin
    moveto c.x c.y ;
    lineto c.x' c.y' ;
  end

(* Dessine un bsp dans la fenêtre *)
let draw_bsp (h,l) (b : Bsp.bsp) : unit =
  let rec slide_tree (b : Bsp.bsp) (is_line: bool) (c: coord) =
    match b with
    | R col -> draw_rect_at c col 
    | L ({coord = vl ; colored ; color = col}, l ,r) ->
      if not is_line then begin
        slide_tree l (not is_line) {x=c.x;x'=vl;y=c.y;y'=c.y'};
        slide_tree r (not is_line) {x=vl;x'=c.x';y=c.y;y'=c.y'};
        draw_line_at {x=vl;x'=vl;y=c.y;y'=c.y'} col;
      end
      else begin
        slide_tree l (not is_line) {x=c.x;x'=c.x';y=c.y;y'=vl};
        slide_tree r (not is_line) {x=c.x;x'=c.x';y=vl;y'=c.y'};
        draw_line_at {x=c.x;x'=c.x';y=vl;y'=vl} col;
      end
  in slide_tree b false { x = 0 ; y = 0 ; x' = h ; y' = l }

(* Dessine le bsp et met à jour la fenêtre *)
let draw_content (h,l) (b : Bsp.bsp) : unit =
  begin
    draw_bsp (h,l) b ;
    synchronize () ;
  end
