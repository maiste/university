package view.terminal;

import java.awt.Color;
import java.awt.Point;
import java.util.LinkedList;
import java.util.Random;

import controller.Control;
import controller.Intel;
import controller.Message;
import model.Blokus;
import model.Piece;
import model.players.IAPlayer;
import model.players.Player;
import view.GameView;
import view.menu.*;

/**
 * Cette classe gère l'affichage (et le déroulement) du tour dans le terminal.
 * La méthode principale play() joue un tour pour le joueur courant.
 * @author Blokus_1
 */
public class TerminalView implements GameView {

	private Control control;
	private boolean withTeam; // si il y a une équipe
	private final static int trainingTurn = 1000000;
	private boolean inGame; //si le jeu est lancé
	private Intel[] players = {new Intel("joueur 1",0),
					new Intel("joueur 2",1),
					new Intel("joueur 3",1),
					new Intel("joueur 4",1)
	};


/*****************
 * Constructeurs *
 *****************/

	/**
	 * Constructeur
	 * @param c le controleur à utiliser
	 */
	public TerminalView(Control c) {
		control = c;
		withTeam = false;
		inGame = false;
		Menu menu = createMenu();
		menu.run();
	}

/***************
 * Menu de jeu *
 ***************/

	/**
	 * Crée le menu lié au jeu courant
	 * @return le menu créé
	 */
	private Menu createMenu(){
		Menu m = new Menu();
		m.changeMenuType();
		m.addQuestion(new Question("Changer le mode de jeu", this::changeGameMode));
		m.addQuestion(new Question("Afficher le mode de jeu", this::showGameMode));
		m.addQuestion(new Question("Modifier les joueurs", this::modifyPlayer));
		m.addQuestion(new Question("Afficher l'état des joueurs", this::verifyPlayers));
		m.addQuestion(new Question("Afficher les règles", this::showRules));
		m.addQuestion(new Question ("Lancer le jeu", this::setControlGame));
		m.addQuestion(new Question ("Lancer l'entrainement de l'IA learning", this::restartGame));
		m.addQuestion(new Question("Quitter", () -> {
			m.invertRun();
			System.out.println("Merci d'avoir joué !");
		}));

		return m;
	}

	/**
	 * Fonction où on modifie le type du joueur 'id+1'
	 * @param id numéro du joueur
	 */
	private void askTypePlayer(int id){
		Intel information = players[id];
		String question  = "Vous avez choisi de modifier " + information.getName() + " ayant pour type " + information.getLevel() + ", entrer son type :\n"
						+ "- '0' pour un joueur réel\n"
						+ "- '1' pour une IA easy\n"
						+ "- '2' pour une IA medium\n"
						+ "- '3' pour une IA hard\n"
						+ "- '4' pour une intelligence artificielle\n\n"
						+ "ou entrer 5 pour retourner au menu";
		int level = ScanTerminal.askSpecificInt(question, "Veuillez entrer un chiffre entre 0 et 5", 0, 5);
		if (level == 5) {
			System.out.println("Abandon de la modification");
		}
		else {
			information.setLevel(level);
			printPlayer(id);
		}
	}

	/**
	 * On demande le nom du player
	 * @param id place de l'information dans le tableau
	 */
	private void askNamePlayer(int id){
		Intel information = players[id];
		String name = ScanTerminal.askString("Quel est le nom du joueur ?");
		if (name.toLowerCase().matches("chuck|norris")) {
			System.out.println("Chuck Norris ne s'abaisse pas à jouer dans le Terminal. \n" +
							"Mettez un autre nom !");
		} else {
			information.setName(name, id);
			printPlayer(id);
		}
	}



	/**
	 * Lance l'interface de changement du mode de jeu
	 */
	private void changeGameMode(){
		showGameMode();
		String question2 = "Entrez '1' pour J1vJ2vJ3vJ4, entrez '2' pour J1&J3 v J2&J4";
		int demande2 = ScanTerminal.askSpecificInt(question2, "Veuillez entrer une option entre 1 et 2", 1, 2);
		if (demande2 == 1) withTeam = false;
		else if (demande2 == 2) withTeam = true;
		showGameMode();
	}


