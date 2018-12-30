package model.players;

import controller.Message;

import java.util.LinkedList;
import java.util.Random;

/**
 * Classe de gestion de la stratégie de niveau moyen
 * <br />
 * Prend un coup au hasard dans parmi les possibilités des pièces ayant les poids les plus élevés
 * @author Blokus_1
 */
public class Medium implements Strategy{


    @Override
    public String getName() {
        return "IA medium";
    }

    /** retourne le coefficient de score pour la bdd */
    public double getCoef () { return 1.5; }

    /**
     * Il prend un message au hasard parmi les messages contenant les plus grosses pièces disponibles
     * @param possibilite ensemble des pièces possibles à jouer
     * @param joueur la place dans le tableau du joueur
     * @return Message correspondant à la stratégie de l'IA medium
     */
    @Override
    public Message getStrategy(LinkedList<LinkedList<Message>> possibilite, int joueur) {
        Random random = new Random();
        if(isNull(possibilite)) return null;
        LinkedList<Message> messages = new LinkedList<>();
        if(possibilite.size() >= 3) {
            messages.addAll(possibilite.get(possibilite.size() - 1));
            messages.addAll(possibilite.get(possibilite.size() - 2));
            messages.addAll(possibilite.get(possibilite.size() - 3));
        } else if(possibilite.size() == 2) {
            messages.addAll(possibilite.get(possibilite.size() - 1));
            messages.addAll(possibilite.get(possibilite.size() - 2));
        } else messages.addAll(possibilite.getLast());
        return messages.get(random.nextInt(messages.size()));
    }
}