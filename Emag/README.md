# Projet Langage à objets avancé 2019

Ce projet à été réalisé dans le cadre de notre M1 d'informatique par Étienne Marais et Marie Bétend.

### Manuel d'utilisation

Tu as besoin de la librairie ncurses pour pouvoir compiler.

Avant de pouvoir commencer à utiliser l'éditeur de niveau ou jouer, il faut commencer par compiler les fichiers nécessaires :
```sh
$ cd emag/
$ make
```
Tu peux aussi compiler le Game Creator et le Game Player de manière indépendante avec :
```sh
$ cd emag/
$ make gp  # pour le game player
$ make gc  # pour le game creator
```

### Manuel de l'aventurier

Bienvenue aventurier ! Maintenant que tu as réussi à générer le jeu, voici toutes les informations nécessaires pour survivre à l'aventure, avec le game player; ou pour piéger tes amis aventuriers, à l'aide du game creator, en leur concoctant des niveaux impossibles ! Prends garde sinon **"Ce tombeau sera votre tombeau"**.

#### Elements du jeu

- Le oueurj (toi) est représenté par **J**. Tu peux te déplacer sur les 8 cases adjacentes à ta position, si elles ne sont pas occupées par des éléments que tu ne peux pas traverser. Si tu as ramassé des geuchars, tu peux les utiliser pour te déplacer sur une case aléatoire valide du plateau. Mais attention, les streumons ne sont pas loin !
- Il y a plusieurs types de streumons, capables de te tuer ou d'entraver tes mouvements :
  - Les NormalStreumons, représentés par **~**, peuvent se déplacer sur l'une des 8 cases adjacentes à leur position, en choisissant la case valide qui les rapproche le plus possible de toi. La légende raconte qu'il s'agit d'élèves ayant trichés, maudits par leurs professeurs.
  - Les LineStreumons, représentés par **l**, se déplacent seulement le long d'une ligne, à droite ou à gauche de sorte à être le plus proche de toi possible. On raconte que ce sont des élèves qui se sont perdus en essayant de trouver une salle dans le batiment Olympe de Gouges.
  - Les ColumnStreumons, représentés par **c**, se déplacent seulement le long d'une colonne, en haut ou en bas, choisissant la case qui te met le plus à leur portée. Certains disent qu'il s'agit de l'âme des précédents oueurjs, qui se sont fait dévorer précédemment... Méfie-toi! Lorsqu'un LineStreumon et un ColumnStreumon se rencontrent sur une case du plateau, ils fusionnent pour créer un mur, peut-être te retrouveras tu coincé ...
  - Les RandomStreumons, représentés par **r**, tirent une case au hasard parmi les 8 cases adjacentes, et si cette case n'est pas valide, ils restent immobiles (les pauvres !). Des histoires les mentionnant nous révèlent qu'il s'agit d'enfants capricieux laissés à l'abandon.
  Tu ne peux pas marcher ou traverser un streumon, à toi donc de trouver un moyen de les éviter !
