package parser.token;

import parser.Sym;

/**
 * Classe de gestion des tokens de chaine de caractères
 * @author DURAND-MARAIS
 */
public class StringToken extends Token {

    private String value;

    
    
    /*****************
     * Constructeur *
     ****************/

    /** 
     * Constructeur par défaut
     * @param  sym    Symbole du Token
     * @param  line   ligne où se situe l'élément
     * @param  column colonne où se situe l'élément
     * @param  value  valeur du Token
     */
    public StringToken (Sym sym, int line, int column, String value) {
        super(sym, line, column);
        this.value = value;
    }



    /**********
     * Getter *
     **********/

    /** 
     * Renvoie la valeur du token
     * @return String correspondant au Token
     */
    public String getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return super.toString() + ", Value : " + this.value;
            
    }
        
}
