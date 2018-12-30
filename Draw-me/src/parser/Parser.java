package parser;

import ast.*;
import exception.ParserException;
import expression.And;
import expression.Or;
import expression.Bool;
import expression.Div;
import expression.Diff;
import expression.Int;
import expression.Sum;
import expression.Prod;
import expression.ComparatorOrdre;
import expression.Comparator; 
import expression.Expression;
import expression.Identificateur;
import expression.Expression;


import java.io.IOException;
import java.lang.Exception;
import java.awt.Color;
import java.util.LinkedList;


/**
 * Gammaire étendue :
 * 
 * programme → blocInstruction
 * blocInstruction → Instruction ; blocInstruction | 𝜀
 * instruction → Begin blocInstruction End 
 * 			   | DrawCircle ( expr , expr , expr , couleur )
 * 			   | FillCircle ( expr , expr , expr , couleur)
 * 			   | DrawRect ( expr , expr , expr , expr , couleur )
 * 			   | FillRect ( expr , expr , expr , expr , couleur )
 * 			   | Const identificateur = expr 
 * 			   | Var identificateur = expr
 * 			   | identificateur identSuite
 *             | Proc identificateur (liste) instruction
 *             | If expr Then instruction Autre
 *             | While condition Do instruction
 * identSuite -> (args) | = expression
 * Autre → Else instruction
 * 		 | 𝜀
 * liste -> identificateur, liste 
         | liste 
         | 𝜀
 * args -> expr, args 
        | expr 
        | 𝜀
 * expr → Nombre | identificateur | Boolean | ( expr exprSuite)
 * exprSuite -> operateur expr
 * Bool -> [Tt]rue | [Ff]alse
 * operateur → Op | ordre | eq | && | ||
 * op → + | - | / | *
 * ordre → > | < | <= | >= |
 * eq -> == | !=
 */



/**
 * Classe qui se charge de parser les fichiers
 * @author DURAND-MARAIS
 */
 
public class Parser{

    protected LookAhead1 reader;
    
    /****************
     * Constructeur *
     ****************/

    /**
     * Constructeur par défaut
     * @param  r  LookAhead1 pour voir un élément après pour une grammaire LL(1)
     */
    public Parser(LookAhead1 r) throws IOException {
        reader = r;
    }

    /** Axiome 
     * programme -> suite_instruction
     */
    public AST progNonTerm() throws Exception {
        
        // Création de l'ast.AST d'origine
        AST tmp =  suite_instruction(new AST(reader.line(),reader.column()));
        reader.eat(Sym.EOF);
        return tmp;
    }

