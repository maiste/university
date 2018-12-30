package view.terminal;

import java.util.Scanner;

/**
 * ScanTerminal est une classe gerant les interactions avec l'utilisateur via la
 * console
 *
 * Restriction : ne pas utiliser d'autres objets Scanner en parallele de celui
 * de cette classe.
 */
public class ScanTerminal {
	private static Scanner scan = new Scanner(System.in);

	/*
	 * Cette classe ne contient que des méthodes statiques public, les autres
	 * méthodes seront privates. Elle possède néanmoins un attribut private statique
	 * de classe Scanner pour faire des requêtes à l'utilisateur.
	 */

	/**
	 * On affiche la question prise en parametre et on renvoie un String
	 * correspondant à la reponse de l'utilisateur.
	 * 
	 * @param question
	 *            L'enoncé de la question que pose le programme.
	 * @return Reponse de l'utilisateur sous forme de String à la question posée.
	 */
	public static String askString(String question) {
		String affichage = (question.length() > 0) ? "\n" : "";

		System.out.print(question + affichage);
		return (scan.nextLine());
	}

	/**
	 * On affiche la question prise en paramètre et on renvoie un boolean exprimant
	 * si la reponse appartient au pattern des reponses possibles.
	 * 
	 * @param question
	 *            L'enoncé de la question que pose le programme.
	 * @param pattern
	 *            Sous forme Regex, ce String contient les différentes reponses
	 *            attendues (reponse1|reponse2|etc)
	 * @return Vrai si la reponse de l'utilisateur appartient au pattern sinon
	 *         renvoie false.
	 */
	public static boolean askQuestion(String question, String pattern) {
		String affichage = (question.length() > 0) ? "\n" : "";

		System.out.println(question + affichage);
		pattern = pattern.toUpperCase();
		String word = scan.nextLine().toUpperCase();
		return (word.matches(pattern));
	}

	/**
	 * On affiche la question prise en paramètre et on renvoie un boolean exprimant
	 * si la reponse appartient au pattern des reponses possibles.
	 *
	 * @param question
	 *            L'enoncé de la question que pose le programme.
	 * @param error_message
	 * 			  message d'erreur si ce n'est pas la reponse attendue
	 * @param pattern
	 *            Sous forme Regex, ce String contient les différentes reponses
	 *            attendues (reponse1|reponse2|etc)
	 * @return renvoie la reponse de l'utilisateur si elle appartient au pattern.
	 */
	public static String askSpecificQuestion(String question,String error_message, String pattern) {
		String affichage = (question.length() > 0) ? "" : "\n";
		String word = null;
		String test = null;
		pattern = pattern.toUpperCase();
		do {
			System.out.println(question + affichage);
			if(word != null) System.out.println(error_message);
			word = scan.nextLine();
			test = word.toUpperCase();
		}
		while (! test.matches(pattern));
		return word;
	}

	/**
	 * On affiche la question prise en paramètre et on renvoie un boolean exprimant
	 * si la reponse appartient au pattern des reponses possibles.
	 *
	 * @param question
	 *            L'enoncé de la question que pose le programme.
	 * @param error_message
	 * 			  message d'erreur si ce n'est pas la reponse attendue
	 * @param pattern
	 *            Sous forme Regex, ce String contient les différentes reponses
	 *            attendues (reponse1|reponse2|etc)
	 * @param stop_regex
	 * 			  regex contenant les differents types d'arret du programme, on renvoie un null si on matche.
	 * @return renvoie la reponse de l'utilisateur si elle appartient au pattern.
	 */
	public static String askSpecificQuestionWithStop(String question,String error_message, String stop_regex, String pattern) {
		String questRegex = stop_regex+"|"+pattern;
		String res;
		res = askSpecificQuestion(question, error_message,questRegex);
		if (res.matches(stop_regex)) return null;
		else return res.toUpperCase();
	}

