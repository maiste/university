package parser;

import exception.ParserException;
import lexer.Lexer;
import parser.token.ColorToken;
import parser.token.IntToken;
import parser.token.StringToken;
import parser.token.BooleanToken;
import parser.token.Token;

import java.awt.Color;

/**
 * Classe de lecture de fichier
 * @author DURAND-MARAIS
 */
public class LookAhead1{

    private Token current;
    private Lexer lex;
 
    
    /****************
     * Constructeur *
     ***************/

    /**
     * Constructeur par défaut
     * @param  l         lexer du fichier qu'on lit
     * @throws Exception exception s'il n'existe pas d'élément dans le fichier lu
     */
    public LookAhead1(Lexer l) throws Exception {
        this.lex = l;
        this.current = lex.yylex();
    }



    
    /**********
     * Reader *
     **********/

    /**
     * Vérifie le token
     * @param sym le symbole à tester
     * @return true si le symbole correspond
     */
    public boolean check (Sym sym) {
        return current.symbol() == sym;
    }

    /**
     * Consomme le token courant
     * @param sym le symbole à consommer
     */
    public void eat (Sym sym) throws Exception {
        if (!check(sym)) {
            throw new ParserException("Erreur de grammaire : symbole voulu <" + sym + ">, symbole actuel <" + current.symbol()+">", current.line(), current.column());
        }
        if (debugMode()) { System.out.println(current); }
        
        this.current = lex.yylex();
    }


    
    /***********
     * Getters *
     ***********/

    /**
     * Renvoie le int du token
     * @return int du Token
     * @throws Exception     le Token courant n'est pas en format d'entier
     */
    public int getIntValue() throws Exception {
        if (current instanceof IntToken){
            return ((IntToken)current).getValue();
        } else {
            throw new ParserException("\n Try to access to an int value", current.line(), current.column());
        }
    }

    /**
     * Renvoie la color du tokne
     * @return couleur du Token
     * @throws Exception     le Token courant n'est pas sous format de Color
     */
    public Color getColorValue() throws Exception {
        if (current instanceof ColorToken) {
            return ((ColorToken)current).getValue();
        } else {
            throw new ParserException("\n Try to access to a color value", current.line(), current.column());
        }
    }

    /**
     * Renvoie la chaine de caractères du token
     * @return String du Token
     * @throws Exception     le Token courant n'est pas sous format d'un String
     */
    public String getStringValue() throws Exception {
        if (current instanceof StringToken) {
            return ((StringToken)current).getValue();
        } else {
            throw new ParserException("\n Try to access to a string value", current.line(), current.column());
        }
    }

    /**
     * Renvoie la valeur boolean du token
     * @return Boolean du Token
     * @throws Exception     le Token courant n'est pas sous format d'un boolean
     */
    public boolean getBooleanValue() throws Exception {
        if (current instanceof BooleanToken) {
            return ((BooleanToken)current).getValue();
        }
        throw new ParserException("\n Try to access to a boolean value", current.line(), current.column());
    }
    
    /**
     * Retourne la ligne du token
     */
    public int line(){ return current.line();}
    
    /**
     * Retourne la colonne du token
     */
    public int column(){return current.column();}

    /** Retourne le symbole du Token courant */
    public Sym getSymbol(){
        return current.symbol();
    }


    /** Debug */
    public boolean debugMode() {
        return false; // True pour afficher les tokens
    }
    
}