	/**
	 * Affiche le mode de jeu de la partie actuellement choisie
	 */
	private void showGameMode () {
		System.out.print("Le mode de jeu actuel est ");
		if (this.withTeam) System.out.println("J1&J3 v J2&J4\n");
		else System.out.println("J1vJ2vJ3vJ4\n");
	}


	/**
	 * Permet de changer le type des joueurs
	 */
	private void modifyPlayer () {
		Menu playerMenu = new Menu();
		for(int i = 0; i < 4 ; i++){
			int id = i;
			Question qPlayer = new Question("Modifier le joueur " + (id+1) + ": " +	players[id].getName(), null);
			qPlayer.setAction(() -> {
				Menu modify = new Menu();
				modify.addQuestion(new Question("Changer le type du joueur",() -> { printPlayer(id) ; askTypePlayer(id); }));
				modify.addQuestion(new Question("Changer le nom du joueur",() -> { printPlayer(id) ; askNamePlayer(id); }));
				modify.addQuestion(new Question("Retourner au menu précédent",modify::invertRun));
				modify.run();
				qPlayer.setQuestion("Modifier le joueur " + (id+1) + ": " +	players[id].getName());
			});
			playerMenu.addQuestion(qPlayer);
		}
		playerMenu.addQuestion(new Question("Retourner au menu principal", playerMenu::invertRun));
		playerMenu.run();
	}

	/**
	 * Affiche le type de chaque joueur s'il est initialisé
	 */
	private void verifyPlayers () {
		for (int i = 0; i < players.length; i++) {
			switch (i){
				case 0:
					System.out.print(new TerminalStyleString("  ", TerminalStyleString.BgColors.LIGHTBLUE)+" ");
					break;
				case 1:
					System.out.print(new TerminalStyleString("  ", TerminalStyleString.BgColors.YELLOW)+" ");
					break;
				case 2:
					System.out.print(new TerminalStyleString("  ", TerminalStyleString.BgColors.RED)+" ");
					break;
				case 3:
					System.out.print(new TerminalStyleString("  ", TerminalStyleString.BgColors.GREEN)+ " ");
					break;
			}
			printPlayer(i);
		}
		System.out.println();
	}

	/**
	 * Affichage de la présentation d'un Player dans le terminal
	 * @param id id du joueur
	 */
	private void printPlayer(int id){
		Intel information = players[id];
		switch (information.getLevel()){
			case 0:
				System.out.println("J" + (id+1) + " --> " + information.getName() + " est un joueur réel");
				break;
			case 1:
				System.out.println("J" + (id+1) + " --> " + information.getName() + " est un IA Easy");
				break;
			case 2:
				System.out.println("J" + (id+1) + " --> " + information.getName() + " est un IA Medium");
				break;
			case 3:
				System.out.println("J" + (id+1) + " --> " + information.getName() + " est un IA Hard");
				break;
			case 4:
				System.out.println("J" + (id+1) + " --> " + information.getName() + " est un IA en apprentissage");
				break;
			default:
				System.out.println("J" + (id+1) + " --> " + information.getName() + " est un joueur inconnu");
				break;
		}
	}

	/**
	 * On recommence un jeu automatiquement pour l'entrainement de l'ia learning
	 */
	private void restartGame(){
		System.out.println("Vous lancez un entrainement de l'IA sur " + trainingTurn + " tours");
		for (int i = 0 ; i < trainingTurn ; i++) {
			System.out.println("Tour d'entrainement n°"+i);
			changePlayer();
			control.setGame(new Blokus(this.players, this.withTeam));
			control.getGame().setTrainingMode(true);
			inGame = true;
			play(true);
		}
	}

