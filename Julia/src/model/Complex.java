package model;

/**
 * Classe Complex
 *
 * @author marais bello
 * Permet de gérer les complexes
 * avec les opérations arithmétiques usuelles
 */
final class Complex {

    static final Complex ZERO = new Complex(0, 0);
    static final Complex UN = new Complex(1, 0);
    static final Complex I = new Complex(0, 1);
    private final double real;
    private final double img;

    /**
     * Constructeur
     *
     * @param real la partie réelle
     * @param img  la partie imaginaire
     */
    Complex(double real, double img) {
        this.real = real;
        this.img = img;
    }

    /**
     * Constructeur
     *
     * @param c le complexe à dupliquer
     */
    Complex(Complex c) {
        this(c.real, c.img);
    }

    /**
     * Getter partie réelle
     */
    double getReal() {
        return real;
    }

    /**
     * Getter partie imaginaire
     */
    double getImg() {
        return img;
    }

    /**
     * Addition de complexe
     *
     * @param c le complexe à ajouter à this
     * @return un nouveau complexe resultat
     */
    Complex plus(Complex c) {
        return new Complex(this.real + c.real, this.img + c.img);
    }

    /**
     * Soustraction de complexe
     *
     * @param c le complexe à soustraire à this
     * @return un nouveau complexe resultat
     */
    Complex minus(Complex c) {
        return new Complex(this.real - c.real, this.img - c.img);
    }


    /**
     * Multiplication de complexe
     *
     * @param c le complexe à multiplier par this
     * @return un nouveau complexe resultat
     */
    Complex times(Complex c) {
        return new Complex(this.real * c.real - this.img * c.img,
                this.real * c.img + this.img * c.real);
    }

    /**
     * Calcul d'un complexe à la puissance n sous forme d'exponentiation  binaire
     *
     * @param n sa puissance
     * @return this puissance pow
     */
    Complex pow(int n) {
        if (n == 0)
            return new Complex(0, 0);
        else if (n == 1)
            return this;
        else if (n % 2 == 0)
            return (this.times(this)).pow(n / 2);
        else
            return (this.times(this)).pow((n - 1) / 2).times(this);
    }

    /**
     * Calcule le module du complex this
     *
     * @return la valeur en double du module
     */
    double module() {
        return Math.sqrt(Math.pow(this.real, 2) + Math.pow(this.img, 2));
    }


    @Override
    public String toString() {
        return real + "+" + img + "i";
    }

}
