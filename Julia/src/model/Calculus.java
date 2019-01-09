package model;

import javafx.scene.paint.Color;

import java.util.function.Function;

/**
 * Classe abstraite de gestion des unités de calcul de la couleur
 *
 * @author bello marais
 */
abstract class Calculus {

    private final int iteration;
    private final double limite;
    private final Function<Complex, Complex> f;
    private boolean run;

    /**
     * Constructeur
     *
     * @param iteration le nombre d'itérations
     * @param limite    la limite de la fonction
     * @param f         la fonction de calcul de C -> C
     */
    Calculus(int iteration, double limite, Function<Complex, Complex> f) {
        this.iteration = iteration;
        this.limite = limite;
        this.f = f;
        this.run = false;
    }

    /**
     * Convertisseur de couleurs de int -> javafx.Color
     *
     * @param value nombre d'itérations
     * @return une javafx.Color pour le rendu
     */
    static Color intToColor(int value) {
        return Color.rgb((value * 10) % 255, (value * 30) % 255, (value * 50) % 255);
    }

    /**
     * Getter de l'itération
     */
    final int getIteration() {
        return this.iteration;
    }

    /**
     * Getter pour la limite
     */
    final double getLimite() {
        return this.limite;
    }

    /**
     * Getter pour le run
     */
    final boolean getRunStatut() {
        return this.run;
    }

    /**
     * Getter de la fonction de calcul
     */
    final Function<Complex, Complex> getFunction() {
        return this.f;
    }

    /**
     * Indique le lancement du calcul
     */
    final void start() {
        this.run = true;
    }

    /**
     * Stop le calcul
     */
    final void stop() {
        this.run = false;
    }

    /**
     * Méthode abstraite pour calculer la limite d'une valeur
     *
     * @param x0 le pixel pour lequel on calcule le nombre d'itération
     * @return la couleur à associer à ce pixel
     */
    abstract Color getColorFrom(Complex x0);

    /**
     * Méthode abstraite pour calculer la limite d'une valeur
     * de façon infinie
     *
     * @param x0 le pixel pour lequel on calcule le nombre d'itération
     * @return la couleur à associer à ce pixel
     */
    abstract Color getInfiniteColorFrom(Complex x0);

}
