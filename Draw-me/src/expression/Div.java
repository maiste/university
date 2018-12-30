package expression;

import ast.Type;
import ast.ValueEnv;
import exception.ParserException;

import java.lang.Exception;

/**
 * Classe pour la division : Div
 * @author DURAND-MARAIS
 */
public class Div extends Expression {
    private Expression exp1;
    private Expression exp2;

    /**
     * On construit une division
     * @param  line   ligne de l'expression dans le fichier
     * @param  column colonne de l'expression dans le fichier
     * @param exp1 Première expression du comparateur
     * @param exp2 Deuxième expression du comparateur
     */
    public Div(int line, int column, Expression exp1, Expression exp2){
        super(line, column, Type.INT);
        this.exp1 = exp1;
        this.exp2 = exp2;
    } 

    /**
     * On crée une nouvelle expression 'Div' avec le getExpression des deux expressions en attribut
     */
    public Expression getExpression(ValueEnv env) throws Exception{
        return new Div(line,column,exp1.getExpression(env), exp2.getExpression(env));
    }  

    /**
     * On change le type de l'expression
     */
    public void setType(ValueEnv env) throws Exception{
        this.type = Type.INT;
    }

    /**
     * On vérifie le type des éléments que doit récupérer l'expression
     */
    public void verifyType(ValueEnv env) throws Exception{
        exp1.setType(env);
        exp2.setType(env);

        exp1.verifyType(env);
        exp2.verifyType(env);

        if(exp1.getType() != Type.INT || exp2.getType() != Type.INT){
            throw new Exception();
        }
    }

    /** On évalue la division entre les deux expressions en attribut, on s'attend à avoir un int */
    public int evalInt(ValueEnv env) throws Exception{
        return exp1.evalInt(env) / exp2.evalInt(env);
    }

    /** Pour le mode debug */
    public boolean debugMode(){
        return true; // A changer pour quitter le mode débug
    }
}