package model;

import javafx.scene.paint.Color;

import java.util.function.Function;

/**
 * Implémentation d'une unité de calcul pour les ensembles de Julia
 *
 * @author bello marais
 * @see Calculus
 */
final class Julia extends Calculus {

    /**
     * Constructeur
     *
     * @param f         la function à utiliser
     * @param iteration le nombre d"itération maximum
     * @param limite    la limite de la fonction f
     */
    Julia(Function<Complex, Complex> f, int iteration, double limite) {
        super(iteration, limite, f);
    }

    @Override
    Color getColorFrom(Complex x0) {
        Complex xn = new Complex(x0);
        int i = 0;
        while (i < getIteration() && xn.module() < getLimite() && getRunStatut()) {
            xn = getFunction().apply(xn);
            i++;
        }
        return intToColor(i);
    }

    /**
     * Calcule de la Julia sous forme infinie
     *
     * @param x0 le complexe de pixel
     * @return une Color
     */
    Color getInfiniteColorFrom(Complex x0) {
        Complex xn = new Complex(x0);
        Complex xn_1 = getFunction().apply(xn);
        int i = 0;
        while (i < Integer.MAX_VALUE && getRunStatut() && xn != xn_1 &&
                Math.abs(xn.minus(xn_1).module()) > 1.0) {
            xn = new Complex(xn_1);
            xn_1 = getFunction().apply(xn);
            i++;
        }
        return intToColor(i);
    }

}
