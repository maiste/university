---
title: Good Duck Transfert Client and Server
author: Étienne Marais - Benjamin Viau
geometry: margin=3cm
---

## Sommaire

 - Protocole
 - Prérequis
 - Informations sur le manuel
 - Compilation
    - Serveur
    - Client
 - Fonctionnement
    - Serveur
    - Client
 - Détails l'implémentation
    - Parties communes
    - Serveur
    - Client

## Protocole

Le répertoire suivant contient le code qui implémente le protocole GDPT pour le côté serveur et pour le côté client.
Le fichier contenant le protocole se trouve ici : [docs/rfc.txt](docs/rfc.txt).

## Prérequis

Votre système doit posséder les outils suivant installés :

- Java 11 ~openjdk11
- Make

## Informations sur la lecture du manuel

Dans le manuel, nous allons distinguer le terminal de l'invite de commandes du client. Nous utiliserons les symboles suivants
pour faire la distinction entre les deux :

### Pour le shell :

```sh
 $ <command>
```

### Pour l'invite de commande du client :

```
>> <command>
```

## Compilation

Pour compiler, il faut se placer à la racine du dossier, là où se trouve le **Makefile**.


### Serveur

Pour compiler et lancer le serveur sur le port **1027**, il faut exécuter la commande suivante :

```sh
  $ make server
```

Pour le lancer sur un autre port, il faut faire comme suit :

```sh
  $ make server SERVER_PORT=<port>
```

### Client

Pour compiler et lancer le client sur le port **1027** et sur l'adresse **127.0.0.1** :

```sh
 $ make client
```

Pour lancer le client sur une autre adresse et sur un autre port, il faut lancer avec :

```sh
 $ make client GTDP_addr=<addr> GDTP_port=<port>
```

Par défaut, le client n'affiche pas les paquets reçus pour permettre une meilleure lisibilité
à l'utilisateur. Il est cependant possible de les voir grâce au paramètre suivant :

```sh
  $ make client DEBUG=yes <options>
```

Nous avons aussi mis en ligne un serveur accessible de n'importe où qui stocke des annonces depuis le 2 Novembre.
Pour vous y connectez, vous devez taper la commande suivante :

```
 $ make client GDTP_addr=psi.maiste.fr
```

### Nettoyer le répertoire

Afin d'éliminer les *\*.class* il est possible d'utiliser la commande suivante :

```sh
 $ make clean
```

## Fonctionnement

### Serveur

Une fois lancé, le serveur fonctionne de façon autonome et affiche les logs
de son exécution. Il peut être arrêté grâce à un **CTRL+C**.

### Client

#### Interface

L'interface client se compose de deux onglets:

- Le terminal
- Le chat

Vous pouvez naviguer entre les deux onglets grâce aux flèches directionnelles et à la touche Tab.
Toutes les commandes doivent être rentrées dans la partie *Terminal*. Pour la
partie *chat*, une fois que vous avez essayé de communiquer avec un pair via
la commande talk, il est possible de voir la liste des pairs avec la flèche en haut
à droite. Le pair indiqué est celui avec qui vous êtes en train de discuter.

#### Aide

Pour afficher l'aide dans l'interpréteur, il faut utiliser la commande suivante :

```
>> help
```

#### Connexion

Le client fonctionne en ligne de commandes une fois lancé. Il faut d'abord se connecter
avec un identifiant pour pouvoir effectuer sa première connexion au serveur en utilisant
la commande suivante dans l'interpréteur du client :

```
>> connect [USERNAME]
```

Pour les connexions suivantes, vous pouvez simplement effectuer la commande suivante:

```
>> connect
```

En effet, un token est créé et ajouté au répertoire *\$HOME* lors de la première connexion
dans **\$HOME/.config/gdtp/token**. Si vous voulez changer d'utilisateur, il faut refaire
la manipulation avec le connect [USERNAME].

#### Quitter

Pour quitter le logiciel et vous déconnecter, vous pouvez utiliser la commande suivante :

```
>> exit
```

#### Domaines

Pour afficher les domaines disponibles sur le serveur, il faut faire la commande suivante :

```
>> domains
```

#### Annonces

Pour obtenir toutes les annonces d'un domaine, il faut utiliser la commande suivante :

```
>> ancs [DOMAINE]
```

#### Propres annonces

Pour obtenir l'ensemble des annonces que vous avez postées, il faut taper la commande suivante :

```
>> own
```

#### Création

Pour poster une nouvelle annonce, il faut lancer l'éditeur intéractif :

```
>> post
```

Le prix s'écrit avec le format suivant : `13.50` correspond à 13.50€.

#### Mise à jour

Pour mettre à jour une annonce, il faut écrire :

```
>> update [ID ANNONCE]
```

#### Suppression

Pour supprimer une annonce sur le serveur qui vous appartient, il faut lancer la commande suivante :

```
 >> delete [ID ANNONCE]
```

#### Récupération de l'IP

Il est possible de récupérer l'ip d'un collaborateur pour le contacter directement.
La commande à utiliser est la suivante :

```
>> ip [ID ANNONCE]
```

### Ouvrir le chat

Il est possible d'ouvrir une discussion avec quelqu'un qui est connecté sur le serveur de discussion
grâce à la commande suivante :

```
  talk [ID ANNONCE]
```

### Discuter avec quelqu'un de connecté

Une fois que l'action `talk` a été faite, vous pouvez utiliser l'onglet de chat pour discuter avec la personne
que vous voulez contacter. Cependant, si celle-ci se déconnecte, elle ne recevera plus vos messages et ceux-
ci seront perdus.
