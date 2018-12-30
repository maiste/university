package view.graphic;

import controller.Message;
import model.Piece;
import model.players.IAPlayer;
import view.GameView;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;

/**
 * Panel principal du jeu.
 * Gère le déroulement du jeu.
 *
 */

public class BoardPanel extends JPanel implements GameView {
	
	private GraphicView frame;
	private Piece pieceSelected;
	private JPanel piece;
	private CaseButton [][] grid;
	protected JPanel board;
	protected JPanel menu;
	private JPanel gridPieces = new JPanel();
	private boolean aJoue = false;
	private boolean stopIa = false;
	private CaseButton actuEv;
	private LinkedList<Point> casePlayable;
	private boolean help = false;

	/**
	*  Initialise le panel de jeu
	*/

	public BoardPanel (GraphicView f) {
		frame = f;
		f.setContentPane(this);
		setLayout(new GridBagLayout());
		addKeyListener(new GameKey());
		addMouseWheelListener(new GameMolette());
		addMouseListener(new MousePanelLis());
		requestFocus();
		initMenu();
		initBoard();
		play();
	}

	public void paintComponent(Graphics g) {
	    g.drawImage(new ImageIcon("lib/menu.jpg").getImage(), 0,0,getWidth(), getHeight(), null);
    }

	/**
	 * Initialise la partie plateau du Panel
	 */


	/**
	 *  Initialise le plateau de jeu
	 */
	private void initBoard() {
        createCaseButton();
        createBoard();
	}

	/**
	 *  Crée la grille du plateau
	 */
    private void createCaseButton() {
        grid = new CaseButton[20][20];
        for(int i = 0; i < grid.length; i++) {
            for(int j = 0; j < grid[i].length; j++) {
                grid[i][j] = new CaseButton(i,j,frame.getControl().getGame().getBoard());
            }
        }
    }

	/**
	 * 	Affecte les listeners et couleurs à la grille du plateau
	 */
    private void createBoard() {
        board = new JPanel();
        board.setOpaque(false);
        Color color;
        board.setLayout(new GridBagLayout());
        for(int i = 0; i < 20; i++) {
            for(int j = 0; j < 20; j++) {
                grid[i][j].addActionListener(new ActionGridButton());
                grid[i][j].addMouseListener(new MouseGridButton());
                grid[i][j].setBorder(new LineBorder(Color.BLACK));
                grid[i][j].setEnabled(true);
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = i;
                gbc.gridy = j;
                gbc.gridheight = 1;
				gbc.gridwidth = 1;
				gbc.fill = GridBagConstraints.BOTH;
				gbc.weightx = gbc.weighty = 0.05;
                board.add(grid[j][i], gbc);
                if(i==0 && j==0) {
                    color = frame.getControl().getGame().getPlayerAt(0).getColor();
                    grid[i][j].setBackground(color);
                    grid[i][j].setOpaque(true);
                }
                else if (i==0 && j==19) {
                    color = frame.getControl().getGame().getPlayerAt(1).getColor();
                    grid[i][j].setBackground(color);
                    grid[i][j].setOpaque(true);
                }
                else if (i==19 && j==19) {
                    color = frame.getControl().getGame().getPlayerAt(2).getColor();
                    grid[i][j].setBackground(color);
                    grid[i][j].setOpaque(true);
                }
                else if (i==19 && j==0) {
                    color = frame.getControl().getGame().getPlayerAt(3).getColor();
                    grid[i][j].setBackground(color);
                    grid[i][j].setOpaque(true);
                }
                else {
                    color = Color.white;
                    grid[i][j].setBackground(color);
                    grid[i][j].setOpaque(true);
                }
            }
        }
		GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
		gbc.gridheight = gbc.gridwidth = GridBagConstraints.RELATIVE;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = gbc.weighty = 1;
		gbc.insets = new Insets(20,20,20,20);
		add(board, gbc);
    }