	/**
	 * on change les Players de façon aléatoire pour l'entrainement de l'IA learning
	 */
	private void changePlayer(){
		Random rand = new Random();
		int learning = rand.nextInt(4);
		for (int i = 0; i < players.length; i++) {
			if(i == learning){
				players[i].setLevel(4);
				players[i].setName("skynet",i);
			}
			else {
				switch (rand.nextInt(3)) {
					case 0:
						players[i].setLevel(1);
						players[i].setName("ia easy", i);
						break;
					case 1:
						players[i].setLevel(2);
						players[i].setName("ia medium", i);
						break;
					default:
						players[i].setLevel(3);
						players[i].setName("ia hard",i);
						break;

				}
			}
		}
	}



	/**
	 * Lance le jeu du Blokus
	 */
	private void setControlGame(){
		control.setGame(new Blokus(this.players, this.withTeam));
		inGame = true;
		play();
	}

	/**
	 * Affiche les règles du jeu
	 */
	private void showRules(){
		System.out.println("Règles du Jeu du Blokus : ");
		System.out.println("Le jeu consiste à placer, chacun son tour, des pièces de 1 à 5 cases de formes\n" +
						"variables. Chaque pièce rapporte le nombre de points correspondant au nombre de cases de\n" +
						"la pièce posée. La partie se termine quand les joueurs ne peuvent plus poser de pièce. Le\n" +
						"jeu se joue selon l'ordre des couleurs : bleu -> jaune -> rouge -> vert. La première pièce se\n" +
						"pose sur la case dans l'angle du plateau associé à la couleur du joueur. Ensuite, une pièce \n" +
						"peut être posée si un des coins d'une pièce est en contact avec le coin d'une des pièces du \n" +
						"joueur qui est déjà posée. De plus, aucun des cotés de la pièce ne doit être en contact avec \n" +
						"le coté d'une des pièces du joueur. Quand plus personne ne peut jouer, on compte les points.");
	}


	/***************************
	 * Méthodes de L'interface *
	 ***************************/
	@Override
	public void play(){
		play(false);
	}

	/**
	 * Joue un tour pour le joueur courant puis passe au joueur suivant.
	 * Affiche les scores si la partie est finie
	 */
	private void play(boolean loop) {
		long startTime = System.currentTimeMillis();
		while (!control.getGame().gameIsFinish() && inGame) {
			if (canPlay()) {
				if (!(control.isIA())) {
					printBoard();
					printPieces();
					playPiece();
				} else {
					if (!loop)System.out.println("Cest à l'IA de jouer.");
					Message message = ((IAPlayer) control.getGame().getCurrentPlayer()).getAnswer();
					if(message == null){
						if(!loop){
							System.out.println("L'IA "+control.getGame().getCurrentPlayer().id+" ne peut plus jouer.");
						}
					}
					else {
						control.sendMessage(message);
						if (!loop)printBoard();
					}
					try {
						if (!loop)Thread.sleep(250);
					}
					catch (Exception e) {
					}
					if (!loop)System.out.println();
				}
			}
			control.getGame().updatePlayer();
		}
		if (loop)System.out.println((System.currentTimeMillis()-startTime)/1000. + "s");
		printBoard();
		Player[] tab = control.getClassement();
		if (! withTeam) {
			for (int i = 0; i < tab.length; i++)
				System.out.println(tab[i] + ", a eu " + tab[i].getScore());
		}
		else {
			System.out.println("La team 1 (J1 & J3) a eu " + tab[0].getScore());
			System.out.println("La team 2 (J2 & J4) a eu " + tab[1].getScore());
		}
	}

	@Override
	/**
	 * Affiche le plateau dans son état actuel
	 */
	public void printBoard() {
		Player[][] board = control.getGame().getBoard();
		System.out.print("  ");
		for (int i = 0; i < board.length; i++)
			System.out.print(" " + (char)(i+65));
		System.out.println();
		for (int i = 0; i < board.length; i++) {
			if (i < 10) System.out.print(i + "  ");
			else System.out.print(i + " ");
			for (int j = 0; j < board[i].length; j++) {
				if (i == 0 && j == 0) System.out.print(getColor(control.getGame().getPlayerAt(0), "  ", true));
				else if (i == 0 && j == 19) System.out.print(getColor(control.getGame().getPlayerAt(1), "  ", true));
				else if (i == 19 && j == 19) System.out.print(getColor(control.getGame().getPlayerAt(2), "  ", true));
				else if (i == 19 && j == 0) System.out.print(getColor(control.getGame().getPlayerAt(3), "  ", true));
				else if (board[i][j] == null) System.out.print(". ");
				else System.out.print(getColor(board[i][j], "  ", true));
			}
			System.out.println();
		}
		System.out.println();
	}



