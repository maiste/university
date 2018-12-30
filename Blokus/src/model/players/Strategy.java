package model.players;

import controller.Message;

import java.util.LinkedList;

/**
 * Interface qui permet de définir les stratégies du joueur IA
 * @author Blokus_1
 */
public interface Strategy {

    /**
     * Donne le Message correspondant au coup de l'IA
     * @param possibilite ensemble des pièces possibles à jouer
     * @param joueur la place dans le tableau du joueur
     * @return Message contenant la pièce et la position à jouer
     */
    Message getStrategy(LinkedList<LinkedList<Message>> possibilite, int joueur);

    /**
     * nom de l'IA
     * @return nom correspondant à l'IA
     */
    String getName();

    /**
     * Le coefficient de l'IA en fonction de sa force
     * @return coefficient de l'IA
     */
    double getCoef();

    /**
     * Mise à jour de l'IA Learning
     */
    default void updateLearning(){}

    /** Test si le contenu de la liste est vide */
    default boolean isNull (LinkedList<LinkedList<Message>> possibilite) {
        if (possibilite == null) { return true; }
        for(LinkedList<Message> tmp : possibilite){
            if (!tmp.isEmpty()) { return false; }
        }
        return true;
    }

    /**
     * Renvoie si l'IA a besoin d'une référence vers son Player
     * @return true si l'IA en a besoin
     */
    default boolean needPlayer(){
        return false;
    }

    /**
     * On change la référence du Player de l'IA
     * @param player le Player de l'IA
     */
    default void setPlayer(Player player){}

}
