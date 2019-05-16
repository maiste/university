# Floodus

Ce projet consiste à implémenter un tchat en réseau à l'aide du principe d'inondation fiable.
L'inondation fiable fonctionne de la façon suivante :
 - Tous les membres du protocole sont liés en pair à pair.
 - Lorsqu'un membre reçoit une donnée, il l'inonde à tous ses pairs symétriques.
 - Il attend un message d'acquittement de tous les pairs qu'il a inondés, et continue d'envoyer les données tant qu'il n'a pas reçu d'acquittement.
 - Pour que deux pairs soient pairs entre eux, on procède à un échange de données d'identification (id).

Pour identifier que nous communiquons bien avec les personnes implémentant le protocole, nous avons défini que tout TLV était encapsulé dans un datagramme ayant pour deux premiers octets :
 - premier octet, noté `magic`, à 93.
 - deuxième octet, appelé `version`, à 2.

## Prérequis

Pour pouvoir lancer le programme, il faut que les bibliothèques C suivantes soient installées :
 - **ncurses**: interface graphique
 - **math.h** : librairie math de c
 - **pthread** : utilisation de threads

Il est aussi nécessaire d'avoir les programmes suivants pour pourvoir compiler ou afficher les rapports 
de couverture de code:
 - **Makefile** : sert pour compiler le programme
 - **lcov** : permet d'obtenir la *couverture* du programme (surtout pour le développement)
 - être sur un ordinateur sous **Linux** : nous ne savons pas si le programme marche sur Windows (l'API socket étant légèrement différente) ou Mac, mais il ne marche pas sur téléphone.

## Compilation

Tout d'abord, il faut télécharger le dossier. Vous pouvez soit déziper le .zip soit cloner le dossier git. Pour cela, il faut utiliser *git*, et lancer, dans le dossier que vous voulez, la commande :

```
        git clone https://github.com/kolibs/Floodus.git
```


Ensuite, vous vous mettez dans le dossier `Floodus/`, puis vous lancez la commande `make floodus`, et enfin vous exécutez le fichier `floodus` :
```
                cd Floodus
                make floodus
                ./floodus
```

Vous pouvez, de plus, obtenir la *couverture du code* de notre programme en lançant les commandes suivantes :
```
                make coverage
```
Cela vous permet de savoir quelles parties de notre code vous avez testé. Il faut avoir préalablement lancé le programme. Ensuite, pour voir le résultat, vous affichez dans votre navigateur préféré le fichier `coverage.html/index.html`.

Enfin, il est possible de voir toute la documentation du programme en lançant la commande :
```
                make doc
```
Et ensuite, toujours avec votre navigateur préféré, vous ouvrez le fichier `doc/html/index.html`.

Pour pouvoir débuguer, il est possible de compiler le programme avec les make suivants. Pour que les "logs" soient affichés dans l'interface graphique, il faut utiliser : 
```
    make debug
```
Pour écrire les logs de debug dans le fichier \textit{debug\_floodus.log}, il faut lancer la commande :  
```
    make log
```
