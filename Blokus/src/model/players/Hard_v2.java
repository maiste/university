package model.players;

import controller.Message;
import model.Blokus;
import model.Piece;

import java.awt.*;
import java.util.LinkedList;
import java.util.Random;

/**
 * Classe de gestion de la stratégie de niveau difficile
 * <br>
 * La stratégie de Hard_v2 est de poser la pièce offrant le plus de possibilités
 * et dont la distance avec le coin du joueur le plus fort est minimale.
 * Son but est de maximiser les pièces qu'il pose tout en voulant bloquer le plus fort.
 *
 * @author Blokus_1
 */
public class Hard_v2 implements Strategy{

    private Blokus game;
    private LinkedList<Message> memoire = new LinkedList<>();
    private Blokus copieGame;
    private int turnConstant = 5;

    private static double pourcentageOfDistance = 0.1;

    /**
     * Contructeur
     * @param game jeu du blokus en cours
     */
    public Hard_v2(Blokus game) {
        this.game = game;
        this.copieGame = new Blokus(game);
    }

    /** getter */
    public double getCoef () {
        return 1.75;
    }

    /**
     * Message que renvoie la stratégie
     * @param possibilite ensemble des pièces possibles à jouer
     * @param joueur la place dans le tableau du joueur
     * @return message qu'a calculé la stratégie
     */
    @Override
    public Message getStrategy(LinkedList<LinkedList<Message>> possibilite, int joueur) {
        copieGame.updateCalculBlokus(game);
        if(copieGame.isFirstTurn()){
            for (LinkedList<Message> messageLinkedList : ((IAPlayer) copieGame.getCurrentPlayer()).possibleStrike()){
                memoire.addAll(messageLinkedList);
            }
        }
        return this.getMessage();
    }

    @Override
    public String getName() {
        return "IA hard";
    }

    /**
     * On applique la strategie de l'IA Hard :
     * On regarde dans la liste de messages en memoire celui qui engendre
     * la liste de possibilités la plus grande.
     * @return le message permettant le plus de coup au tour d'après.
     */
    public Message getMessage(){
        long originTime = System.currentTimeMillis();
        Message ref = null;
        int max = 0;
        LinkedList<Message> memMax = new LinkedList<>();

        LinkedList<Message> mem = new LinkedList<>();
        int provisoire = 0;

        cleanMemory();

        LinkedList<Piece> pieceLinkedList = getHigherWeightPiece(5);

        for (Message message : memoire) {
            if(pieceLinkedList.contains(message.getPiece())) {
                copieGame.updateBoard(message, true);
                copieGame.setTurn(1);
                mem = listOfPossibleMessage(message);
                provisoire = mem.size() + getNumberOfOther();
                ref = this.minnimumDistanceWithStronger(ref,max,message,provisoire);
                if (ref == message){
                    max = provisoire;
                    memMax = mem;
                }
                copieGame.cleanBox(message);
                copieGame.getCurrentPlayer().addPiece(message.getPiece());
                copieGame.setTurn(-1);
            }
        }

        cleanMemory(ref);
        memoire.addAll(memMax);
        long time = (System.currentTimeMillis() - originTime) / 100;
        System.out.println("Réponse en " + time + " s");
        return ref;
    }

    /**
     * On calcule tous les messages possibles sur les nouveaux coins que nous procure
     * la piece du message donné en paramètre.
     * @param message Message correspondant à la pièce qu'on a placé et à sa position
     * @return liste des messages engendrés en ayant appliqué le message donné en parametre
     */
    public LinkedList<Message> listOfPossibleMessage(Message message) {
        LinkedList<Message> messageLinkedList = new LinkedList<>();
        LinkedList<Point> points = message.getPiece().getCornerOfPiece();
        for (Point ref : points){
            ref.translate((int) message.getPoint().getX(), (int) message.getPoint().getY());
            for (LinkedList<Piece> pieceLinkedList : copieGame.getCurrentPlayer().getPieces()) {
                for (Piece piece : pieceLinkedList) {
                    for (int i = 0; i < 2; i++) {
                        for (int j = 0; j < 4; j++) {
                            LinkedList<Message> test = copieGame.allPositionByPiece(ref, (Piece) piece.clone());
                            test.removeIf((Message message1) -> memoire.contains(message1));
                            messageLinkedList.addAll(test);
                            piece.rotate();
                        }
                        piece.mirror();
                    }
                }
            }
        }
        return messageLinkedList;
    }

    /**
     * On regarde dans la memoire et on supprime tous les messages
     * possèdant la même pièce que le message donné en paramètre.
     * @param message Message dont on veut supprimer tous ceux qui ont la même pièce dans la mémoire
     */
    public void cleanMemory(Message message){
        if(! (message == null)){
            memoire.removeIf((Message message1) -> message.getPiece().equals(message1.getPiece()));
        }
    }

    /**
     * On supprime de la mémoire tous les messages qui ne sont plus jouables
     */
    public void cleanMemory(){
        memoire.removeIf((Message message1) -> ! copieGame.verifyPosition(message1));
    }

