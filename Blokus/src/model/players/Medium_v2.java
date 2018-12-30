package model.players;

import controller.Message;

import java.util.LinkedList;
import java.util.Random;

/**
 * Classe de gestion de la stratégie aléatoire sous le nom de Medium
 * <br />
 * Prend une pièce au hasard
 * @author Blokus_1
 */
public class Medium_v2 implements Strategy{


    @Override
    public String getName() {
        return "IA aleatoire";
    }

    @Override
    /** retourne  le coefficient du score pour l'enregistrement dans la bdd */
    public double getCoef () { return 1.5; }

    /**
     * Il prend un message au hasard parmi les messages disponibles
     * @param possibilite ensemble des pièces possibles à jouer
     * @param joueur la place dans le tableau du joueur
     * @return Message correspondant à la stratégie de l'IA medium
     */
    @Override
    public Message getStrategy(LinkedList<LinkedList<Message>> possibilite, int joueur) {
        Random random = new Random();
        if(isNull(possibilite)) return null;
        LinkedList<Message> messages = new LinkedList<>();
        for (LinkedList<Message> mess : possibilite){
            messages.addAll(mess);
        }
        return messages.get(random.nextInt(messages.size()));
    }
}
