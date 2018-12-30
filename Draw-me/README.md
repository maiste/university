# Projet ADS4 : Draw Me 

## Binome
 * Xavier DURAND (Maths-Info 1)
 * Étienne MARAIS (Info 4)

## Partie réalisée
  * Langage de base avec le système de tokens.
  * Constantes (Const x = expr) avec utilisation des constantes dans le langage avec gestion de la portée. La liaison est dynamique.
  * Conditionnelle => If Then inst Else inst.
  * Système de variables avec gestion de la portée. La liaison est dynamique.

## Extensions libres :
  * Implémentation de la boucle While exp Do inst.
  * Conditionnelle utilisable sans Else et utilisation possible du Else If (If inst Else If inst Else inst;).
  * Utilisation possible du type bool (True et False) et int pour les variables et les constantes. Le typage est statique. Il est déterminé en fonction du contexte (inférence de type). Une variable x de type int ne peut pas recevoir un bool plus tard dans le programme. Dans Var x = 12, si on veut modifier la valeur de x, il faut forcement que ce soit un int. x = True déclenchera une erreur. Il n'est pas possible de pré déclarer une variable comme en Java. ```Var x;``` lance une erreur de l'interpréteur. 
  * Implémentation de procédures avec le mot clef Proc. Elles n'ont pas de type de retour : il s'agit juste de factoriser le code. Les arguments utilisent un typage dynamique. On peut la déclarer avec "Proc nom (args) instruction;". On peut utiliser Begin et End pour faire des blocs d'instructions. Il est possible d'utiliser la récursion. Cependant elle n'est pas optimisée.
  * Un programme **creator** permettant de transformer une photo du dossier img en code de notre langage dans le dossier test. 

## Étapes pour compiler

  ### Informations importantes
  > Afin de pouvoir lancer les commandes du projet, vous devez avoir le programme *make* d'installé. Votre terminal doit supporter les séquences d'échappement **ANSI** pour interpréter les couleurs du make. Sinon cela affichera des symboles comme \033[0m.


  ### Compilation 
  
  Pour compiler le projet, il faut utiliser la commande :
  ```sh
    make compile
  ```

  ### Exécution code
  
  Pour lancer l'exécution de l'interpréteur sur un fichier, il faut lancer la commande :
  ```sh
    make run file=<filename> 
  ```

  \<filename\> désigne le nom du fichier dans le dossier **test/**.
  ```sh
    # Exemple du fichier test/simple
    make run file=simple
  ```
  ### Utilisation du créator

  Pour transformer une image du dossier **img/** en code dans **test/**, il faut lancer la commande :
  ```sh
    make creator image=<filename>
  ```
   
  \<filename\> désigne le nom du fichier dans le dossier **img/**
  ```sh
  # Exemple pour le fichier img/braddock.jpg
  make creator image=braddock.jpg
  # Une fois le fichier créé dans test/ il suffit de l'afficher avec 
  make run file=braddock
  ```

  ### Nettoyer les fichiers

  Pour supprimer les .class il suffit de faire : 
  ```sh
    make clear
  ```

## Note
  L'ensemble des fichiers de tests fournis pour le projet se trouve dans le dossier test. Nous avons rajouter certains fichiers afin de tester les nouvelles fonctionnalités. Il est tout à fait possible de rajouter un nouveau fichier dans ce dossier et de le tester. Pour avoir un rappel des actions possibles vous pouvez utiliser ```make```.
