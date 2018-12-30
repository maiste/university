package expression;

import ast.Type;
import ast.ValueEnv;
import exception.ParserException;

import java.lang.Exception;

/**
 * Classe pour l'opérateur || : Or
 * @author DURAND-MARAIS
 */
public class Or extends Expression{
    private Expression exp1;
    private Expression exp2;

    /**
     * On construit un Or
     * @param  line   ligne de l'expression dans le fichier
     * @param  column colonne de l'expression dans le fichier
     * @param exp1 Première expression du comparateur
     * @param exp2 Deuxième expression du comparateur
     */
    public Or(int line, int column, Expression exp1, Expression exp2){
        super(line, column, Type.BOOLEAN);
        this.exp1 = exp1;
        this.exp2 = exp2;
    }   

    /**
     * On crée une nouvelle expression 'Or' avec le getExpression des deux expressions en attribut
     */
    public Expression getExpression(ValueEnv env) throws Exception{
        return new Or(line,column,exp1.getExpression(env), exp2.getExpression(env));
    }

    /**
     * On change le type de l'expression
     */
    public void setType(ValueEnv env) throws Exception{
        this.type = Type.BOOLEAN;
    }

    /**
     * On vérifie le type des éléments que doit récupérer l'expression
     */
    public void verifyType(ValueEnv env) throws Exception{
        exp1.setType(env);
        exp2.setType(env);

        exp1.verifyType(env);
        exp2.verifyType(env);

        if(exp1.getType() != Type.BOOLEAN || exp2.getType() != Type.BOOLEAN){
            throw new ParserException("Il y a une problème de typage dans les arguments de l'opérateur Or (||)",line,column);
        }
    }

    /** on évalue le 'ou' entre deux expressions (ou '||' en java), on s'attend à un boolean */
    public boolean evalBool(ValueEnv env) throws Exception{
        return exp1.evalBool(env) || exp2.evalBool(env);
    }

    /** Pour le mode debug */
    public boolean debugMode(){
        return true; // A changer pour quitter le mode débug
    }
}