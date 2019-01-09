type color = Blue | Red | Black | Purple 
type label = { coord : int; colored : bool; color : color option}
type bsp = R of color option | L of label * bsp * bsp


(* 
  retourne un bsp avec au moins une solution à partir des paramètres
  chaque label à son champ colored initialisé mais color à None
*)
let random_bsp_naive2 (width : int) (height : int) (bsp_height : int) : bsp =
  Random.self_init ();
  let rec aux (depth : int) (line_interval : int * int) 
  (col_interval : int * int) : bsp =
    if depth <> bsp_height && ((depth mod 2 = 0 && (snd (col_interval)) - (fst (col_interval)) >= 100) 
    || (depth mod 2 <> 0 && (snd (line_interval)) - (fst (line_interval)) >= 100)) then 
    (* n'est pas feuille et l'interval de ligne / colonne est encore asser grand pour diviser en deux *)
      let is_colored = (Random.int 5) <> 0 in
      if depth mod 2 = 0 then (* colonne *)
        let pos = (Random.int ((((snd col_interval) - (fst col_interval)) / 5) * 3))
        + (fst col_interval) + (((snd col_interval) - (fst col_interval)) / 5)  in 
        L(
          {coord = pos; colored = is_colored; color = None},
          aux (depth + 1) line_interval ((fst col_interval), pos),
          aux (depth + 1) line_interval (pos, (snd col_interval))
        )
      else (* ligne *)
        let pos = (Random.int ((((snd line_interval) - (fst line_interval)) / 5) * 3))
        + (fst line_interval) + (((snd line_interval) - (fst line_interval)) / 5) in
        L(
          {coord = pos; colored = is_colored; color = None},
          aux (depth + 1) ((fst line_interval), pos) col_interval,
          aux (depth + 1) (pos, (snd line_interval)) col_interval
        )
    else (* est une feuille *)
      let color = if (Random.int 2) = 0 then Blue else Red in
      R (Some color)
  in aux 1 (0, height) (0, width) 


(* 
  retourne une copie du bsp bsp mais avec tout les rectangles à None
*)
let rec get_empty_bsp (bsp : bsp) : bsp =
  match bsp with 
  | R (_) -> R None
  | L (l,g,d) -> L (l, get_empty_bsp g, get_empty_bsp d)


(* 
  change la couleur du rectangle de coordonnées (x,y) avec la
  couleur new_col
*)
let modify_color (bsp : bsp) ((x, y) : (int * int)) (new_col : color option)  : bsp = 
  let rec aux (node : bsp) (depth : int) : bsp = 
    match node with 
    | L (l,g,d) ->  
        if depth mod 2 = 0 then
          if x < l.coord then L (l, aux g (depth + 1), d)
          else L (l, g, aux d (depth + 1))
        else 
          if y < l.coord then L (l, aux g (depth + 1), d)
          else L (l, g, aux d (depth + 1))
    | R _ -> R new_col
  in aux bsp 0 


(* 
  Donne le type de la ligne : 
  - Bleu : int negatif
  - Rouge : int positif
  - violet : 0
*)
let rec get_type_line (bsp : bsp) (g : bool) (p : bool) (lp : bool) : int =
  match bsp with
  | R col -> ( match col with
      | Some Blue -> -1
      | Some Red -> 1
      | _ -> 0 )
  | L (_,l,r) ->
    if p = lp then
      if g then (get_type_line r g p (not lp))
      else (get_type_line l g p (not lp))
    else
      (get_type_line l g p (not lp))
      + (get_type_line r g p (not lp))


(* 
  retourne la couleur de la ligne/colonne de label lab dans le bsp
*)
let get_line_color (bsp : bsp) (lab : label) (is_line : bool) : color option =
  if not lab.colored then Some Black
  else
    let sum = match bsp with
      | L (_,l,r) ->
        (get_type_line l true is_line (not is_line))
        + (get_type_line r false is_line (not is_line))
      |  _ -> 0
    in
    if sum > 0 then Some Red
    else if sum = 0 then Some Purple
    else Some Blue 


(* 
  return true si la partie est gagnée, false sinon
*)
let is_win (player_bsp : bsp) : bool =
  let rec aux (pl : bsp) (is_line : bool) : bool =
    match pl with
    | L (l,g,d) ->
      let pl_c = get_line_color pl l is_line
      in if pl_c = l.color then
        aux g (not is_line) && aux d (not is_line)
      else false
    | R col ->
      if col = None then false
      else true
  in aux player_bsp false


(* 
  génère un bsp aléatoire, récupère d'abord le bsp aléatoire sans 
  couleur des lignes fixée, puis fixe le champ color de chaque ligne
  et return le bsp
*)
let random_bsp_naive (width : int) (height : int) (bsp_height : int) : bsp =
  let bsp = random_bsp_naive2 width height bsp_height in
  (* fixe la couleur de chaque ligne *)
  let rec aux bsp depth = match bsp with
    | R(x) -> R(x)
    | L (l,g,d) -> 
        let linecol = get_line_color bsp l (depth mod 2 <> 0) in
        L (
          {coord = l.coord; colored = l.colored; color = linecol},
          aux g (depth + 1),
          aux d (depth + 1)
        )
  in aux bsp 0


(* 
  FONCTIONS SERVANT POUR PARTIE 7
*)


