package model.players;

import controller.Message;

import java.util.LinkedList;
import java.util.Random;


/**
 * Stratégie du joueur niveau facile
 * <br/>
 * Pose les plus petites pièces en évitant d'aller vers le centre
 * @author Blokus_1
 */
public class Easy implements Strategy{

    @Override
    public Message getStrategy (LinkedList<LinkedList<Message>> possibilite, int joueur) {
        if (isNull(possibilite)) { return null; }
        return minimumCote(possibilite.getFirst(), joueur);
    }

    @Override
    public String getName() {
        return "IA easy";
    }

    @Override
    public double getCoef() {
        return 1.25;
    }

/******************************************
 * Méthodes statiques communes aux IAs Easy  *
 ******************************************/

    /**
     * Donne la position la plus proche du coté
     * @param messages liste de possiblités
     * @param joueur le numéro du joueur dans le tableau
     * @return Message contenant la position
     */
    private static Message minimumCote (LinkedList<Message> messages, int joueur) {
        int rand = new Random().nextInt(2);
        switch (joueur) {
            case 0:
                return getMinFirstPlayer(messages, rand);
            case 1:
                return getMinSecondPlayer(messages, rand);
            case 2:
                return getMinThirdPlayer(messages, rand);
            case 3:
                return getMinFourthPlayer(messages, rand);
            default:
                return null;
        }
    }

    /**
     * Retourne l'abscisse ou l'ordonnée la plus petite
     * @param messages liste des positions possibles
     * @param side 0 abscisse / 1 ordonnée
     * @return
     */
    private static Message getMinFirstPlayer(LinkedList<Message> messages, int side){
        Message ret = messages.getFirst();
        if (side == 0) {
            // l'abscisse la plus petite
            for (Message i : messages)
                if(i.getPoint().getX() < ret.getPoint().getX()){ ret = i; }
        }
        if (side == 1) {
            // l'ordonnée la plus petite
            for (Message i : messages)
                if(i.getPoint().getY() < ret.getPoint().getY()){ ret = i; }
        }
        return ret;
    }

    /** Même qu'au dessus pour le joueur deux */
    private static Message getMinSecondPlayer (LinkedList<Message> messages, int side) {
        Message ret = messages.getFirst();
        if (side == 0) {
            // l'abscisse la plus grande
            for (Message i : messages)
                if (i.getPoint().getX() > ret.getPoint().getX()) { ret = i; }
        }
        if(side == 1){
            // l'ordonnée la plus petite
            for (Message i : messages)
                if (i.getPoint().getY() < ret.getPoint().getY()) { ret = i; }
        }
        return ret;
    }

    /** Même qu'au dessus pour le joueur trois */
    private static Message getMinThirdPlayer (LinkedList<Message> messages, int rand) {
        Message ret = messages.getFirst();
        if(rand == 0){
            // l'abscisse la plus grande
            for (Message i : messages)
                if (i.getPoint().getX() > ret.getPoint().getX()) { ret = i; }
        }
        if(rand == 1){
            // l'ordonnée la plus grande
            for (Message i : messages)
                if (i.getPoint().getY() > ret.getPoint().getY()) { ret = i; }
        }
        return ret;
    }

    /** Même qu'au dessus pour le joueur deux */
    private static Message getMinFourthPlayer (LinkedList<Message> messages, int rand) {
        Message ret = messages.getFirst();
        if (rand == 0) {
            // l'abscisse la plus petite
            for (Message i : messages)
                if (i.getPoint().getX() < ret.getPoint().getX()) { ret = i; }
        }
        if (rand == 1) {
            // l'ordonnée la plus grande
            for (Message i : messages)
                if (i.getPoint().getY() > ret.getPoint().getY()) { ret = i; }
        }
        return ret;
    }
}