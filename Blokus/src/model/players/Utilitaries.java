package model.players;

import java.awt.*;

/**
 * Classe utilitaire possédant des fonctions utilisées par plusieurs classes
 *
 * @author Blokus_1
 */
public class Utilitaries {

    /**
     * On prend un tableau d'objet et on applique un miroir selon un axe vertical
     * @param tab tableau d'objet qu'on prend
     * @param <T> type du tableau d'objet
     * @return tableau après avoir subi l'opération de miroir
     */
    public static <T> T[][] mirrorVertical(T[][] tab){
        T ref;
        for (int i = 0; i < tab.length; i++) {
            for (int j = 0; j < tab[i].length / 2; j++) {
                ref = tab[i][j];
                tab[i][j]=tab[i][tab[i].length-1-j];
                tab[i][tab[i].length-1-j]=ref;
            }
        }
        return tab;
    }

    /**
     * On prend un tableau d'objet et on applique un miroir selon un axe horizontal
     * @param tab tableau d'objet qu'on prend
     * @param <T> type du tableau d'objet
     * @return tableau après avoir subi l'opération de miroir
     */
    public static <T> T[][] mirrorHorizontal(T[][] tab){
        T[] ref;
        for (int i = 0; i < tab.length; i++) {
            for (int j = 0; j < tab[i].length / 2; j++) {
                ref = tab[i];
                tab[i]=tab[tab.length-1-i];
                tab[tab.length-1-i]=ref;
            }
        }
        return tab;
    }

    /**
     * On prend un tableau d'objet et on applique une rotation de 90 degré
     * @param tab tableau de Point
     * @return tableau après avoir subi l'opération de miroir
     */
    public static Point[][] rotate(Point[][] tab){
        Point[][] rotate = new Point[tab.length][tab[0].length];
        for (int i = 0; i < tab.length; i++) {
            for (int j = 0; j < tab[i].length; j++) {
                rotate[j][i]=tab[i][tab.length-1-j];
            }
        }
        return rotate;
    }

}
