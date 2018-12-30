package ast;

import java.awt.*;
import java.lang.Exception;

import exception.ParserException;
import expression.Expression;

/**
 * Classe des AST permettant de déclarer une variable/constante
 * @author DURAND-MARAIS
 */
public class Assign extends AST {
    boolean isConstante;
    String nom;
    Expression exp1;

    /**
     * Constructeur
     * @param line la ligne du token
     * @param column la colonne du token
     * @param isConstante si c'est une constante
     * @param nom le nom de la variable
     */
    public Assign(int line, int column, boolean isConstante, String nom, Expression exp1){
        super(line, column);
        this.isConstante = isConstante;
        this.nom = nom;
        this.exp1 = exp1;
    }

    @Override   
    public void verifyAll(ValueEnv env)throws Exception{
        exp1.setType(env);
        exp1.verifyType(env);
        env.put(nom,exp1, isConstante);
    }

    @Override
    public void exec(Graphics2D g2d,ValueEnv val) throws Exception {
        if(debugMode()) { debug(val); }
        val.put(nom,exp1, isConstante); // On ajoute dans l'environnement la valeur
    }

    /** Debug */
    public void debug(ValueEnv val) throws Exception {
        System.out.println("Assignation => Nom: " + nom );        
        System.out.println("\texp =>" + exp1);
    }
	
}
