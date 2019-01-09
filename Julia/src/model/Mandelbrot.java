package model;

import javafx.scene.paint.Color;

/**
 * Implémentation d'une unité de calcul pour les ensembles de Mandelbrot
 *
 * @author bello marais
 * @see Calculus
 */
final class Mandelbrot extends Calculus {

    /**
     * Constructeur
     *
     * @param iteration le nombre d"itération maximum
     */
    Mandelbrot(int iteration) {
        super(iteration, 2.0, (xn) -> xn.times(xn));
    }

    @Override
    Color getColorFrom(Complex x0) {
        Complex xn = new Complex(0, 0);
        int i = 0;
        while (i < getIteration() && xn.module() < getLimite() && getRunStatut()) {
            xn = getFunction().apply(xn).plus(x0);
            i++;
        }
        return intToColor(i);
    }

    @Override
    Color getInfiniteColorFrom(Complex x0) {
        return getColorFrom(x0);
    }

}