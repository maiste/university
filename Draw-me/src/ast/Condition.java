package ast;

import exception.ParserException;
import expression.Expression;

import java.awt.*;
import java.lang.Exception;

/**
 * Classe de gestion des conditions
 * @author DURAND-MARAIS
 */

public class Condition extends AST {
    Expression condition;
    AST inst1;
    AST inst2;

    /**
     * constructeur d'un AST correspondant à un If
     * @param  line      ligne de l'expression dans le fichier
     * @param  column    colonne de l'expression dans le fichier
     * @param  condition condition du If
     * @param  inst1 instruction faite si la condition est à true
     * @param  inst2 instruction faite si la condition est à false
     */
    public Condition(int line, int column, Expression condition, AST inst1, AST inst2){
        super(line, column);
        this.condition = condition;
        this.inst1 = inst1;
        this.inst2 = inst2;
    }
	
    @Override
    public void verifyAll(ValueEnv env) throws Exception{
        condition.setType(env);
        condition.verifyType(env);
        Type type = condition.getType();
        if(type != Type.BOOLEAN && type != Type.INT) throw new ParserException("Il y a un problème de typage.",line,column);
        env.add();
        inst1.verifyAll(env);
        env.pollLast();
        env.add();
        inst2.verifyAll(env);
        env.pollLast();
    }

    @Override
    public void exec(Graphics2D g2d, ValueEnv val) throws Exception { // A revoir simplement prend en paramètre deux instructions !
        val.add();
        boolean cond = false;
        if(condition.getType() == Type.BOOLEAN){
            cond = condition.evalBool(val);
        }
        else{
            cond = condition.evalInt(val) != 0;
        }
        if(debugMode()) {debug(val,cond);} //debug

        if(cond) { inst1.exec(g2d,val); }
        else { inst2.exec(g2d,val); }
        val.pollLast();
    }

    /** Debug */
    public void debug(ValueEnv val, boolean b) {
        System.out.println("Condition => valeur: "+b);
    }
}
