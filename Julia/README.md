# Projet_Julia

## Nécessaire
Ce projet fonctionne sous **Java 11**.

## Compilation et lancement
Le fichier a déjà été compilé sous Intellij dans bin/production/Projet_Julia. Il peut être lancé depuis le script ```fractal```. 
Il doit être importé en tant que projet dans Intellij pour pouvoir être de nouveau compilé dans ce logiciel. Le dossier `javafx-sdk-11.0.1` doit être à la racine de ce projet pour pouvoir être exécuté (cf Annexe).

``` sh
    # Mode ligne de commandes
    user$ ./fractal [julia | mandelbrot] width height origin_x origin_y zoom(%) iteration name [reel] [img]

    # Mode graphique
    user$ ./fractal graphique

    # Mode interactif ligne de commandes 
    user$ ./fractal interactif

```

## Architecture

L'arborescence du projet est la suivante. Elle s'inspire du modèle MVC : 
```sh
    src
├── model
│   ├── Calculus.java
│   ├── Complex.java
│   ├── Grid.java
│   ├── Julia.java
│   ├── Mandelbrot.java
│   ├── ModelTest.java
│   └── PolynomialFactory.java
└── view
    ├── command
    │   └── TerminalMain.java
    ├── graphic
    │   ├── FunctionBox.java
    │   ├── JuliaBox.java
    │   ├── MainStage.java
    │   └── MandelbrotBox.java
    ├── ImageSaver.java
    └── lib
        ├── error.png
        ├── play-button-1.png
        ├── plus.png
        └── stop-1.png
```


## Implémentations

### Ensembles de Julia et Mandelbrot
Les ensembles de Julia ont été implémentés dans le package *model* au travers de la classe abstraite ```Calculus```. Celle-ci est
définie comme la classe servant à calculer la couleur d'un pixel via le nombre d'itérations, la fonction, et la limite. Le paramètre d'itération est toujours modifiable pour les deux ensembles. La fonction polynôme est modifiable pour Julia dans les modes graphique et interactif. La limite n'est modifiable dans aucun mode, faute de preuve mathématique, mais la classe Calculus est conçue pour pouvoir la modifier si besoin. <br />

Pour les deux ensembles, il est possible d'effectuer le calcul en mode normal avec un nombre fixe d'itérations ou en mode infini.
Il est possible de modifier le zoom ainsi que la position de l'origine dans le repère orthonormé.

### Mode arguments ligne de commandes
Le mode arguments permet d'être utilisé dans un terminal. Il est surtout conçu pour tourner dans un script d'automatisation de tâches. Il permet de définir la taille de l'image, le nombre d'itérations, le zoom, la position de l'origine, le nom et pour les Julia, le complexe c de la fonction f(x+1) = f(x)^2 + c. Dans ce mode, seule cette fonction quadratique est disponible. Une fois les arguments mis en place, l'image est exportée dans save/name.png. Le programme s'utilise de la façon suivante :  

``` sh
    # Mode ligne de commandes
    user$ fractal <julia|mandelbrot> width height reel img zoom(%) iteration origin_x origin_y name
```

### Mode interactif ligne de commandes
Le mode interactif est conçu pour la ligne de commandes pour concevoir les ensembles de façon plus simple que par les arguments en ligne de commandes. Il permet la création de polynomes en choisissant la valeur des coefficients devant. Ceux-ci sont des doubles qui s'écrivent sous la forme a,b avec a la partie entière et b la partie décimale.

``` sh
    # Mode ligne de commandes
    user$ fractal interactif
```

### Mode graphique
Il permet de visualiser les ensembles dans une fenêtre. Il s'agit d'un mode interactif où les images ne sont pas exportables.
Il est possible de choisir le type de l'ensemble grâce au bouton plus. Pour Mandelbrot, il est possible de définir le nombre d'itérations. Pour Julia, il est possible de définir le nombre d'itérations ou d'avoir le mode de calcul infini. Il est possible de définir le polynome en ajoutant les x^n au fur et à mesure (sous forme de doubles). Il est aussi possible de choisir la constante complexe des Julia. Par défaut, la fonction est f(x+1) = f(x)^2 + c. Dans le panneau latéral, le bouton play lance le calcul de l'ensemble. Un simple appui sur stop interrompt le calcul (utile surtout dans le mode infini). Il est possible de déplacer l'origine du repère grace aux flèches directionnelles et de zoomer grace à + et à -. Pour supprimer un ensemble, il suffit de cliquer sur la croix.

### Multhreading
Tous les calculs sont faits en multithread. Ils utilisent le maximum de coeurs moins un qui s'occupera de gérer l'interface graphique ou d'autres ressources. Les calculs ne peuvent être effectués en monothread.

### Mode infini
Il est possible de lancer un calcul qui s'arrête quand la fonction diverge ou est sûr de converger. Pour cela, on vérifie si le module de la différence est supérieur à 1 ou si l'on a trouvé un point fixe. Il est possible d'arrêter le calcul grâce au bouton stop. 

## Répartition des rôles

Étienne s'est occupé de l'ensemble du modèle (backend). Pablito a géré toute la partie graphique.

## Annexe : utiliser javaFX 11 pour le script fractal
* Installer openjdk 11 : https://dzone.com/articles/installing-openjdk-11-on-ubuntu-1804-for-real
* Télécharger javaFX11 : https://gluonhq.com/products/javafx/
* Unziper javaFX11 à la racine du projet 

## Annexe : pour utiliser javaFX 11 (avec Intellij IDEA): 
* Installer openjdk 11 : https://dzone.com/articles/installing-openjdk-11-on-ubuntu-1804-for-real
* Télécharger javaFX11 : https://gluonhq.com/products/javafx/
* Unziper javaFX11
* Lancer intellij puis aller dans : 
`file -> project structure`
* Dans l'onglet SDK's cliquer sur "+" , "ajouter JDK" et ajouter votre JDK 11.
* Dans l'onglet project, choisir le jdk 11 et le language level 11
* Toujours dans project structure, aller dans `libraries` -> `+` -> `Java` -> puis selectionner votre dossier `javafx-sdk-11/lib`
* Retour au menu principal. `run -> edit configuration` et dans `VM options` mettre :`--module-path PATH_TO/javafx-sdk-11.0.1/lib --add-modules=javafx.controls,javafx.fxml` en modifiant le début avec votre chemin vers le dossier lib de javaFX
* Finalement : `build` -> `rebuild-project`<br/>

## Copyrights
© BELLO MARAIS