	/**
	 * Initialise le menu
	 */
	public void initMenu() {
		menu = new JPanel();
		menu.setBackground(new Color(56,147,240));
		menu.setOpaque(false);
		menu.setLayout(new GridBagLayout());
		JButton rules = new JButton("      Règles de jeu     ");
		rules.addActionListener(e -> {
			JOptionPane.showMessageDialog(this, regles(), "Règles",
					JOptionPane.INFORMATION_MESSAGE);
		});
        rules.setBackground(Color.WHITE);
        JButton help = new JButton("Aide");
		help.addActionListener(e -> {
		    playableHelp(Color.MAGENTA);
        });
		help.setBackground(Color.white);
		piece = new JPanel();
		piece.setLayout(new GridLayout(5,5));
		piece.setOpaque(false);
		actuPanCurrentPiece();
		initGridPieces();
		JButton quit = new JButton("Quit");
		quit.setBackground(Color.WHITE);
		quit.addActionListener(e -> {
			new Audio("lib/validate.wav").play();
			stopIa = true;
			frame.initMenu();
		});
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridheight = gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.ipadx = 20;
		gbc.ipady = 6;
		gbc.insets = new Insets(20,0,20,20);
		add(menu, gbc);
        // Règles :
		gbc = new GridBagConstraints();
		gbc.gridx = gbc.gridy = 0;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weighty = 0.5;
		gbc.insets = new Insets(0,0,10,0);
		menu.add(rules, gbc);
		// Aide :
        gbc.gridy = 1;
        gbc.gridheight = 1;
        menu.add(help, gbc);
		// Piece courante :
		gbc.weighty = 0;
		piece.setPreferredSize(new Dimension(150,120));
		gbc.gridy = 2;
		menu.add(piece, gbc);
		// Pieces dispos :
		gbc.gridy = 3;
		gbc.weighty = 0;
		gridPieces.setPreferredSize(new Dimension(200,300));
		menu.add(gridPieces, gbc);
		// Quitter :
		gbc.gridy = 4;
		gbc.gridheight = GridBagConstraints.REMAINDER;
		gbc.anchor = GridBagConstraints.PAGE_END;
		gbc.insets = new Insets(20, 0, 0, 0);
		menu.add(quit, gbc);
	}

	/**
	 * 	Actualise la grille qui affiche les pièces
	 */
	private void initGridPieces() {
		gridPieces.removeAll();
		gridPieces.setOpaque(false);
		gridPieces.setLayout(new GridLayout(7,3));
		gridPieces.setMinimumSize(new Dimension(5, 1));
		LinkedList<LinkedList<Piece>> piecesJCourant = frame.getControl().getGame().getCurrentPlayer().getPieces();
		PieceButton piecesButton;
		int cmp = 0;
		for(int i = 0; i < piecesJCourant.size(); i++) {
			for (int j = 0; j < piecesJCourant.get(i).size(); j++) {
			    cmp++;
				piecesButton = new PieceButton(piecesJCourant.get(i).get(j).getId()%21 + "-" + frame.getControl().getGame().getCurrentPlayer().getId());
				piecesButton.addActionListener(new ActionPieceButton());
				piecesButton.setOpaque(true);
				piecesButton.setBackground(Color.white);
				if (frame.getControl().isIA())
					piecesButton.setEnabled(false);
				gridPieces.add(piecesButton);
			}
		}
		for(int i = 0; i < (21-cmp); i++) {
		    JPanel remplirVide = new JPanel();
            remplirVide.setOpaque(false);
            gridPieces.add(remplirVide);
        }
		menu.revalidate();
	}

	/**
	 * Actualise la pièce courante dans le menu
	 */
	private void actuPanCurrentPiece() {
		piece.removeAll();
			for(int i = 0; i < 5; i++) {
				for(int j = 0; j < 5; j++) {
					PieceButton bouton = new PieceButton(frame.getControl().getGame().getCurrentPlayer().getId());
					if (pieceSelected != null) {
						if (pieceSelected.getCoordonates()[i][j] == null) {
							bouton.setImg(null);
						}
					}
					else bouton.setImg(null);
					bouton.setEnabled(false);
					bouton.addMouseListener(new MousePanelLis());
					bouton.setBorderPainted(false);
					piece.add(bouton);
				}
			}
		piece.revalidate();
	}

	/**
	 * 	Actualise le plateau
	 */
	public void printBoard() {
		Color color;
		for(int i = 0; i < 20; i++) {
			for(int j = 0; j < 20; j++) {
				color = frame.getControl().getGame().getBoard()[i][j].getColor();
				grid[i][j].setBackground(color);
			}
		}
	}

