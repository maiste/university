package ast;

import java.awt.*;
import java.util.LinkedList;
import java.lang.Exception;

/**
 * Classe de gestion des arbres de syntaxe abstraite
 * @author DURAND-MARAIS
 */
public class AST {

    protected LinkedList<AST> next;
    protected int line;
    protected int column;
       
    /****************
     * Constructeur *
     ****************/
    
    /**
     * Constructeur 
     * @param line ligne du token
     * @param column colonne du token
     */
    public AST(int line, int column){
    	this.line = line;
    	this.column = column;
    	next = new LinkedList<>();
    }

    /** 
     * on vérifie le type de chacun des AST suivants 
     * @param env l'ensemble des variables, constantes et fonctions 
     * @throws Exception erreur de l'exécution 
     */
    public void verifyAll(ValueEnv env) throws Exception{
      env.add();
    	for (AST suivant : next) {
            suivant.verifyAll(env);
    	}
      env.pollLast();
    }

    /** 
     * fonction d'exécution des AST suivants
     * @param g2d élément de gestion du graphique (paintComponent)
     * @param val registre de variables
     * @throws Exception erreur de l'exécution
     */
    public void exec(Graphics2D g2d,ValueEnv val) throws Exception {
        val.add();
        for (AST suivant : next) {
            suivant.exec(g2d,val);
        }
        val.pollLast(); // Enlève la dernière couche de  variables
    }

    /**
     * On ajoute un AST dans les suivants
     * @param suivant AST qu'on rajoute dans les suivants de this
     * @return true si l'opération d'ajout s'est bien passée, false sinon
     */
    public boolean addNext(AST suivant){
        next.add(suivant);
        return next.contains(suivant);
    }

    /** Permet d'activer ou non le mode debug */
    public boolean debugMode(){
        return false; // Pour la phase de débug !
    }

    /** Retourne la line */
    public int line() {
        return this.line;
    }

    /** Retourne la colonne */
    public int column() {
        return this.column;
    }
    
}
