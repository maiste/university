package model;

import java.util.function.Function;

/**
 * Classe statique de gestion de la création des polynômes
 *
 * @author bello marais
 */
public final class PolynomialFactory {

    /**
     * Donne un nouveau terme
     *
     * @param coeff le coefficient multiplicateur
     * @param n     la puissance
     * @return la function correspondant au n-éme termes
     */
    private static Function<Complex, Complex> giveTerm(double coeff, int n) {
        return x -> new Complex(coeff, 0).times(x.pow(n));
    }

    /**
     * Fabrique les termes du polynome
     *
     * @param coeffs    l'ensemble des coefficients devant les termes
     * @param constante la valeur constante
     * @param i         la position de la fabrique
     * @return la fonction polynome associée
     */
    private static Function<Complex, Complex> makeFunctionAux(double[] coeffs, Complex constante, int i) {
        if (i <= 0)
            return x -> constante;
        return x -> makeFunctionAux(coeffs, constante, i - 1).apply(x).plus(giveTerm(coeffs[i], i).apply(x));
    }

    /**
     * Fabrique les termes du polynome
     *
     * @param coeffs    l'ensemble des coefficients devant les termes
     *                  Ex : coeff = {0.0,0.0,1.0} donne le polynome x^2
     * @param constante la valeur constante
     * @return la fonction polynome associée
     */
    public static Function<Complex, Complex> makeFunction(double[] coeffs, Complex constante) {
        return makeFunctionAux(coeffs, constante, coeffs.length - 1);
    }

}
