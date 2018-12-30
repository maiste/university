package parser.token;

import parser.Sym;

/**
 * Classe de gestion des tokens de boolean
 * @author DURAND-MARAIS
 */
public class BooleanToken extends Token {

    private boolean value;

    
    
    /*****************
     * Constructeur *
     ****************/

    /** 
     * Constructeur par défaut
     * @param  sym    Symbole du Token
     * @param  line   ligne où se situe l'élément
     * @param  column colonne où se situe l'élément
     * @param  value  correspondant au string d'un boolean
     */
    public BooleanToken (Sym sym, int line, int column, String value) {
        super(sym, line, column);
        this.value = value.matches("[Tt]rue");
    }



    /**********
     * Getter *
     **********/

    /** 
     * Renvoie la valeur du token
     * @return String correspondant à la couleur du Token
     */
    public boolean getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return super.toString() + ", Value : " + this.value;
            
    }
        
}
