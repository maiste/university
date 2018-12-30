package expression;

import ast.Type;
import ast.ValueEnv;
import exception.ParserException;

import java.lang.Exception;

/**
 * Classe des Expressions
 * @author DURAND-MARAIS
 */
public abstract class Expression {
    protected int line;
    protected int column;
    protected Type type = Type.VOID;

    /**
     * On construit une expression avec la ligne et la colonne de l'expression dans le fichier
     * @param  line   ligne de l'expression dans le fichier
     * @param  column colonne de l'expression dans le fichier
     * @param  type type de l'expression
     */
    public Expression(int line, int column, Type type){
        this.line = line;
        this.column = column;
        this.type = type;
    }	
	
    /**
     * On récupère le Type de l'expression
     * @return ast.Type de l'expression
     */
    public Type getType(){
        return this.type;
    }

    /**
     * On récupère l'expression correspondant à l'expression, 
     * c'est à dire que si on a un identificateur, on le remplace par son
     * expression dans l'ensemble des valeurs 'env'
     * @param  env       ensemble de valeurs
     * @return           expression de this
     * @throws Exception Il y a un soucis dans la récupération de l'expression
     */
    public abstract Expression getExpression(ValueEnv env) throws Exception;

    /**
     * On change le type de l'expression
     */
    public abstract void setType(ValueEnv env) throws Exception;

    /**
     * On vérifie le type des éléments que doit récupérer l'expression
     */
    public abstract void verifyType(ValueEnv env) throws Exception;

    /**
     * On évalue l'expression pour récupérer un int
     * @param  env       environnement de valeur
     * @return           evalution de l'expression selon un int
     * @throws Exception on ne peut évaluer en int
     */
    public int evalInt(ValueEnv env) throws Exception{
        throw new Exception();
    }

    /**
     * On évalue l'expression pour récupérer un boolean
     * @param  env       environnement de valeur
     * @return           evalution de l'expression selon un boolean
     * @throws Exception on ne peut évaluer en boolean
     */
    public boolean evalBool(ValueEnv env) throws Exception{
        throw new Exception();
    }

    /**
     * On récupère la ligne correspondant à l'expression
     * @return int de la ligne
     */
    public int getLine() {
        return line;
    }

    /**
     * On récupère la colonne correspondant à l'expression
     * @return int de la colonne
     */
    public int getColumn() {
        return column;
    }

    /** Pour le mode debug */
    public boolean debugMode(){
        return true; // A changer pour quitter le mode débug
    }

    public String toString(){
        return "Expression : ligne="+line+", colonne="+column;
    }
}