    /**
     * On calcule le nombre de messages valide dans la mémoire (dans l'état en cours du board).
     * Cette fonction est appelée après avoir simulé la pose d'une pièce.
     * @return le nombre de messages valides dans la mémoire.
     */
    public int getNumberOfOther(){
        int compteur = 0;
        for (Message message : memoire){
            if(copieGame.verifyPosition(message)) compteur++;
        }
        return compteur;
    }

    /**
     * on récupère les 'numberOfPiece' plus grosses pièces.
     * Si on pioche ses pièces dans une liste plus grande que 'numberOfPiece', on les choisit au hasard
     * @param numberOfPiece nombre de pièces qu'on cherche
     * @return 'numberOfPiece' pièces les plus grosses.
     */
    private LinkedList<Piece> getHigherWeightPiece(int numberOfPiece){
        LinkedList<LinkedList<Piece>> piecesOfPlayer = new LinkedList<>();
        for (LinkedList<Piece> list : copieGame.getCurrentPlayer().getPieces()){
            LinkedList<Piece> linkedList = new LinkedList<>();
            linkedList.addAll(list);
            piecesOfPlayer.add(linkedList);
        }
        Random random = new Random();
        LinkedList<Piece> higherPiece = new LinkedList<>();

        int i = 4;
        while (higherPiece.size() < numberOfPiece){
            if(i < 0){
                break;
            }
            if(piecesOfPlayer.get(i).size() > 0){
                Piece piece = piecesOfPlayer.get(i).get(random.nextInt(piecesOfPlayer.get(i).size()));
                higherPiece.add(piece);
                piecesOfPlayer.get(i).remove(piece);
            }
            else {
                i--;
            }
        }
        return higherPiece;
    }

    /**
     * On regarde lequel des deux Messages donnés en argument est le plus intéressant suivant
     * la stratégie de l'IA Hard
     * @param betterChoice le meilleur choix parmi les précédents choix possibles
     * @param biggest le nombre de possibilités qu'engendre betterChoice
     * @param otherChoice le deuxième Message avec lequel on va comparer betterChoice
     * @param other le nombre de possibilités qu'engendre otherChoice
     * @return le plus intéressant des deux Messages donnés en argument
     */
    private Message minnimumDistanceWithStronger(Message betterChoice, int biggest, Message otherChoice, int other){
        if(betterChoice == null) return otherChoice;
        if(game.getTurn() >= turnConstant){
            if (biggest < other) {
                return otherChoice;
            } else if (biggest == other) {
                if (betterChoice.getPiece().weight() < otherChoice.getPiece().weight()) {
                    return otherChoice;
                }
            }
            else return betterChoice;
        }
        Point corner = strongerPlayer();
        int distance1 = betterChoice.getPiece().minimumOfDistanceWith(corner,betterChoice.getPoint());
        int distance2 = otherChoice.getPiece().minimumOfDistanceWith(corner,otherChoice.getPoint());
        if(coefOfDistance(distance1,betterChoice.getPiece().getPoints().size(),biggest) >= coefOfDistance(distance2,otherChoice.getPiece().getPoints().size(),other)){
            return betterChoice;
        }
        else return otherChoice;
    }

    /**
     * on utilise une fonction pour donner un coefficient à chaque coup, pour lui donner
     * plus ou moins de l'importance
     * @param distance distance de la pièce avec le coin du plus fort des autres joueurs
     * @param poids poids de la pièce
     * @param choix le nombre de possibilités qu'on a après avoir posé la pièce
     * @return résultat de la fonction
     */
    private double coefOfDistance(int distance, int poids, int choix){
        return choix*(1-((1/270.)*distance*(1-2*(poids/100.))));
    }

    /**
     * On cherche quel est le joueur avec le score le plus élevé.
     * Si on a une égalité de joueur ou si le joueur le plus fort est l'IA lui-même,
     * on préviligie celui qui a le coin le plus éloigné avec le coin de l'IA Hard
     * @return le coin correspond au joueur le plus fort
     */
    private Point strongerPlayer(){
        Player[] players = game.getPlayersByScore();
        Player me = game.getCurrentPlayer();
        Point cornerOfMe = game.getCornerPlayer(me.getId()-1);
        Player stronger = (players[0]==me)? players[1]:players[0];
        for (int i = 1; i < players.length; i++) {
            if(players[i] != me
                    && players[i] != stronger) {
                if (players[i].getScore() > stronger.getScore()) stronger = players[i];
                else if (players[i].getScore() == stronger.getScore()){
                    double distance1 = cornerOfMe.distance(game.getCornerPlayer(players[i].getId()-1));
                    double distance2 = cornerOfMe.distance(game.getCornerPlayer(stronger.getId()-1));
                    if (distance1 > distance2) stronger = players[i];
                }
            }
        }
        return game.getCornerPlayer(stronger.getId()-1);
    }
}