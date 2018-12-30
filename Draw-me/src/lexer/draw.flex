package lexer;

import parser.*;
import parser.Sym;
import parser.token.*;
import java.util.HashMap;

// Liste de symboles uniques
class Keys extends HashMap<String, Sym> {
	public Keys(){
		super();
		this.put("Begin",Sym.BEGIN);
		this.put("End",Sym.END);
		this.put("While",Sym.WHILE);
		this.put("Do",Sym.DO);
		this.put("If",Sym.IF);
		this.put("Else",Sym.ELSE);
		this.put("Then",Sym.THEN);
		this.put("DrawCircle",Sym.DRAWCIRCLE);
		this.put("FillCircle",Sym.FILLCIRCLE);
		this.put("DrawRect",Sym.DRAWRECT);
		this.put("FillRect",Sym.FILLRECT);
		this.put("Const",Sym.CONST);
		this.put("Var",Sym.VAR);
		this.put("Proc",Sym.PROC);
  }
}

// Gestion d'exceptions du lexer
class LexerException extends Exception{
	public LexerException(int line, int column, String caract){
  		super("Le caractere "+caract.replace("\\","\\\\")+" a la ligne "+line+" et a la colonne "+column+" n'est pas reconnu par la grammaire.");
	}
}

%%
%public
%class Lexer
%line
%column
%unicode
%type Token

%{
	private Keys keys = new Keys();

	private Token identKeyWord(int line, int column, String word) throws LexerException{
		if(keys.containsKey(word)){
			return new Token(keys.get(word), line, column);
		}
		else throw new LexerException(line,column,word);
	}

%}


%yylexthrow{
  //Exception qu’on va créer nous meme pour l’analyse lexicale
  Exception, LexerException
%yylexthrow}



//definition des differentes variables


commentaire = ("/*"[^]*"*/") | ("//"[^\n\r]*)
hex = [0-9A-F]
nombre = [0-9]+ 
couleur = "#"{hex}{hex}{hex}{hex}{hex}{hex}
ordre = ">" | "<" | "<=" | ">="  
equal = "==" | "!="
identificateur = [a-z][a-zA-Z_]*
keyWord = [A-Z][a-zA-Z]*
blanc = [\n\ \t\r]

%%
// Valeurs
{couleur}  		   			 { return new ColorToken(Sym.COULEUR,yyline + 1,yycolumn +1 ,yytext()) ;}
{nombre}  		   			 { return new IntToken(Sym.INT,yyline + 1,yycolumn +1 ,yytext()) ;}
"True"        		     { return new BooleanToken(Sym.BOOLEAN,yyline + 1,yycolumn +1 ,yytext()) ;}
"False"        		     { return new BooleanToken(Sym.BOOLEAN,yyline + 1,yycolumn +1 ,yytext()) ;}

// Opérations
"+"                    { return new Token(Sym.PLUS,yyline + 1,yycolumn +1 ) ;}
"-"                    { return new Token(Sym.MINUS,yyline + 1,yycolumn +1 ) ;}
"*"                    { return new Token(Sym.TIMES,yyline + 1,yycolumn +1 ) ;}
"/"                    { return new Token(Sym.PLUS,yyline + 1,yycolumn +1 ) ;}
"&&"                   { return new Token(Sym.AND,yyline + 1,yycolumn +1 ) ;}
"||"                   { return new Token(Sym.OR,yyline + 1,yycolumn +1 ) ;}
{ordre}         	     { return new StringToken(Sym.COMPARATOR,yyline + 1,yycolumn +1 , yytext()) ;}
{equal}         	     { return new StringToken(Sym.EQ,yyline + 1,yycolumn +1 , yytext()) ;}

// Mots-clés
{keyWord}		 	         { return this.identKeyWord(yyline, yycolumn, yytext()) ;}
"="             	     { return new Token(Sym.ASSIGNATION,yyline + 1,yycolumn +1 ) ;}
{identificateur}	     { return new StringToken(Sym.IDENT,yyline + 1,yycolumn +1 , yytext()) ;}

// Délimiteurs
"("             	     { return new Token(Sym.LPAR,yyline + 1,yycolumn +1 ) ;}
")"             	     { return new Token(Sym.RPAR,yyline + 1,yycolumn +1 ) ;}
";"                	   { return new Token(Sym.POINTVIRGULE,yyline + 1,yycolumn +1 ) ;}
<<EOF>>                { return new Token(Sym.EOF,yyline + 1,yycolumn +1 ) ;}

// Ignorés
{commentaire}          {}
{blanc}+|","  		     {}

// Erreurs
[^]                    { throw new LexerException(yyline + 1,yycolumn +1 , yytext()) ;}
