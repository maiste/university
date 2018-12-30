package ast;

import java.awt.*;
import java.lang.Exception;
import exception.ParserException;
import expression.Expression;
/**
 * Classe correspondant à la fonction fillOval de Graphics
 * @author DURAND-MARAIS
 */

public class FillCircle extends AST {
    Expression exp1;
    Expression exp2;
    Expression exp3;
    Color color;
	
    /**
     * on construit un AST correspondant à la fonction fillOval
     * @param  line   ligne de l'expression dans le fichier
     * @param  column colonne de l'expression dans le fichier
     * @param  exp1   premier argument de la fonction
     * @param  exp2   deuxième argument de la fonction
     * @param  exp3   troisème argument de la fonction
     * @param  color  couleur
     */
    public FillCircle(int line, int column, Expression exp1, Expression exp2, Expression exp3, Color color){
        super(line, column);
        this.exp1 = exp1;
        this.exp2 = exp2;
        this.exp3 = exp3;
        this.color = color;
    }

    @Override
    public void verifyAll(ValueEnv env) throws Exception{
        exp1.setType(env);
        exp2.setType(env);
        exp3.setType(env);

        exp1.verifyType(env);
        exp2.verifyType(env);
        exp3.verifyType(env);

        if(exp1.getType() != Type.INT || exp2.getType() != Type.INT || exp3.getType() != Type.INT) throw new ParserException("Il y a un problème.", line, column);
    }

    @Override
    public void exec(Graphics2D g2d, ValueEnv val) throws Exception{
        int x = exp1.evalInt(val);
        int y = exp2.evalInt(val);
        int r = exp3.evalInt(val); 
        g2d.setColor(color);
        g2d.fillOval(x-r, y-r, r*2, r*2); // décalage centre + diametre
        if(debugMode()){debug(x,y,r);} //debug
    }

    /** Debug */
    public void debug(int x, int y, int r) {
        System.out.println("FillCircle => x: " + x + " y: " +y+ " r: " +r );
    }
    
}