	/**
	 * 	Joue le tour des IA tant que le joueur suivant est un IA
	 */
	public void play() {
		Thread t = new Thread(() -> {
			stopIa = false;
			while (!stopIa) {
				if(frame.getControl().isIA()) {
					try {
						boolean needDial = false;
						if ((((IAPlayer) frame.getControl().getGame().getCurrentPlayer())).getCoef() == 1.75)
							needDial = true;
						JDial dial = null;
						if (needDial)
                        	dial = new JDial(frame, "", false);
						frame.getControl().sendMessage(((IAPlayer) frame.getControl().getGame().getCurrentPlayer()).getAnswer());
						if (needDial)
							dial.dispose();
						requestFocus();
					}
					catch (Exception e) { e.printStackTrace(); }
					if (frame.getControl().getGame().gameIsFinish())
						stopIa = true;
					try {
						Thread.sleep(50);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
                    changePlayer();
				}
				else
					stopIa = true;
			}
		});
        t.start();
	}

	/**
	 * Adapte les actions en fonction du joueur suivant
	 */
	public void changePlayer() {
		if (frame.getControl().getGame().gameIsFinish()) {
			frame.initEndMenu();
		}
		else {
			pieceSelected = null;
			actuPanCurrentPiece();
            frame.getControl().getGame().updatePlayer();
			initGridPieces();
			piece.removeAll();
			removeMagenta();
			aJoue = false;
			help = false;
            repaint();
            if(!frame.getControl().getGame().getCurrentPlayer().getCanPlay()) {
                changePlayer();
            }
			Toolkit.getDefaultToolkit().sync();
		}
	}

	private void removeMagenta() {
	    for(int i = 0; i < 20; i++) {
	        for(int j = 0; j < 20; j++) {
	            if(grid[i][j].getBackground() == Color.MAGENTA) {
	                grid[i][j].setBackground(Color.WHITE);
	            }
            }
        }
    }
	private void playableHelp(Color color) {
	    help = true;
        LinkedList<Point> playableCase = frame.getControl().getGame().getGoodCase();
        casePlayable = playableCase;
        for(Point p : playableCase) {
            grid[(int)p.getY()][(int)p.getX()].setBackground(color);
        }
    }

	/**
	 * @param x Coordonnée dans la grille
	 * @param y Coordonnée dans la grille
	 * @return 	Vérifie si pieceSelected peut être joué en coordonnées (x,y)
	 */
	private boolean canPlayPiece(int x, int y) {
		if(!(pieceSelected ==null)) {
			return frame.getControl().sendMessage(new Message(new Point(x,y), pieceSelected, 0));
		}
		return false;
	}

	private String regles() {
		return "Le jeu consiste à placer chacun son tour des pièces de 1 à 5 cases de formes " +
                "variables.\nChaque pièce rapporte le nombre de points correspondant au nombre de cases de " +
                "la pièce posée.\nLa partie se termine quand les joueurs ne peuvent plus poser de pièce.\nLe " +
                "jeu se joue selon l'ordre des couleurs bleu -> jaune -> rouge -> vert.\nLa première pièce se " +
                "pose sur la case dans l'angle du plateau associé à la couleur du joueur.\nEnsuite, une pièce " +
                "peut être posée si un des coins des pièces est en contact avec le coin d'une des pièces du " +
                "joueur qui est déjà posée.\nDe plus, aucun des cotés de la pièce ne doit être en contact avec " +
                "le coté d'une des pièces du joueur.\nQuand plus personne ne peut jouer, on compte les points.\n" + 
                "Rotation : Appuyer sur r ou tourner la molette\n" +
                "Miroir : Appuyer sur m ou sur le clic droit";
	}

	 /* ------------------
	 -
	 -   ACTIONS DES     -
	 -    LISTENERS      -
	 -
	 -------------------*/

	/**
	 * = MouseEntered
	 */
	public void actionSurvol () {
		int x = actuEv.getCoorX();
		int y = actuEv.getCoorY();
		if(pieceSelected != null) {
			for(int i = -2; i < 3; i++) {
				for(int j = -2; j < 3; j++) {
					if (!((y+i==0 && x+j==0) || (y+i==19 && x+j==0) || (y+i==19 && x+j==19) ||
                            (y+i==0 && x+j==19) || y+i < 0 ||y+i > 19 || x+j < 0 || x+j>19)) {
						if (pieceSelected.getCoordonates()[i + 2][j + 2] != null) {
							if(frame.getControl().getGame().verifyPosition(new Message(new Point(x,y), pieceSelected, 0))) {
								grid[y + i][x + j].setBackground(frame.getControl().getGame().getCurrentPlayer().getColor());
							}
							else {
								grid[y + i][x + j].setBackground(Color.GRAY);
							}
						}
					}
				}
			}
		}
	}

	/**
	 *  = MouseExited
	 */
	public void actionSortie () {
		int x = actuEv.getCoorX();
		int y = actuEv.getCoorY();
		for(int i = -2; i < 3; i++) {
			for(int j = -2; j < 3; j++) {
				if (!((y+i==0 && x+j==0) || (y+i==19 && x+j==0) || (y+i==19 && x+j==19) || (y+i==0 && x+j==19) ||
                        y+i < 0 ||y+i > 19 || x+j < 0 || x+j>19)) {
					if (y + i >= 0 && y + i < 20) {
						if (x + j >= 0 && x + j < 20) {
						    grid[y + i][x + j].setBackground(Color.white);
						    if (help == true) {
						        for(Point p: casePlayable) {
						            if (grid[y + i][x + j] == grid[(int)p.getY()][(int)p.getX()]) {
                                        grid[y + i][x + j].setBackground(Color.MAGENTA);
                                    }
                                }
                            }
						}
					}
				}
			}
		}
	}

    public void mouseClicDroit() {
        if(pieceSelected != null) {
            pieceSelected = pieceSelected.mirror();
        }
        if(actuEv != null) {
            actionSortie();
            actionSurvol();
            requestFocus();
        }
        actuPanCurrentPiece();
    }


     /* ------------------
	 -
	 -  CLASSES INTERNES -
	 -
	 -------------------*/

    class JDial extends JDialog {

        Image gif = new ImageIcon("lib/sablier.gif").getImage();

        public JDial (JFrame parent, String tittle, boolean modal) {
            super(parent, tittle, modal);
            this.setSize(100, 100);
            this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            this.setLocationRelativeTo(board);
            setUndecorated(true);
            setOpaque(false);
            add(new JPanel() {
				public void paintComponent (Graphics g) {
					super.paintComponent (g);
					g.drawImage(gif, 0,0,getWidth(), getHeight(), this);
				}
			});
            setVisible(true);
        }
    }

    /**
	 * 	Gère l'interaction pour choisir une pièce
	 */
	class ActionPieceButton implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			int num = ((PieceButton)(actionEvent.getSource())).getNum();
			pieceSelected = frame.getControl().getGame().getPieceFromID(num);
			actuPanCurrentPiece();
			requestFocus();
			repaint();
		}
	}

	/**
	 * Gère l'interaction pour poser une pièce
	 */
	class ActionGridButton implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			int x = ((CaseButton)(actionEvent.getSource())).getCoorX();
			int y = ((CaseButton)(actionEvent.getSource())).getCoorY();
			aJoue = canPlayPiece(x,y);
			if(aJoue) {
				Audio ad = new Audio("lib/click.wav");
				ad.play();
				changePlayer();
				if (frame.getControl().isIA()) {
					play();
				}
			}
		}
	}

	/**
	 * Gère le survol des cases de la grille
	 */
	class MouseGridButton implements MouseListener {

		@Override
		public void mouseClicked(MouseEvent mouseEvent) {
			int button = mouseEvent.getButton();
			if (button == mouseEvent.BUTTON3) {
				mouseClicDroit();
			}
		}

		public void mousePressed(MouseEvent mouseEvent) {}
		public void mouseReleased(MouseEvent mouseEvent) {}

		@Override
		public void mouseEntered(MouseEvent mouseEvent) {
			actuEv = ((CaseButton)(mouseEvent.getSource()));
			actionSurvol();
			requestFocus();
		}

		@Override
		public void mouseExited(MouseEvent mouseEvent) {
			actionSortie();
			actuEv = null;
		}
	}

	class MousePanelLis implements MouseListener {

		@Override
		public void mouseClicked(MouseEvent mouseEvent) {
			int button = mouseEvent.getButton();
			if (button == mouseEvent.BUTTON3) {
				mouseClicDroit();
			}
		}

		public void mousePressed(MouseEvent mouseEvent) {}
		public void mouseReleased(MouseEvent mouseEvent) {}
		public void mouseEntered(MouseEvent mouseEvent) {}
		public void mouseExited(MouseEvent mouseEvent) {}
	}

	class GameMolette implements MouseWheelListener {

		@Override
		public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
			int notches = mouseWheelEvent.getWheelRotation();
			if(pieceSelected != null) {
				if (notches < 0) {
					pieceSelected = pieceSelected.rotate();
				} else {
					for (int i = 0; i < 3; i++)
						pieceSelected = pieceSelected.rotate();
				}
			}
			if(actuEv != null) {
				actionSortie();
				actionSurvol();
			}
			actuPanCurrentPiece();
			requestFocus();
		}
	}

	/**
	 * Gère la rotation et le miroir d'une pièce avec les touches
	 */
	class GameKey implements KeyListener {
		public void keyTyped(KeyEvent keyEvent) {}

		@Override
		public void keyPressed(KeyEvent keyEvent) {
			if (pieceSelected != null) {
				char eve = keyEvent.getKeyChar();
				if (eve == 'r') {
					pieceSelected.rotate();
				} else if (eve == 'm') {
					pieceSelected = pieceSelected.mirror();
				}
				if (actuEv != null) {
					actionSortie();
					actionSurvol();
				}
				actuPanCurrentPiece();
			}
		}

		public void keyReleased(KeyEvent keyEvent) {}
	}
}
