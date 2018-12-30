package model.players;

import controller.Message;
import model.Blokus;
import model.Piece;

import java.awt.*;
import java.util.LinkedList;
import java.util.Random;

/**
 * Classe de gestion de la stratégie de niveau difficile
 * <br />
 * Maximise le nombre de possibilités qu'il engendre en posant une pièce
 * @author Blokus_1
 */
public class Hard implements Strategy{

    private Blokus game;
    private LinkedList<Message> memoire = new LinkedList<>();
    private Blokus copieGame;

    /** Constructeur */
    public Hard(Blokus game) {
        this.game = game;
        this.copieGame = new Blokus(game);
    }

    /** retourne le coefficiant pour le calcul du score */
    public double getCoef () {
        return 1.75;
    }

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
     * On applique la stratégie de l'IA Hard :
     * On regarde dans la liste de message en mémoire celui qui engendre
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
        memoire.addAll(memMax);
        return ref;
    }

    /**
     * On calcule tous les messages possibles sur les nouveaux coins que nous procure
     * la piece du message donné en paramètre.
     * @param message Message correspondant à la piece qu'on a placée et sa position
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
     * On regarde dans la mémoire et on supprime tous les messages
     * possèdant la même pièce que le message donné en paramètre.
     * @param message Message dont on veut supprimer tous ceux qui ont la même pièce dans la mémoire
     */
    public void cleanMemory(Message message){
        if(! (message == null)){
            memoire.removeIf((Message message1) -> message.getPiece().equals(message1.getPiece()));
        }
    }

    /**
     * On supprime tous les messages qui ne sont plus jouables de la mémoire
     */
    public void cleanMemory(){
        memoire.removeIf((Message message1) -> ! copieGame.verifyPosition(message1));
    }

    /**
     * On calcule le nombre de messages valides dans la mémoire (dans l'état en cours du board).
     * Cette fonction est appelée après avoir simulé la pose d'une piece.
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
     * Si on pioche ces pièces dans une liste plus grande que 'numberOfPiece', on les choisit au hasard
     * @param numberOfPiece nombre de pièces qu'on cherche
     * @return 'numberOfPiece' pieces les plus grosses.
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
        while (higherPiece.size() < numberOfPiece) {
            if (i < 0) { break; }
            if (piecesOfPlayer.get(i).size() > 0) {
                Piece piece = piecesOfPlayer.get(i).get(random.nextInt(piecesOfPlayer.get(i).size()));
                higherPiece.add(piece);
                piecesOfPlayer.get(i).remove(piece);
            } else { i--; }
        }
        return higherPiece;
    }
}