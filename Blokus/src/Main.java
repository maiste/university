import controller.Control;

/**
 * Classe de lancement du jeu du Blokus
 * @author Blokus_1
 */
public class Main {

	/**
	 * Fonction main e lancement du jeu du Blokus
	 * @param args le tableau d'arguments
	 */
	public static void main(String[] args){
		if (args.length != 0 && args[0].equals("1")) {
			new Control(true);
		}
		else {
			new Control (false);
		}
	}
	
}