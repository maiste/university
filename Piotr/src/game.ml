(**
   Module de gestion du jeu
   [auteurs] bello marais
*)

(* == PACKAGES == *)
open Graphics

(* Menu de victoire *)
let win_menu (h,l) =
  Menu.make_menu (h,l)
    [
      ("Gagner !", (fun () -> false));
    ] |> Menu.menu_loop (h,l)
    
(* Affiche les touches utilisables en jeu  *)
let help_menu (h,l) : unit =
  Menu.make_menu (h,l)
    [("Touches :", (fun () -> true)) ;
     ("b -> Bleu", (fun () -> true)) ;
     ("r-> Rouge ", (fun () -> true)) ;
     ("d -> Blanc", (fun () -> true)) ;
     ("m -> Menu", (fun () -> true)) ;
     ("q -> Quitter\n", (fun () -> true)) ;
     ("Retour menu", ( fun () -> false))
    ]
  |> Menu.menu_loop (h,l)

(* Affichage de vérification *)
let check_menu (h,l) (is_sol) : unit =
  Menu.make_menu (h,l)
    (if is_sol then
      [("Solution existe", (fun () -> false))]
    else
      [("Pas de solution", (fun () -> false))])
  |> Menu.menu_loop (h,l)
      
(* Affiche le menu de jeu *)
let game_menu (tree : Bsp.bsp) (h,l) : unit =
  Menu.make_menu (h,l)
    [
      ("Jeu",(fun () -> false)) ;
      ("Verification",
       (fun () ->
          Bsp.get_lines_predicates tree
          |> Solving.check |> check_menu (h,l) ;
           false
         )
      ) ;
      ("Aide", (fun () -> help_menu (h,l) ; true)) ;
      ("Quitter", (fun () -> raise Window.Quit))
    ]
  |>  Menu.menu_loop (h,l)

(* Boucle d'intéraction avec l'utilisateur *)
let interact (h,l) (tree : Bsp.bsp) (c : Bsp.color option) : Bsp.bsp*Bsp.color option =
  let evt = wait_next_event [Key_pressed ; Button_down]
  in if evt.keypressed then
    match evt.key with
    | 'b' -> (tree, Some Blue)
    | 'r' -> (tree, Some Red)
    | 'd' -> (tree, None)
    | 'm' -> game_menu tree (h,l) ; (tree,c)
    | 'q' -> raise Window.Quit
    | _ -> (tree, c)
  else if evt.button then
    (Bsp.modify_color tree (evt.mouse_x, evt.mouse_y) c, c)
  else
    (tree, c)

(*  Boucle principale du jeu : affiche l'arbre,
 vérifie la victoire et modifie le contexte *)
let rec loop (h,l) (tree : Bsp.bsp) (c : Bsp.color option) :unit =
  let (next_tree, next_color) = interact (h,l) tree c
  in
  if Bsp.is_win next_tree then win_menu (h,l)
  else begin
    Window.draw_content (h,l) next_tree ;
    Unix.sleepf 0.025 ; (* Avoid freeze with to much event *)
    loop (h,l) next_tree next_color ;      
  end

(* Lancement du jeu avec la bonne dimension *)
let game (h,l) p : unit =
  let size = (h,l)
  in begin
    Window.close ();
    Window.make_window size ;
    let solution = Bsp.random_bsp_naive h l p
    in let empty = Bsp.get_empty_bsp solution
    in
    Window.draw_content size empty;
    try
      loop size empty None
    with Window.Quit | Graphics.Graphic_failure _ -> () ;
  end


(* Menu de choix des profondeurs *)
let prof_menu (h,l) (hg, lg) : unit =
  Menu.make_menu (h,l)
    [
      ("Profondeur BSP",(fun () -> true)) ;
      ("Niveau 1", (fun () -> game (hg,lg) 3 ; false)) ;
      ("Niveau 2", (fun () -> game (hg,lg) 4 ; false)) ;
      ("Niveau 3", (fun () -> game (hg,lg) 5 ; false)) ;
      ("Niveau 4", (fun () -> game (hg,lg) 6 ; false)) ;
      ("Retour", (fun () -> false))
    ]
  |>  Menu.menu_loop (h,l)

(* Menu de choix des dimensions *)
let dim_menu (h,l) : unit =
  Menu.make_menu (h,l)
    [
      ("Dimension du jeu",(fun () -> true)) ;
      ("500x 500", (fun () -> prof_menu (h,l) (500,500) ; false)) ;
      ("600x600", (fun () ->  prof_menu (h,l) (600,600) ; false)) ;
      ("700x700", (fun () ->  prof_menu (h,l) (700,700) ; false)) ;
      ("800x800", (fun () ->  prof_menu (h,l) (800,800) ; false)) ;
      ("Retour", (fun () -> false))
    ]
  |>  Menu.menu_loop (h,l)

(* Main menu *)
let first_menu (h,l) : unit =
  Menu.make_menu (h,l)
    [
      ("Jouer",(fun () -> dim_menu (h,l) ; Window.close () ; Window.make_window (h,l) ; true)) ;
      ("Aide", (fun () -> help_menu (h,l) ; true)) ;
      ("Quitter", (fun () -> false))
    ]
  |>  Menu.menu_loop (h,l)

(* Fonction main du jeu *)
let main () =
  let size = (500,500)
  in begin
    Window.make_window size  ; 
    try 
      first_menu size
    with _ -> ();
  end
  