	/**
	 * On affiche la question prise en paramètre et on renvoie la reponse si elle
	 * appartient au pattern des reponses possibles.
	 * 
	 * @param question
	 *            L'enoncé de la question que pose le programme.
	 * @param pattern
	 *            Sous forme Regex, ce String contient les différentes reponses
	 *            attendues (reponse1|reponse2|etc)
	 * @return renvoie la reponse de l'utilisateur si elle est dans le pattern sinon
	 *         repose la question.
	 */
	public static String askQuestionAnswerKnown(String question, String pattern) {
		pattern = pattern.toUpperCase();
		String word = "";
		do {
			String affichage = (question.length() > 0) ? "\n" : "";
			System.out.print(question + affichage);
			word = scan.nextLine().toUpperCase();
		} while (!word.matches(pattern));
		return (word);
	}

	/**
	 * On renvoie la traduction de ce qu'ecrit l'utilisateur en boolean. Si ce n'est
	 * pas traductible en boolean, on repose la question.
	 * 
	 * @param question
	 *            L'enoncé de la question que pose le programme.
	 * @return renvoie la traduction en boolean de la reponse de l'utilisateur.
	 */
	public static boolean askYesNo(String question) {
		String word = "";

		do {
			String affichage = (question.length() > 0) ? "\n" : "";
			System.out.print(question + affichage);
			word = scan.nextLine().toUpperCase();
			if (word.equals("")) {
				return (true);
			}
		} while (!word.matches("TRUE|T|YES|Y|F|FALSE|NO|N"));
		return (word.matches("TRUE|T|YES|Y"));
	}

	/**
	 * On pose une question à l'utilisateur et on attend un int en reponse.
	 * 
	 * @param question
	 *            L'enoncé de la question que pose le programme.
	 * @return On renvoie un int si la reponse est un int sinon on repose la
	 *         question.
	 */
	public static int askInteger(String question) {
		String affichage = (question.length() > 0) ? "\n" : "";

		System.out.print(question + affichage);
		Scanner sca = new Scanner(scan.nextLine());
		while (!sca.hasNextInt()) {
			System.out.print(question + affichage);
			sca = new Scanner(scan.nextLine());
		}
		return (sca.nextInt());
	}

	/**
	 * On pose une question à l'utilisateur et on attend un int en reponse.
	 * 
	 * @param question
	 *            L'enoncé de la question que pose le programme.
	 * @param pattern
	 *            Sous forme Regex, ce String contient les différentes reponses
	 *            attendues (reponse1|reponse2|etc)
	 * @return On renvoie un int si la reponse est un int appartenant au pattern
	 *         sinon on repose la question.
	 */
	public static int askSpecifiedInteger(String question, String pattern) {
		String affichage = (question.length() > 0) ? "\n" : "";
		int res;

		System.out.print(question + affichage);
		Scanner sca = new Scanner(scan.nextLine());
		while (true) {
			if (sca.hasNextInt()) {
				res = sca.nextInt();
				if (String.valueOf(res).matches(pattern)) {
					return (res);
				}
			}
			System.out.print(question + affichage);
			sca = new Scanner(scan.nextLine());
		}
	}

	/**
	 * On pose la question a l'utilisateur et on attend une reponse sous forme d'int
	 * étant supérieur ou égal à min_bound et étant supérieur ou egal à max_bound.
	 * 
	 * @param question
	 *            L'enoncé de la question que pose le programme.
	 * @param error_message
	 * 			  Message qu'on affiche lors d'une mauvaise valeur rentree.
	 * @param min_bound
	 *            Borne inférieure du nombre attendu.
	 * @param max_bound
	 *            Borne supérieure du nombre attendu.
	 * @return int compris entre min_bound(inclus) et max_bound(inclus)
	 */
	public static int askSpecificInt(String question, String error_message, int min_bound, int max_bound) {
		String affichage = (question.length() > 0) ? "\n" : "";
		int res;

		System.out.print(question + affichage);
		Scanner sca = new Scanner(scan.nextLine());
		while (true) {
			if (sca.hasNextInt()) {
				res = sca.nextInt();
				if (res >= min_bound && res <= max_bound) {
					return (res);
				}
				else {
					System.out.println(error_message);
				}
			}
			System.out.println("It is not an integer.");
			System.out.print(question + affichage);
			sca = new Scanner(scan.nextLine());
		}
	}

