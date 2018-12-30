package model.players;

import controller.Message;
import model.Blokus;
import model.Piece;

import java.awt.*;
import java.util.LinkedList;

/**
 * Classe de gestion du joueur IA
 * @author Blokus_1
 */
public class IAPlayer extends Player{

    private Strategy strategy;
    protected Blokus game;


/*****************
 * Constructeurs *
 *****************/


    /**
     * Constructeur de l'IA
     * @param color la couleur de l'IA
     * @param id l'id de jeu de l'IA {1,2,3,4}
     * @param game la référence vers le plateau de jeu
     * @param strategy la stratégie à employer
     * @param name nom de l'IA
     */
    public IAPlayer(Color color, int id,Blokus game, Strategy strategy, String name) {
        super(color, id, name);
        this.game = game;
        if(strategy.needPlayer()){
            strategy.setPlayer(this);
        }
        this.strategy = strategy;
    }

/*******************
 * Actions de l'IA *
 *******************/

    /** on change la stratégie */
    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    /** retourne si on est un IA */
    @Override
    public boolean isIA() {
        return true;
    }

    /** on renvoie le coefficient de l'IA */
    public double getCoef () {
        return strategy.getCoef();
    }

    /** Retourne la pièce à jouer*/
     public Message getAnswer(){
        Message e;
        // on fait une distinction de cas avec l'IA Hard parce qu'il n'a pas besoin de calculer 
        // possibleStrike au debut de son tour, ni de l'indice du joueur
        if(strategy instanceof Hard) e = strategy.getStrategy(new LinkedList<>(),-1);
        else e = strategy.getStrategy(this.possibleStrike(),game.getPlayerTurn());
        if(e == null) this.setCanPlay(false);
        return e;
     }

    /**
     * On récupère les coordonnées de tous les coins où l'on peut poser un coin de pièce.
     * Il faut tester si une pièce marche en faisant une translation du point de réfèrence
     * de telle facon qu'un coin de la pièce doit être sur le point des coins disponibles.
     * @return la liste des coups possibles sous forme de message
     */
    public LinkedList<LinkedList<Message>> possibleStrike(){
        LinkedList<Point> coins = game.getPlayableCase();
        LinkedList<LinkedList<Message>> ret = new LinkedList<>();
        for (LinkedList<Piece> list : pieces){
            LinkedList<Message> mess = new LinkedList<>();
            for (Piece piece : list){
                for (Point a : coins) {
                    for (int i = 0; i < 2; i++) {
                        for (int j = 0; j < 4; j++) {
                            LinkedList<Message> test = game.allPositionByPiece(a,(Piece) piece.clone());
                            mess.addAll(test);
                            piece.rotate();
                        }
                        piece.mirror();
                    }
                }
            }
            if(! mess.isEmpty()) ret.add(mess);

        }
        return ret;
    }

    /**
     * On récupère les coordonnees de tous les coins où on peut poser un coin de piece.
     * Il faut tester si une pièce marche en faisant une translation du point de réfèrence
     * de telle façon qu'un coin de la pièce doit être sur le point des coins disponibles.
     * @param player Player dont on calcule les coups possibles
     * @return liste des coups possibles sous forme de message
     */
    public LinkedList<LinkedList<Message>> possibleStrike(Player player){
        LinkedList<Point> coins = game.getPlayableCase(player);
        LinkedList<LinkedList<Message>> ret = new LinkedList<>();
        for (LinkedList<Piece> list : pieces){
            LinkedList<Message> mess = new LinkedList<>();
            for (Piece piece : list){
                for (Point a : coins) {
                    for (int i = 0; i < 2; i++) {
                        for (int j = 0; j < 4; j++) {
                            LinkedList<Message> test = game.allPositionByPiece(a,(Piece) piece.clone());
                            mess.addAll(test);
                            piece.rotate();
                        }
                        piece.mirror();
                    }
                }
            }
            if(! mess.isEmpty()) ret.add(mess);

        }
        return ret;
    }

    @Override
    public void updateLearning() {
        strategy.updateLearning();
    }
}