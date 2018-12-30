package parser;

/**
 * Enumération de l'ensemble des symboles utiles au Parser
 * @author DURAND - MARAIS
 */
public enum Sym{
    EOF, POINTVIRGULE, BEGIN, END, LPAR, RPAR,
    COULEUR, INT, CONST, VAR, BOOLEAN, STRING, IDENT,
    OPERATEUR, COMPARATOR, EQ, OR, AND, PLUS, MINUS, TIMES, DIV,
    IF, THEN, ELSE, ASSIGNATION, WHILE, DO,
    DRAWCIRCLE, FILLCIRCLE, FILLRECT, DRAWRECT,
    PROC;
}