	/**
	 * On pose la question a l'utilisateur et on attend une reponse sous forme d'int
	 * étant supérieur ou égal à min_bound et étant supérieur ou egal à max_bound.
	 * Si on a un element appartenant regex d'arret, on renvoie un int inférieur à min_bound.
	 *
	 * @param question
	 *            L'enoncé de la question que pose le programme.
	 * @param error_message
	 * 			  Message qu'on affiche lors d'une mauvaise valeur rentree.
	 * @param stop_regex
	 * 			  regex contenant les differents types d'arret du programme, on renvoie un entier inférieur à min_bound si on est dans ce cas la.
	 * @param min_bound
	 *            Borne inférieure du nombre attendu.
	 * @param max_bound
	 *            Borne supérieure du nombre attendu.
	 * @return int compris entre min_bound(inclus) et max_bound(inclus) ou inférieur à min_bound si on est dans une condition d'arret
	 */
	public static int askSpecificIntWithStop(String question, String error_message, String stop_regex, int min_bound, int max_bound) {
		String questRegex = stop_regex;
		String res;
		if(stop_regex.length() == 0) return askSpecificInt(question,error_message,min_bound,max_bound);
		else {
			for (int i = min_bound; i <= max_bound; i++) {
				questRegex += "|"+i;
			}
			res = askSpecificQuestion(question, error_message,questRegex);
			if (res.matches(stop_regex)) return min_bound-1;
			else return Integer.parseInt(res);
		}
	}

	/**
	 * On pose la question a l'utilisateur et on attend une reponse sous forme
	 * d'float étant supérieur ou égal à min_bound et étant supérieur ou egal à
	 * max_bound.
	 * 
	 * @param question
	 *            L'enoncé de la question que pose le programme.
	 * @param min_bound
	 *            Borne inférieure du nombre attendu.
	 * @param max_bound
	 *            Borne supérieure du nombre attendu.
	 * @return float compris entre min_bound(inclus) et max_bound(inclus)
	 */
	public static float askSpecificFloat(String question, float min_bound, float max_bound) {
		String affichage = (question.length() > 0) ? "\n" : "";
		float res;

		System.out.print(question + affichage);
		Scanner sca = new Scanner(scan.nextLine());
		while (true) {
			if (sca.hasNextFloat()) {
				res = sca.nextFloat();
				if (res >= min_bound && res <= max_bound) {
					return (res);
				}
			}
			System.out.print(question + affichage);
			sca = new Scanner(scan.nextLine());
		}
	}

	/**
	 * On pose la question à l'utilisateur et on renvoie sa reponse que si c'est un
	 * float sinon on repose la question.
	 * 
	 * @param question
	 *            L'enoncé de la question que pose le programme.
	 * @return la reponse de l'utilisateur à la question sous forme de float
	 */
	public static float askFloat(String question) {
		int flo = 0;

		while (!scan.hasNextFloat()) {
			String affichage = (question.length() > 0) ? "\n" : "";
			System.out.print(question + affichage);
		}
		return (scan.nextFloat());
	}

	/**
	 * On attend une reponse de l'utilisateur sous forme de boolean sans question.
	 * 
	 * @return traduction en boolean de la reponse.
	 */
	public static boolean askYesNo() {
		return (askYesNo(""));
	}

	/**
	 * On attend une reponse de l'utilisateur sous forme d'int sans question.
	 * 
	 * @return traduction en int de la reponse.
	 */
	public static int askInteger() {
		return (askInteger(""));
	}

	/**
	 * On attend une reponse de l'utilisateur sous forme de float sans question.
	 * 
	 * @return traduction en float de la reponse.
	 */
	public static float askFloat() {
		return (askFloat(""));
	}
}