    /**
     * Instruction 
     * @return l'AST représentant l'instruction
     */
    public AST instruction() throws Exception {
        int line = reader.line();
        int column = reader.column();

        if (reader.check(Sym.BEGIN))
            {
                /* instruction -> Begin suite_instruction End*/
                reader.eat(Sym.BEGIN);
                AST tmp = new AST(line,column);
                tmp = this.suite_instruction(tmp);
                reader.eat(Sym.END);
                return tmp; 
            }
        else if (reader.check(Sym.DRAWCIRCLE))
            {
                /* instruction -> DrawCircle ( exp, exp, couleur) */
                reader.eat(Sym.DRAWCIRCLE);
                reader.eat(Sym.LPAR);
                Expression x = this.non_term_exp();
                Expression y = this.non_term_exp();
                Expression z = this.non_term_exp();
                Color col = reader.getColorValue();
                reader.eat(Sym.COULEUR);
                reader.eat(Sym.RPAR);
                return new DrawCircle(line,column,x,y,z,col);
            }
        else if (reader.check(Sym.DRAWRECT))
            {
                /* instruction -> DrawRect(exp,exp,exp,couleur) */
                reader.eat(Sym.DRAWRECT);
                reader.eat(Sym.LPAR);
                Expression x = this.non_term_exp();
                Expression y = this.non_term_exp();
                Expression w = this.non_term_exp();
                Expression h = this.non_term_exp();
                Color col = reader.getColorValue();
                reader.eat(Sym.COULEUR);
                reader.eat(Sym.RPAR);
                return new DrawRect(line,column,x,y,w,h,col);
            }
        else if (reader.check(Sym.FILLCIRCLE))
            {
                /* instruction -> FillCircle(exp,exp,exp)*/
                reader.eat(Sym.FILLCIRCLE);
                reader.eat(Sym.LPAR);
                Expression x = this.non_term_exp();
                Expression y = this.non_term_exp();
                Expression z = this.non_term_exp();
                Color col = reader.getColorValue();
                reader.eat(Sym.COULEUR);
                reader.eat(Sym.RPAR);
                return new FillCircle(line, column, x, y, z, col);
            }
        else if (reader.check(Sym.FILLRECT))
            {
                /* instruction -> FillRect (exp,exp,exp,exp, couleur) */
                reader.eat(Sym.FILLRECT);
                reader.eat(Sym.LPAR);
                Expression x = this.non_term_exp();
                Expression y = this.non_term_exp();
                Expression w = this.non_term_exp();
                Expression h = this.non_term_exp();
                Color col = reader.getColorValue();
                reader.eat(Sym.COULEUR);
                reader.eat(Sym.RPAR);
                return new FillRect(line, column ,x,y,w,h,col);
        }
        else if (reader.check(Sym.CONST))
            {
                /* instruction -> Const identificateur = exp */
                reader.eat(Sym.CONST);
                String name = reader.getStringValue();
                reader.eat(Sym.IDENT);
                reader.eat(Sym.ASSIGNATION);
                Expression exp = this.non_term_exp();
                return new Assign(line,column,true,name,exp);
            }
        else if (reader.check(Sym.VAR))
            {
                /* instruction -> Var identificateur = exp */
                reader.eat(Sym.VAR);
                String name = reader.getStringValue();
                reader.eat(Sym.IDENT);
                reader.eat(Sym.ASSIGNATION);
                Expression exp = this.non_term_exp();
                return new Assign(line, column, false, name, exp);
            }
        else if(reader.check(Sym.IDENT))
            {
                /* instruction -> identificateur indenSuite */
                String name = reader.getStringValue();
                reader.eat(Sym.IDENT);
               
                if (reader.check(Sym.ASSIGNATION))
                    {
                         /* identSuite-> = exp */
                        reader.eat(Sym.ASSIGNATION);
                         Expression exp = this.non_term_exp();
                        return new Change(line, column, name, exp);
                    }
                else 
                    {
                        /* identSuite -> (args) */
                        reader.eat(Sym.LPAR);
                        LinkedList<Expression> args = this.args();
                        reader.eat(Sym.RPAR);
                        return new CallProc(line,column, name, args);
                    }    

            }
        else if (reader.check(Sym.IF))
            {
                /* If expr Then instruction Autre */
                reader.eat(Sym.IF);
                Expression exp = this.non_term_exp();
                reader.eat(Sym.THEN);
                AST ifAST = this.instruction();
                AST elseAST = new AST(line,column);
                if (reader.check(Sym.ELSE))
                    {
                        /* Autre -> Else instruction */
                        reader.eat(Sym.ELSE);
                        elseAST = instruction();
                    }
                return new Condition(line,column,exp, ifAST, elseAST);
            }
        else if (reader.check(Sym.WHILE)) 
            {
            /* instruction -> while exp do inst */
            reader.eat(Sym.WHILE);
            Expression exp = this.non_term_exp();
            reader.eat(Sym.DO);
            AST inst = this.instruction();
            return new While(line,column,exp,inst);
            }  
        else if (reader.check(Sym.PROC)) 
            {
                /* instruction -> Proc identificateur (liste) instruction */
                reader.eat(Sym.PROC);
                String name = reader.getStringValue();
                reader.eat(Sym.IDENT);
                reader.eat(Sym.LPAR);
                LinkedList<String> args = this.liste();
                reader.eat(Sym.RPAR);
                AST content = this.instruction();
                return new Proc(line, column, name, content, args);
            }        
        else
            {
                /* Erreur de parsing d'instructions */
                throw new ParserException("Motif non reconnu", reader.line(),reader.column());
            }
    }

    /**
     * Suite d'instruction de la grammaire 
     * @param current l'AST représentant la file d'exécution
     * @return l'AST courant
     */
    public AST suite_instruction (AST current) throws Exception {
        if(reader.check(Sym.END) || reader.check(Sym.EOF)){
            /* suite_instruction -> 𝜀 */
            return current;
        } else {
            /* suite_instruction -> instruction ; suite_instruction */
            current.addNext(this.instruction());
            reader.eat(Sym.POINTVIRGULE);
            return this.suite_instruction(current);
        }
    }