- Les reumus sont représentés par **#**, ni les streumons, ni toi ne peuvent les traverser.
- Les teupors sont représentées par **-**, elles sont positionnées sur les reumus extérieurs, et s'ouvrent lorsque tu ramasses le diams associé. Lorsqu'elles sont ouvertes, elles sont représentées par **+**. Tu ne peux les traverser que si elles sont ouvertes. En passant par une porte, tu finis un niveau (et tu es peut être tiré d'affaire !). À cause d'un mauvais sort, les streumons sont bloqués dans la pièce qui leur est associée, ils ne te suivront donc pas à travers la porte.
- Les diams sont représentés par **$**. C'est ton unique objectif (en plus de survivre) dans tes aventures. Un diams peut être associé à une porte ou non, en fonction du nombre de teupors dans le niveau. À cause de leur malédiction, les streumons ne peuvent pas ramasser de diams, ils les évitent donc. Tu peux ramasser un diams en marchant dessus. Plus tu as de diams, plus tes amis aventuriers seront jaloux de toi avec l'argent que tu gagneras !
- Les geuchars, représentés par **\***, peuvent être ramassés si tu marches dessus. Cela te rajoute une possibilité de déplacement. Les streumons peuvent marcher sur les geuchars, ce qui les fera disparaitre définitivement. Attention, les geuchars sont capricieux, ils te transporteront peut être face à un streumons!
- Les cases vides, représentées par **.**, peuvent être parcourues par tout le monde.

#### Game Creator

###### Board

Pour créer un niveau, utlise la commande **./gc** suivi du nom de fichier voulu. Il faut que l'extension du fichier soit un _.board_.
```sh
$ ./gc <name>.board
```

L'éditeur de niveaux permet de créer un nouveau niveau ou de modifier un niveau déjà existant.
Si tu décides de créer un niveau, il te sera demandé les dimensions souhaitées pour le nouveau plateau : un plateau basique constitué des 4 reumus extérieurs avec le oueurj placé au centre sera généré. Si tu choisis de modifier un niveau existant, le plateau correpondant au fichier passé en argument sera chargé dans l'éditeur s'il est conforme (hauteur supérieure à 3, largeur supérieure à 4).

Dans les deux cas, il te sera ensuite proposé de modifier le plateau, en ajoutant ou supprimant des élements ou en déplaçant la position intiale du oueurj.
Il faudra que le plateau comporte _au moins une teupor_, et qu'il y ait _au moins autant de diams que de teupors_, sans quoi l'éditeur retournera une erreur et ne voudra pas que tu quittes (le vilain !).

Lorque tu seras satisfait avec ton plateau, tu pourras quitter l'éditeur en rentrant le caractère *q* lorqu'une nouvelle modification te sera proposée.
N'hésite pas à laisser courrir ton imagination !

###### Game

Pour créer un jeu, composé de plusieurs niveaux, utilise de nouveau **./gc** suivi du nom du fichier de jeu, avec une extension _.game_, et le nom de tous les niveaux que tu veux intégrer dans le jeu, en _.board_.
```sh
$ ./gc <name>.game [<name>.board ...]
```
Tu peux ajouter autant de niveaux au jeu que tu souhaites.
Si le fichier _.game_ existe déjà, il sera entièrement réécrit avec les nouveaux niveaux passés en arguments. Ainsi, si tu souhaites rajouter un niveau à un jeu déjà existant, il faudra passer en argument tous les niveaux déjà intégrés dans le jeu, et le nouveau niveau. L'ordre de passage des niveaux correspond à l'ordre des niveaux dans le jeu.

De plus, tous les _.board_ passés en arguments doivent exister, sans quoi l'éditeur retournera une erreur.


#### Game Player

Pour lancer un jeu, utilise la commande **./gp** suivi du type de vue (terminal : t ou ncurses :n) et du nom du fichier du jeu souhaité. Ce fichier doit être un _./game_, un _./board_ ou un _./save_.
```sh
$ ./gp [t|n] <name>.game
# ou pour tester un niveau
$ ./gp [t|n] <name>.board
# ou pour reprendre une partie précédemment sauvegardée
$ ./gp [t|n] <name>.save
```


Il te sera ensuite demandé de saisir ton nom d'aventurier. Tu entres alors dans une succession de niveaux palpitants dans lesquels tu vas devoir faire preuve d'ingéniosité pour te sortir de ce mauvais pas !

Pour finir un niveau il faut sortir par l'une des teupors présentes dans la pièce. Pour finir le jeu, il faut réussir tous les niveaux. La somme d'argent que tu reçois à la fin est calculée en fonction du nombre de diams ramassés et du niveau correspondant. Plus un niveau est loin dans le jeu plus il te rapporte d'argent (prime de risques oblige !). La seule manière de mourir, est qu'un streumon arrive sur la même case que toi.

Au cours du jeu tu peux te déplacer en utilisant les touches suivantes :

- z pour aller vers le haut
- s pour aller en bas
- d pour aller à droite
- q pour aller à gauche
- a pour aller en haut à gauche
- e pour aller en haut à droite
- w pour aller en bas à gauche
- x pour aller en bas à droite
- g pour utiliser un geuchar (si tu en as)
- l pour quitter le jeu

Lorsque que tu quittes le jeu, il te sera proposé de sauvergarder ta progression, en entrant le nom d'un fichier.

## Mot de la fin

Bon courage aventurier !
