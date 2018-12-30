package parser.token;

import parser.Sym;

import java.awt.Color;


/**
 * Classe de gestion des tokens de couleur
 * @author DURAND-MARAIS
 */
public class ColorToken extends Token {

    private Color value;

    
    
    /****************
     * Constructeur *
     ****************/

    /** 
     * Constructeur par défaut
     * @param  sym    Symbole du Token
     * @param  line   ligne où se situe l'élément
     * @param  column colonne où se situe l'élément
     * @param  hex    format en hexadécimal d'une color
     */
    public ColorToken(Sym sym, int line, int column, String hex){
        super(sym, line, column);
        this.value = Color.decode(hex);
    }



    /**********
     * Getter *
     **********/

    /** 
     * Renvoie la valeur du token
     * @return String correspondant à la couleur du Token
     */
    public Color getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return super.toString() + ", Value : " + this.value;
    }

}
