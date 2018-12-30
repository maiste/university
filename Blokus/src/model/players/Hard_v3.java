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
 * Le but de cet IA est de maximiser les pièces qu'il pose tout en
 * minimisant les pièces que peuvent poser les autres joueurs.
 * Pour cela, il va calculer toutes ses possibilités et toutes celles
 * des autres joueurs, c'est la raison de sa lenteur.
 *
 * @author Blokus_1
 */
public class Hard_v3 implements Strategy{

    private Blokus game;
    private Blokus copieGame;
    private LinkedList<Message>[] memoires = new LinkedList[4];
    private Player player;

    public Hard_v3(Blokus game) {
        this.game = game;
        this.copieGame = new Blokus(game);
        for (int i = 0; i < memoires.length; i++) {
            memoires[i] = new LinkedList<>();
        }
    }

    @Override
    public boolean needPlayer() {
        return true;
    }

    @Override
    public void setPlayer(Player player) {
        this.player = player;
    }

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
                memoires[0].addAll(messageLinkedList);
            }
        }
        return this.getMessage();
    }

    @Override
    public String getName() {
        return "IA hard";
    }

    /**
     * On applique la stratégie de l'IA Hard :
     * On regarde dans la liste de messages en mémoire celui qui engendre
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
        if(! game.isFirstTurn()) updateListOfMessageForOther();
        cleanMemory();

        LinkedList<Piece> pieceLinkedList = getHigherWeightPiece(5);

        for (Message message : memoires[0]) {
            if(pieceLinkedList.contains(message.getPiece())) {
                copieGame.updateBoard(message, true);
                copieGame.setTurn(1);
                mem = listOfPossibleMessage(message);
                provisoire = mem.size() + getNumberOfMyMessages() - getNumberOfOtherPlayer();
                if (max < provisoire) {
                    ref = message;
                    memMax = mem;
                    max = provisoire;
                } else if (max == provisoire) {
                    if (ref != null && ref.getPiece().weight() < message.getPiece().weight()) {
                        ref = message;
                        max = provisoire;
                    }
                }
                copieGame.cleanBox(message);
                copieGame.getCurrentPlayer().addPiece(message.getPiece());
                copieGame.setTurn(-1);
            }
        }

        cleanMemory(ref);
        memoires[0].addAll(memMax);
        long time = (System.currentTimeMillis() - originTime) / 100;
        System.out.println("Réponse en " + time + " s");
        return ref;
    }

    /**
     * On calcule tous les messages possibles sur les nouveaux coins que nous procure
     * la pièce du message donné en paramètre.
     * @param message Message correspondant à la piece qu'on a placé et à sa position
     * @return liste des messages engendrés en ayant appliqué le message donné en paramètre
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
                            test.removeIf((Message message1) -> memoires[0].contains(message1));
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
            memoires[0].removeIf((Message message1) -> message.getPiece().equals(message1.getPiece()));
        }
    }

    /**
     * On supprime tous les messages qui ne sont plus jouables de la mémoire
     */
    public void cleanMemory(){
        memoires[0].removeIf((Message message1) -> ! copieGame.verifyPosition(message1));
    }

    /**
     * On calcule le nombre de messages valides dans la mémoire (dans l'état en cours du board).
     * Cette fonction est appelée après avoir simulé la pose d'une pièce.
     * @return le nombre de messages valides dans la memoire.
     */
    public int getNumberOfMyMessages(){
        int compteur = 0;
        for (Message message : memoires[0]){
            if(copieGame.verifyPosition(message)) compteur++;
        }
        return compteur;
    }

    /**
     * On calcule le nombre de messages valides dans la mémoire (dans l'état en cours du board).
     * Cette fonction est appelé après avoir simulé la pose d'une pièce.
     * @return le nombre de message valide dans la memoire.
     */
    public int getNumberOfOtherPlayer(){
        int compteur = 0;
        for (int i = 1; i< memoires.length;i++) {
            for (Message message : memoires[i]) {
                if (copieGame.verifyPosition(message)) compteur++;
            }
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
     * On met à jour la liste des possibilités des autres joueurs.
     */
    public void updateListOfMessageForOther(){
        for (int i = 0; i < 3; i++) {
            for(LinkedList<Message> messages : ((IAPlayer)player).possibleStrike(getNextPlayer())){
                memoires[i+1].addAll(messages);
            }
        }
        getNextPlayer();
    }

    /**
     * on récupère le joueur suivant sans changer le tour.
     * @return le prochain joueur
     */
    public Player getNextPlayer(){
        game.updatePlayer();
        if(game.getPlayerTurn() == 0) game.setTurn(-1);
        return game.getCurrentPlayer();
    }
}