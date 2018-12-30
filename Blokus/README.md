# Blokus - mai 2018

![license](https://img.shields.io/github/license/mashape/apistatus.svg?style=for-the-badge)
![GitHub release](https://img.shields.io/github/release/elusyo/Blokus.svg?style=for-the-badge)

### Partie Technique

  #### Makefile
   - L'ensemble des commandes de compilation, lancement de jeu, documentation et jeu peuvent être lancées depuis
    la commande make. Pour afficher les options, utiliser la commande "make".

  #### Lancement du jeu
   - Pour construire les données de bases de données, il faut utiliser la commande "make build" 
   - Pour lancer le jeu dans le terminal, il faut utiliser la commande "make terminal".
   - Pour lancer le jeu dans sa version graphique, il faut utiliser la commande "make graphic".
   - Ces commandes lancent la compilation du jeu puis lancent le jeu dans le terminal ou l'interface graphique.

  #### Documentation
   - Pour créer la documentation, il faut lancer la commande "make docs". Une fois créée, la documentation se
    trouve au format .html dans le dossier docs.
   - Vous pouvez les ouvrir avec un navigateur web tels que Firefox, Opéra, etc.

  ##### Suppression des binaires et de la documentation  
   - Si vous souhaitez supprimer les fichiers .class de la compilation et .html de la documentation, il suffit de faire 
    la commande "make clear"

### Partie Jeu
    
   #### Règle
   - Les règles du Blokus sont disponibles dans les deux versions du jeu.
    
   #### Attention
   - Pour la version terminal, vérifiez que votre terminal supporte l'UTF-8 sinon l'affichage des couleurs ne fonctionnera pas.
   - Pour utiliser le machine learning, si vous êtes sur un réseau avec un pare-feu tel que celui de l'Université Paris 7,
   il faut utiliser un VPN pour permettre la connexion avec la base de données.
   - Le machine learning ne peut être entrainé que dans le terminal, option7.
### Développeurs
   Le présent jeu Blokus a été développé par l'équipe Blokus 1 pour le projet de quatrième semestre de l'Université Paris Diderot.
   Cette équipe est composée de :
    
   - Pablito BELLO
   - Xavier DURAND
   - Joris JEAN-CHARLES
   - Étienne MARAIS




