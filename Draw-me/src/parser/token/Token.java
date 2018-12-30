package parser.token;

import parser.Sym;

/**
 * Classe de génération de token
 * @author DURAND-MARAIS
 */
public class Token {

    private Sym sym;
    private int line;
    private int column;


    
    /****************
     * Constructeur *
     ****************/
    
    /** 
     * Constructeur par défaut
     * @param  sym    Symbole du Token
     * @param  line   ligne où se situe l'élément
     * @param  column colonne où se situe l'élément
     */
    public Token(Sym sym, int line, int column){
        this.sym = sym;
        this.line = line;
        this.column = column;
    }


    
    /***********
     * Getters *
     ***********/

    /**
     * Récupération du symbole
     * @return Symbole du Token
     */
    public Sym symbol() {
        return this.sym;
    }

    /**
     * Renvoie la line du Token
     * @return entier correspondant à la ligne
     */
    public int line() {
        return this.line;
    }

    /** 
     * Renvoie la colonne
     * @return entier correspondant à la colonne
     */
    public int column() {
        return this.column;
    }


    @Override
    public String toString() {
        return "Token => Symbole : " + this.sym;
    }
    
}