(* 
  retourne le rectangle pos dans bsp, pos correspond au numéro
  du rectangle en parcourant de manière prefixe le bsp et en numérotant
  les rectangles à partir de 1. 
  renvoie une paire ((couleur du rectangle, numero), coordonnées) 
  avec coordonnées = un tableau de 4 elements par rectangle, sous forme 
  [| haut, bas, gauche, droite |]
*)
let get_rectangle (bsp : bsp) (pos : int) : ((color option * int) * int array) option =
  let rec aux (node : bsp) (depth : int) (visited : int) : 
  ((color option * int) * int array) option * int =
    match node with 
    | L (lab, g, d) -> 
        let resg = aux g (depth + 1) (visited) in
        if fst resg = None then (* pas trouvé rect a gauche *)
          (* on récup partie droite en incrémentant nb rect visited *)
          let resd = aux d (depth + 1) (snd resg) in
            if fst resd = None then None, snd resd (* pas dans ce sous arbre *)
            else (* trouvé a droite *)
              let tab = snd (match fst resd with Some x -> x | None -> (None, 1),[||]) in
              if (depth mod 2) = 0 then 
                if tab.(2) = 0 then begin 
                  tab.(2) <- lab.coord; resd 
                end 
                else resd 
              else (* ligne *)
                if tab.(1) = 0 then begin 
                  tab.(1) <- lab.coord; resd 
                end
                else resd
        else (* trouvé à gauche *)
          let tab = snd (match fst resg with | Some x -> x | None -> (None, 1),[||]) in
          if (depth mod 2) = 0 then (* colonne *)
            if tab.(3) = 0 then begin 
              tab.(3) <- lab.coord; resg 
            end
            else resg
          else (* ligne *)
            if tab.(0) = 0 then begin 
              tab.(0) <- lab.coord; resg 
            end
            else resg
    | R (col) -> 
        if visited + 1 = pos then Some ((col, (visited + 1)), [|0;0;0;0|]), visited + 1
        else None, visited + 1
  in fst (aux bsp 0 0)


(* 
  retourne une liste avec les coordonnées de tout les rectangles
  du bsp, voir get_rangle pour explication type de retour
*)
let get_rectangles (bsp : bsp) : ((color option * int) * int array) list =
  let i = ref 1 in
  let curr = ref (get_rectangle bsp !i) in
  let res = ref [] in
  while (!curr) <> None do 
    res := ((match !curr with | Some curr -> curr | None -> (None, 1),[||]))::(!res);
    i := !i + 1 ;
    curr := get_rectangle bsp !i
  done ; !res


(* 
  return true si le rectangle rect toucle la ligne/colonne lab 
*)
let is_touching_line (rect : (color option * int) * int array) 
(lab : label) (is_line : bool) : bool =
  let coord = snd rect in
  if is_line then (coord.(0) = lab.coord || coord.(1) = lab.coord)
  else (coord.(2) = lab.coord || coord.(3) = lab.coord)

(* 
  retourne l'ensemble des predicats de tout les rectangles de la liste rect_list
*)
let rec get_rectangles_predicates (rect_list : ((color option * int) * int array) list) :
(bool * int) list = match rect_list with
  | [] -> []
  | t::q ->
      let rect_color = fst (fst t) in
      if rect_color = None then get_rectangles_predicates q
      else (* le rectangle est coloré, on l'ajoute *)
        let rect_id = snd (fst t) in
        if rect_color = Some Red then (true, rect_id)::(get_rectangles_predicates q)
        else (false, rect_id)::(get_rectangles_predicates q)


(* 
  retourne la liste des predicates des rectangles qui touchent la ligne/colonne
  fixe le boolean de chaque predicat en fonction de is_red
*)
let rec get_predicates (lab : label) (is_line : bool) (is_red : bool)
(rect_list : ((color option * int) * int array) list) : (bool * int) list =
  match rect_list with
    | [] -> []
    | t::q -> 
        if is_touching_line t lab is_line then
          (is_red, snd (fst t))::(get_predicates lab is_line is_red q)
        else get_predicates lab is_line is_red q


(* 
  retourne la liste de listes de predicats pour chaque ligne/colonne
  du bsp combinée la liste des predicats des rectangles
*)
let get_lines_predicates (bsp : bsp) : (bool * int) list list = 
  let rect_list = get_rectangles bsp in
  let rec aux bsp depth = match bsp with 
    | R _ -> []
    | L (lab, g, d) -> 
        let is_line = depth mod 2 <> 0 in
        if lab.color = Some Red then 
          ((get_predicates lab is_line true rect_list |> Solving.get_all_subset_aux false)
          @(aux g (depth + 1)))@(aux d (depth + 1))
        else if lab.color = Some Blue then 
          ((get_predicates lab is_line false rect_list |> Solving.get_all_subset_aux false)
          @(aux g (depth + 1)))@(aux d (depth + 1))
        else if lab.color = Some Purple then 
          (get_predicates lab is_line true rect_list |> Solving.get_all_subset_aux true)
          @(aux g (depth + 1)) 
          @(get_predicates lab is_line false rect_list |> Solving.get_all_subset_aux true)
          @(aux d (depth + 1))
        else aux g (depth + 1)@(aux d (depth + 1))
  in let line_predicates = (aux bsp 0) in
  let rec add_rects_predicates (acc : (bool*int) list list) (rect : (bool*int) list) : (bool*int) list list =
    match rect with
    | [] -> acc
    | t::q -> add_rects_predicates ([[t]]@acc) q
  in (add_rects_predicates [] (get_rectangles_predicates rect_list))@line_predicates
