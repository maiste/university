package ast;

import exception.ParserException;
import expression.Expression;

import java.awt.*;
import java.lang.Exception;

/**
 * Classe de gestion de boucles
 * @author DURAND-MARAIS
 */
public class While extends AST {

    Expression condition;

  /**
   * Constructeur de l'AST de boucle 
   * @param line
   * @param column
   * @param condition
   * @param instruction
   */
  public While(int line, int column,Expression condition, AST instruction) {
    super(line,column);
    this.condition = condition;
    super.addNext(instruction);
  }
  	
    @Override
    public void verifyAll(ValueEnv env) throws Exception{
        condition.setType(env);
        condition.verifyType(env);
        Type type = condition.getType();
        if(type != Type.BOOLEAN && type != Type.INT) throw new ParserException("Il y a un problème de typage.",line,column);
        super.verifyAll(env);
    }

    @Override
    public void exec(Graphics2D g2d, ValueEnv val) throws Exception { // A revoir simplement prend en paramètre deux instructions !
        boolean cond = false;
        if(condition.getType() == Type.BOOLEAN){
            cond = condition.evalBool(val);
        }
        else{
            cond = condition.evalInt(val) != 0;
        }
        if(debugMode()) {debug(val,cond);} //debug

        if(cond){
          super.exec(g2d,val);
          this.exec(g2d,val);
        }
    }

    /** Debug */
    public void debug(ValueEnv val, boolean b) {
        System.out.println("Boucle => valeur: "+b);
    }

}