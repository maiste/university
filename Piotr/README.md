# Piotr

## Présentation

Piotr est un projet codé en *OCaml* dans le cadre de l'enseignement PF5 du S5 de l'Université Paris 7.
Il a été réalisé par :
 * Étienne MARAIS, Gr 3
 * Pablito BELLO, Gr 3

## Paquets requis

Pour pouvoir compiler et exécuter le projet, il est necessaire d'avoir **dune 1.0** et la police **bitstream** installés sur le pc.

```sh
        opam update && opam install dune
```

## Compilation et exécution

Pour lancer le projet, il suffit de lancer le script piotr à la racine du dossier avec :
```sh
        ./piotr
```

Si vous souhaitez lancer et compiler le projet à la main, il faut lancer : 
```sh
        dune build ; cd _build/install/default/bin ; ./piotr
```

Pour nettoyer le répertoire, vous pouvez lancer : 
```sh
        dune clean
```


### Copyright
 © *BELLO MARAIS*

