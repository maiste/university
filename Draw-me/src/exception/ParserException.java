package exception;

import java.io.*;

/**
 * Classe correspondant à une Exception personnalisée
 * @author DURAND-MARAIS
 */

public class ParserException extends Exception{
	
	/**
	 * Constructeur
	 * @param errorMessage message à afficher
	 * @param line ligne de l'erreur
	 * @param column colonne de l'erreur
	 */
	public ParserException(String errorMessage, int line, int column){
		super(errorMessage + "\n" 
			+ "L'erreur a été repérée à la ligne " + line 
			+ " et à la colonne " + column + ".");
	}	
}
