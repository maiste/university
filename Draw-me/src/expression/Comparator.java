package expression;

import ast.Type;
import ast.ValueEnv;
import exception.ParserException;

import java.lang.Exception;

/**
 * Classe des comparateurs
 * @author DURAND-MARAIS
 */
public class Comparator extends Expression{
    private Expression exp1;
    private Expression exp2;
    private String comparator;
    static final String[] possibleComparator = {"!=","=="};

    /**
     * On construit un comparateur d'équivalence
     * @param  line   ligne de l'expression dans le fichier
     * @param  column colonne de l'expression dans le fichier
     * @param exp1 Première expression du comparateur
     * @param exp2 Deuxième expression du comparateur
     * @param comparator string correspondant à l'opérateur de comparaison
     */
    public Comparator(int line, int column, Expression exp1, Expression exp2, String comparator){
        super(line,column,Type.BOOLEAN);
        this.exp1 = exp1;
        this.exp2 = exp2;
        this.comparator = comparator;
    }   

    /** On renvoie la même expression en ayant comme attribut le 'getExpression' des attributs de 'this' */
    public Expression getExpression(ValueEnv env) throws Exception{
        return new Comparator(line,column,exp1.getExpression(env), exp2.getExpression(env), comparator);
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

        if( ! ( (exp1.getType() == Type.INT && exp2.getType() == Type.INT) 
            || (exp1.getType() == Type.BOOLEAN && exp2.getType() == Type.BOOLEAN) )){
            throw new ParserException("Il y a une problème de typage dans les arguments de l'opérateur "+comparator,line,column);
        }
    }

    /** On évalue l'expression selon le comparateur '==' ou '!=', on s'attend à renvoyer un boolean */
    public boolean evalBool(ValueEnv env) throws Exception{
        boolean isInt = exp1.getType() != Type.INT;
        switch(comparator){
            case "==":
                if (isInt) {
                    return exp1.evalBool(env) == exp2.evalBool(env);
                }
                else{
                    return exp1.evalInt(env) == exp2.evalInt(env);
                }
            case "!=":
                if (isInt) {
                    return exp1.evalBool(env) != exp2.evalBool(env);
                }
                else{
                    return exp1.evalInt(env) != exp2.evalInt(env);
                }
            default: throw new ParserException("L'opérateur "+comparator+" n'existe pas, ou n'est pas géré",line,column);
        }
    }

    /** Pour le mode debug */
    public boolean debugMode(){
        return true; // A changer pour quitter le mode débug
    }
}