package expression;

import ast.Type;
import ast.ValueEnv;
import exception.ParserException;

/**
 * Classe pour l'identificateur
 * @author DURAND-MARAIS
 */
public class Identificateur extends Expression{
    private String nom;

    /** on construit une Expression pour un identificateur (Var et Const) */
    public Identificateur(int line, int column, String value){
        super(line,column, Type.VOID);
        this.nom = value;
    }

    /** On récupère l'expression correspondante à l'identificateur dans l'environnement de valeur.
     *  On renvoie une erreur si l'identificateur n'existe pas.
     */
    public Expression getExpression(ValueEnv env) throws Exception{
        return env.get(nom,line,column).getExpression(env);
    }

    /** On change le type de l'expression en fonction de son expression dans l'environnement de valeur 'env' */
    public void setType(ValueEnv env) throws Exception{
        super.type = env.getType(nom, line, column);
    }

    /** vérifie la portée de la variable */
    public void verifyType(ValueEnv env) throws Exception{
        if(! env.contains(nom)){
            //System.out.println("Here");
            throw new ParserException("L'identificateur "+nom+" n'existe pas, il n'a pas été déclaré",line,column);
        }
    }

    /** on évalue l'identificateur en fonction le l'environnement de valeur, on s'attend à renvoyer un int */
    public int evalInt(ValueEnv env) throws Exception{
        return env.get(nom, line, column).evalInt(env);
    }

    /** on évalue l'identificateur en fonction le l'environnement de valeur, on s'attend à renvoyer un boolean */
    public boolean evalBool(ValueEnv env) throws Exception{
        return env.get(nom, line, column).evalBool(env);
    }

    /** Debug */
    public void debug(){
        System.out.println("expression.Identificateur => nom: " + nom);
    }
}
