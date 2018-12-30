package parser.token;

import parser.Sym;

/**
 * Classe de gestion des tokens de type integer
 * @author DURAND-MARAIS
 */
public class IntToken extends Token {

    private int value;


    
    /****************
     * Constructeur *
     ***************/

    /** 
     * Constructeur par défaut
     * @param  sym    Symbole du Token
     * @param  line   ligne où se situe l'élément
     * @param  column colonne où se situe l'élément
     * @param  value  valeur du Token correspondant à un int
     */
    public IntToken(Sym sym, int line, int column, String value) {
        super(sym,line,column);
        this.value = Integer.parseInt(value);
    }



    
    /**********
     * Getter *
     *********/

    /** 
     * Renvoie la valeur du token
     * @return String correspondant au Token
     */
    public int getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return super.toString() + ", Value : " + this.value;
    }
    
}