	/***************************
	 * Affichage des Pieces *
	 ***************************/

	/**
	 * Affiche les pièces du joueur en s'adaptant à la taille du terminal en mettant
	 * le plus de pièces de la même taille sur la même ligne. Sinon, on va à la
	 * ligne pour chaque changement de nombre de cases de pièces. En dessous de
	 * chaque ligne de piece seront affichés leurs numéros.
	 */

	/**
	 * Vérifie si les lignes du tableau sont vides
	 * @param tableau à vérifier
	 * @return []boolean contenant si la ligne est vide ou non pour chaque ligne
	 */
	private boolean[] linesIsEmpty(String[][] tableau) {
		boolean lineIsEmpty[] = new boolean[tableau.length];
		for (int i = 0; i < tableau.length; i++) {
			lineIsEmpty[i] = true;
			for (int j = 0; j < tableau[i].length; j++) {
				if (tableau[i][j].matches("[#¤]"))
					lineIsEmpty[i] = false;
			}
		}
		return lineIsEmpty;
	}

	/**
	 * Vérifie si les lignes du tableau sont vides
	 * @param tableau à vérifier
	 * @return []boolean contenant si la ligne est vide ou non pour chaque ligne
	 */
	private boolean[] linesIsEmpty(Point[][] tableau) {
		boolean lineIsEmpty[] = new boolean[tableau.length];
		for (int i = 0; i < tableau.length; i++) {
			lineIsEmpty[i] = true;
			for (int j = 0; j < tableau[i].length; j++) {
				if (tableau[i][j] != null)
					lineIsEmpty[i] = false;
			}
		}
		return lineIsEmpty;
	}

	/**
	 * Affiche les pièces du joueur courant
	 */
	private void printPieces() {
		LinkedList<LinkedList<Piece>> piecesJCourant = control.getGame().getCurrentPlayer().getPieces();
		String[][] megaTableau;
		for (int k = 0; k < piecesJCourant.size(); k++) {
			LinkedList<Piece> l = piecesJCourant.get(k);
			if (!(l.isEmpty())) {
				megaTableau = createMegaTableau(l);
				printMegaTableau(megaTableau);
				for (Piece p : l) {
					if (p.getId()%21 > 10)
						System.out.print("  " + p.getId() % 21 + "   ");
					else
						System.out.print("  " + p.getId() % 21 + "    ");
				}
				System.out.println("\n");
			}
		}
	}

	/**
	 * Créer un tableau à partir d'une liste de pieces en affichant côte à côte tous
	 * les tableaux de chaque piece de la liste
	 * @param l liste à transformer
	 * @return tableau à afficher
	 */
	private String[][] createMegaTableau (LinkedList<Piece> l) {
		String [][] megaTableau = new String[5][l.size() * 5];
		Point[][] tab;
		int ind = 0;
		for (Piece p : l) {
			tab = p.getCoordonates();
			for (int i = 0; i < megaTableau.length; i++) {
				for (int j = 0; j < megaTableau.length; j++) {
					if (tab[i][j] == null) megaTableau[i][j + 5 * ind] = ".";
					else if (tab[i][j].equals(p.getReference())) megaTableau[i][j + 5 * ind] = "¤";
					else megaTableau[i][j + 5 * ind] = "#";
				}
			}
			ind++;
		}
		return megaTableau;
	}