    /**
     * Partie prefixe des expressions de la grammaire 
     * @return l'expression de la grammaire 
     * @throws ParserException quand on ne peut pas construire
     */
    public Expression non_term_exp() throws Exception {
        int line = reader.line();
        int column = reader.column();

         
        if (reader.check(Sym.INT))
            {
                /* exp -> nombre */
                int value = reader.getIntValue();
                reader.eat(Sym.INT);
                return new Int(line,column,value);
            }
        else if (reader.check(Sym.IDENT))
            {
                /* exp -> identificateur */
                String ident = reader.getStringValue();
                reader.eat(Sym.IDENT);
                return new Identificateur(line, column,ident);
            }
        else if (reader.check(Sym.BOOLEAN)){
             /* exp -> boolean */
            boolean val = reader.getBooleanValue();
            reader.eat(Sym.BOOLEAN);
            return new Bool(line,column,val);
        }
        else if (reader.check(Sym.LPAR))
            {
                /* exp -> ( exp expSuit ) */
                reader.eat(Sym.LPAR);
                Expression left = non_term_exp();
                Expression res = this.non_term_expSuite(left);
                reader.eat(Sym.RPAR);
                return res;
            }
        throw new ParserException("Le symbole n'est pas reconnu !", line,column);
    }

    /**
     * Suite d'une expression
     * @param  beg Première partie de l'expression
     * @return Expression contruite
     * @throws ParserException On ne peut pas construire l'expression
     */
    public Expression non_term_expSuite(Expression beg) throws Exception {
        int line = reader.line();
        int column = reader.column();

        if (reader.check(Sym.PLUS))
            {
                /* exprSuite -> + expr */
                reader.eat(Sym.PLUS);
                return new Sum(line,column,beg,this.non_term_exp());
            }
        else if (reader.check(Sym.MINUS))
            {
                /* exprSuite -> - expr */
                reader.eat(Sym.MINUS);
                return new Diff(line,column,beg,this.non_term_exp());
            }
        else if (reader.check(Sym.TIMES))
            {
                /* exprSuite -> * expr */ 
                reader.eat(Sym.TIMES);
                return new Prod(line,column,beg,this.non_term_exp());
            }
        else if (reader.check(Sym.DIV))
            {
                /* exprSuite -> / expr */
                reader.eat(Sym.DIV);
                return new Div(line,column,beg,this.non_term_exp());
            }
        else if (reader.check(Sym.COMPARATOR))
            {
                /* exprSuite -> < | > | <= | >= expr */
                String symbol = reader.getStringValue();
                reader.eat(Sym.COMPARATOR);
                return new ComparatorOrdre(line,column,beg,this.non_term_exp(),symbol);
            }
        else if (reader.check(Sym.EQ))
            {
                /* exprSuite -> == | != expr */
                String symbol = reader.getStringValue();
                reader.eat(Sym.EQ);
                return new Comparator(line,column,beg,this.non_term_exp(),symbol);
            }
        else if (reader.check(Sym.OR))
            {
                /* exprSuite -> || expr */
                reader.eat(Sym.OR);
                return new Or(line,column,beg,this.non_term_exp());
            }
        else if (reader.check(Sym.AND))
            {
                /* exprSuite -> && expr */
                reader.eat(Sym.AND);
                return new And(line,column,beg,this.non_term_exp());
            }

        /* Erreur de lecture */    
        throw new ParserException("Erreur de symbole, on se retrouve avec "+reader.getSymbol(),line,column);
    }


    /**
     * Gestion de la liste d'arguments d'une fonction 
     * @return la LinkedList d'argument
     * @throws ParserException quand on ne peut pas lire le symbole
     */
    public LinkedList<String> liste() throws Exception {
        if (reader.check(Sym.RPAR))
            {
                /* liste -> 𝜀 */
                return new LinkedList<String>();
            }
        /* liste -> ident, liste | liste */    
        String val = reader.getStringValue();
        reader.eat(Sym.IDENT);
        LinkedList<String> param = liste();
        param.push(val);
        return param;
    }

    /**
     * Gestion de la liste du passage de paramètres
     * @return la liste de paramètres
     * @throws ParserException quand on ne peut pas lire 
     */
    public LinkedList<Expression> args() throws Exception {
        if (reader.check(Sym.RPAR))
            {
                /* args -> 𝜀 */
                return new LinkedList<Expression>();
            } 
        Expression exp = this.non_term_exp();
        /* args -> exp, args | exp */   
        LinkedList<Expression> res = args();   
        res.push(exp);
        return res;        
    }
}
