# Mpsh : mon petit shell

## Équipe

* Alexandre Moine
* Pablito Bello
* Étienne Marais

## Contenu
La description du contenu se trouve dans le fichier man/mpsh.1.gz. Il s'ouvre en suivant l'installation ci-dessous soit
via ```man mpsh.1.gz```.

## Installation

Pour installer le programme et le lancer depuis n'importe où : 

```sh
  user@local $ make install
  user@local $ echo "export PATH=$PATH:~/mpsh" >> .bashrc
  user@local $ echo "export MANPATH=$MANPATH:~/mpsh/man" >> .bashrc
  user@local $ source .bashrc
  
  # Lancer le shell
  user@local $ mpsh

  # Lancer le man 
  user@local $ man mpsh
```

Si jamais le man n'est pas trouvé, vous pouvez le lancer via :

```sh
  # Dans le répertoire du projet
  user@local $ cd man
  user@local $ man mpsh.1.gz
```