	/**
	 * Affiche le tableau passé en paramètre avec les couleurs associées
	 * @param megaTableau tableau à afficher
	 */
	private void printMegaTableau (String [][] megaTableau) {
		for (int i = 0; i < megaTableau.length; i++) {
			boolean isBreak = linesIsEmpty(megaTableau)[i];
			for (int j = 0; j < megaTableau[i].length; j++) {
				if (isBreak) break;
				if (megaTableau[i][j].matches("[#¤]")) System.out.print(getColor(control.getGame().getCurrentPlayer(), megaTableau[i][j], false));
				else System.out.print(megaTableau[i][j]);
				if ((j + 1) % 5 == 0) System.out.print("  ");
			}
			if (!(isBreak)) System.out.println();
		}
	}

	/**
	 * Affiche une pièce dans le terminal
	 * @param p Piece à afficher
	 */
	private void printPiece(Piece p) {
		System.out.println("Pièce actuelle :");
		Point[][] t = p.getCoordonates();
		boolean [] isBreak = linesIsEmpty(t);
		for (int i = 0; i < t.length; i++) {
			for (int j = 0; j < t[i].length; j++) {
				if (isBreak[i]) break;
				if (t[i][j] == null) { System.out.print("."); }
				else if (t[i][j].equals(p.getReference())) { System.out.print(getColor(control.getGame().getCurrentPlayer(), "¤", false)); }
				else { System.out.print(getColor(control.getGame().getCurrentPlayer(), "#", false)); }
			}
			System.out.println();
		}
		System.out.println("Le symbole '¤' représente la case de référence de la pièce\n"
						+ "quand vous placer la pièce sur le plateau, veillez à toujours donner\n"
						+ "les coordonnées relativement à la position que vous souhaitez pour"
						+ "la case de référence.\n");
	}



/*****************
 * Verifications *
 ****************/

	/**
	 * Regarde si le joueur courant peut jouer au moins une pièce, s'il ne peut pas
	 * on affiche qu'il ne peut plus jouer et on passe au joueur suivant.
	 *
	 * @return si le joueur courant peut jouer ou non
	 */
	private boolean canPlay() {
		Player currentPlayer = control.getGame().getCurrentPlayer();
		return currentPlayer.getCanPlay();
	}


	/***************************
	 *  Affichage Menu en jeu  *
	 ***************************/

	/**
	 * Menu de jeu du joueur
	 */
	private void playPiece() {
		StringMenu selectMenu = new StringMenu();
		String enPiecesPossibles = getPlayerList();
		Container current = new Container(null);
		selectMenu.addQuestion(new Question("pour choisir la pièce de numéro n",
						()->{	current.setPiece(selectPiece(selectMenu.getDemande()));	}), enPiecesPossibles);
		selectMenu.addQuestion(new Question("pour faire le miroir de la pièce actuellement choisie",
						() -> turnPiece(current.getPiece(),2)), "m");
		selectMenu.addQuestion(new Question("pour tourner la pièce actuellement choisie",
						()->turnPiece(current.getPiece(),1)), "r");
		selectMenu.addQuestion(new Question("pour poser la pièce choisie",
						()-> {
							if(putPiece(current.getPiece())) { selectMenu.invertRun(); }
						}), "p");
		selectMenu.addQuestion(new Question("pour afficher la pièce séléctionnée",
						()->printCurrent(current.getPiece())), "s");
		selectMenu.addQuestion(new Question("pour ré-afficher les pièces", this::printPieces), "pi");
		selectMenu.addQuestion(new Question("pour ré-afficher le plateau", this::printBoard),"t");
		selectMenu.addQuestion(new Question("Quitter",
						()->{ selectMenu.invertRun(); inGame = false; }), "q");
		selectMenu.run();
	}

	/** Recupère la liste des id des pièces du joueur sous forme d'un string
	 * @return id des pièces du joueur séparé par un '|'
	 */
	private String getPlayerList(){
		String enPiecesPossibles = "";
		for (LinkedList<Piece> pieces : control.getGame().getCurrentPlayer().getPieces()) {
			for (Piece p : pieces) { enPiecesPossibles += p.getId()%21 + "|"; }
		}
		return enPiecesPossibles.substring(0, enPiecesPossibles.length() - 1);
	}

	/** change la pièce */
	private void turnPiece(Piece current, int type) {
		if (current != null) {
			control.sendMessage(new Message(null, current, type));
			printPiece(current);
		} else
			System.out.println("Veuillez choisir une pièce avant.\n");
	}

	/** Affiche la pièce */
	private void printCurrent(Piece current) {
		if (current != null) {
			printPiece(current);
		} else {
			System.out.println("Veuillez choisir une pièce avant.\n");
		}
	}

	/** On demande les coordonnées d'une pièce et on la pose sur le plateau si possible
	 * @return true si la pièce a été bien posée, false sinon
	 */
	private boolean putPiece(Piece current) {
		if (current != null) {
			printPiece(current);
			printBoard();
			String abs = ScanTerminal.askSpecificQuestionWithStop("Veuillez choisir une case en abscisse (entre A et T), ou 'back' pour retour au menu précédent.",
							"Il faut une lettre entre A et T svp","back", "[A-T]");
			if (abs != null) {
				int numAbs = abs.charAt(0) -65;
				int ord = ScanTerminal.askSpecificIntWithStop("Veuillez choisir une case en ordonnée (entre 0 et 19), ou 'back' pour retour au menu précédent.",
								"Il faut un entier entre 0 et 19 svp", "back", 0, 19);
				if (ord != -1) {
					boolean aEtePosee = control.sendMessage(new Message(new Point(numAbs, ord), current, 0));
					if (aEtePosee) { return true; }
					else { System.out.println("La pièce ne peut pas être posée ici\n"); }
				}
			}
		}	else {	System.out.println("Veuillez choisir une pièce avant.\n"); }
		return false;
	}

	/** On sélectionne la pièce si elle est jouable, sinon on a un message d'erreur et on recommence
	 * @return la pièce choisie
	 */
	private Piece selectPiece(String demande) {
		Piece essai = control.getGame().getPieceFromID(Integer.parseInt(demande));
		if (!control.canPlayPiece(essai)) {
			System.out.println("La pièce ne possède aucune position jouable, veuillez en choisir une autre.\n");
		}
		if (essai != null) {	printPiece(essai); }
		return essai;
	}


	/***************************
	 * Autres méthodes *
	 ***************************/

	/**
	 * Renvoie une String s avec la couleur du Player p sous forme d'un TerminalStyleString
	 *
	 * @param p joueur dont on veut récupérer la couleur
	 * @param s  String que l'ont veut coloré
	 * @return String avec la couleur
	 */
	private TerminalStyleString getColor(Player p, String s, boolean surligne) {
		Color coulJoueur = p.getColor();
		if (coulJoueur == Color.BLUE) {
			TerminalStyleString t = TerminalStyleString.LIGHTBLUE(s);
			if (surligne)
				t.changeBackground(TerminalStyleString.BgColors.LIGHTBLUE);
			t.addEffect(TerminalStyleString.Effects.BOLD);
			return t;
		} else if (coulJoueur == Color.YELLOW) {
			TerminalStyleString t = TerminalStyleString.YELLOW(s);
			if (surligne)
				t.changeBackground(TerminalStyleString.BgColors.YELLOW);
			t.addEffect(TerminalStyleString.Effects.BOLD);
			return t;
		} else if (coulJoueur == Color.RED) {
			TerminalStyleString t = TerminalStyleString.RED(s);
			if (surligne)
				t.changeBackground(TerminalStyleString.BgColors.RED);
			t.addEffect(TerminalStyleString.Effects.BOLD);
			return t;
		} else {
			TerminalStyleString t = TerminalStyleString.GREEN(s);
			if (surligne)
				t.changeBackground(TerminalStyleString.BgColors.GREEN);
			t.addEffect(TerminalStyleString.Effects.BOLD);
			return t;
		}
	}

	/** Classe interne de transport de pièce */
	private class Container {
		Piece p;
		private Container(Piece p){
			this.p = p;
		}
		Piece getPiece() { return p; }
		void setPiece(Piece change) { p = change; }
	}



}
