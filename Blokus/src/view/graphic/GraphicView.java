package view.graphic;

import controller.Control;
import controller.Intel;
import data.Bdd;
import model.Blokus;
import model.players.Player;
import view.GameView;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.SQLException;
import java.util.LinkedList;

/**
 * Cette classe représente la fenetre de l'interface graphique, Elle est utilisé pour crée la fenetre
 * mais aussi TOUT les panels et effectués les changement de panel
 */

public class GraphicView extends JFrame implements GameView {
	
	private static final long serialVersionUID = 6612515691877278634L;

	private Bdd bdd;
	private Control control;
	private MenuPanel menu;
	private MenuPanel endMenu;
	private BoardPanel board;
	private Audio fond;
	private JPanel sousMenu;
	private boolean withTeam;
	private Intel [] players = {new Intel("joueur 1",0),
 new Intel("joueur 2",0),
 new Intel("joueur 3",0),
 new Intel("joueur 4",0)
};
private int gbcy;
private boolean isPush = false;

public GraphicView (Control c) {
  control = c;
  bdd = new Bdd();
  setTitle("Blokus"); 
  setSize(1000,700);
  setMinimumSize(new Dimension(900, 625));
  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  setLayout(new GridBagLayout());
  initMenu();
  menu.setFramesize(getWidth());
  setVisible(true);
}

public Control getControl () {
  return control;
}

	/**
	 * Change le contentPane de la fenetre
	 * @param p nouveau contentPane
	 */
	private void changeContentPane (JPanel p) {
		setContentPane(p);
		revalidate();
		repaint();
	}

/* ------------------
 -
 - MENU PRINCIPAL
 -
 -------------------*/

	/**
	 * Crée le menu principal d'arrivé dans le jeu
	 */
	public void initMenu () {
		// menu général
		menu = new MenuPanel(true);
		menu.setFramesize(getWidth());
		menu.setLayout(new GridBagLayout());
		addComponentListener(new FrameListen());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = GridBagConstraints.RELATIVE;
		gbc.gridheight = GridBagConstraints.REMAINDER;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.ipady = 40;
		gbc.insets = new Insets(80,0,0,0);
		// musiques
		if (fond != null)
			fond.stop();
		fond = new Audio("lib/menu.wav");
		fond.loop();
		// sous menu affichant les options (fond marron)
		sousMenu = new JPanel() {
			private static final long serialVersionUID = 5923721123697736865L;

			public void paintComponent(Graphics g) {
				g.setColor(new Color(191,159,86));
				g.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
			}
		};
		sousMenu.setLayout(new GridBagLayout());
        menu.add(sousMenu,gbc);
		// bouton scores
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridx = 1;
        gbc.insets = new Insets(80,10,0,0);
        JButton score = new JButton() {
          public void paintComponent(Graphics g) {
              g.drawImage(new ImageIcon("lib/leaderboard.png").getImage(),0,0,getWidth(),getHeight(),null);
          }
      };
      score.setPreferredSize(new Dimension(75,35));
      score.setBorderPainted(false);
      score.addActionListener(e -> {
          new Thread(() -> {
              if (!isPush) {
                  isPush = true;
                  initPrintScorePanel();
                  revalidate();
                  isPush = false;
              }
          }).start();
      });
		// ajouts
      initNbIA();
      addWithGBC(initChoiceTeam(), sousMenu, 0, 15);
      addWithGBC(initLaunchGameButton(), sousMenu, 10, 0);
      menu.add(score, gbc);
      changeContentPane(menu);
  }

	/**
	 * Initialise le panel contenant le choix des joueurs
	 */
	private void initNbIA () {
	    gbcy = 0;
	    JPanel [] colorPlayers = new JPanel[4];
		JLabel [] labPlayers = new JLabel [4];
		for (int i = 0; i < players.length; i++) {
		    colorPlayers[i] = new JPanel();
		    colorPlayers[i].setLayout(new GridBagLayout());
		    colorPlayers[i].setOpaque(false);
		    GridBagConstraints gbc = new GridBagConstraints();
		    JLabel color = new JLabel();
		    color.setOpaque(true);
            color.setBackground(Blokus.colors[i]);
            color.setPreferredSize(new Dimension(15,15));
            colorPlayers[i].add(color, gbc);
            labPlayers[i] = new JLabel(players[i].getName() + " :");
        }
		JComboBox<String> [] types = new JComboBox[4];
		for (int i = 0; i < types.length; i++) {
			types[i] = new JComboBox<>();
			types[i].setFocusable(false);
			types[i].setBackground(new Color(56,147,240));
			types[i].setForeground(Color.WHITE);
			types[i].setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
			types[i].setName("" + i);
			types[i].addItem("Real player");
			types[i].addItem("IA easy");
			types[i].addItem("IA medium");
			types[i].addItem("IA hard");
			types[i].addItem("Machine Learning");
            types[i].setSelectedItem(types[i].getItemAt(players[i].getLevel()));
			types[i].addActionListener(e -> {
				JComboBox<String> source = ((JComboBox)e.getSource());
				String item = source.getSelectedItem().toString().toUpperCase();
				int numPlayer = Integer.parseInt(source.getName());
				if (item.equals("REAL PLAYER"))
					players[numPlayer].setLevel(0);
				else if (item.equals("IA EASY"))
					players[numPlayer].setLevel(1);
				else if (item.equals("IA MEDIUM"))
					players[numPlayer].setLevel(2);
				else if (item.equals("IA HARD"))
					players[numPlayer].setLevel(3);
				else if (item.equals("MACHINE LEARNING"))
				    players[numPlayer].setLevel(4);
			});
		}
		for (int i = 0; i < types.length; i++) {
		    JPanel contPlay = new JPanel();
		    contPlay.setOpaque(false);
		    contPlay.setPreferredSize(new Dimension(300,25));
		    contPlay.setLayout(new GridLayout(1,3,10,0));
			JButton changeName = getChangeName(labPlayers,i);
			contPlay.add(colorPlayers[i]);
			contPlay.add(labPlayers[i]);
			contPlay.add(changeName);
            addWithGBC(contPlay, sousMenu,0, 5);
            addWithGBC(types[i], sousMenu, 0, 15);
		}
	}

    /**
     * Renvoie une nouvelle instance du bouton pour changer le nom d'un joueur
     * @return instance du bouton changeName
     */
    private JButton getChangeName (JLabel [] labPlayers, int i) {
        JButton changeName = new JButton(){
            @Override
            public void paintComponent(Graphics g) {
                BufferedImage icon = null;
                try {
                    icon = ImageIO.read(new File("lib/changeNameBlanc.png"));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                g.drawImage(icon, 0, 0, getWidth(), getHeight(), null);
            }
        };
        changeName.setName("" + i);
        changeName.setBorderPainted(false);
        changeName.setFocusPainted(false);
        changeName.addActionListener(e -> {
            int num = Integer.parseInt(((JButton)(e.getSource())).getName());
            String nom = JOptionPane.showInputDialog(this, "Veuillez entrer le nom pour le joueur " + (num+1),
                "Selection nom", JOptionPane.QUESTION_MESSAGE);
            if (nom != null) {
                players[num].setName(nom, num);
                labPlayers[num].setText(players[num].getName() + " :");
            }
        });
        return changeName;
    }

    /**
     * initialise le panel contenant le choix de team
     * @return
     */
    private JPanel initChoiceTeam() {
        JPanel type = new JPanel();
        type.setOpaque(false);
        type.setLayout(new GridLayout(2, 1));
        withTeam = false;
        JLabel gameType = new JLabel("  Type de partie :");
        JComboBox<String> gameMode = new JComboBox<>();
        gameMode.setBackground(new Color(56, 147, 240));
        gameMode.setFocusable(false);
        gameMode.setForeground(Color.WHITE);
        gameMode.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        gameMode.addItem("J1 vs J2 vs J3 vs J4");
        gameMode.addItem("J1 & J3 vs J2 & J4");
        gameMode.addActionListener(e -> {
            JComboBox<String> source = ((JComboBox) e.getSource());
            String item = source.getSelectedItem().toString().toUpperCase();
            withTeam = (item.equals("J1 & J3 VS J2 & J4"));
        });
        type.add(gameType);
        type.add(gameMode);
        return type;
    }

    /**
     * initialise le boutton de lancement d'une partie
     * @return boutton pour lancer une partie
     */
    private JButton initLaunchGameButton() {
        JButton go = new JButton();
        try {
            BufferedImage icon = ImageIO.read(new File("lib/play.png"));
            go.setIcon(new ImageIcon(icon));
        } catch (Exception e) {
            e.printStackTrace();
        }
        go.setBorder(null);
        go.setContentAreaFilled(false);
        go.addActionListener(e -> {
            menu.stopAnim();
            fond.stop();
            new Audio("lib/validate.wav").play();
            fond = new Audio("lib/game.wav");
            fond.loop();
            control.setGame(new Blokus(players, withTeam));
            board = new BoardPanel(this);
            changeContentPane(board);
        });
        return go;
    }

    /**
     * Ajoute un composant au container en lui fixant des marges, les composants sont ajoutés
     * les uns en dessous des autres (1 seule colonne pour x lignes)
     * @param comp composant à ajouter
     * @param container container dans lequel ajouter comp
     * @param marginTop marge supérieur
     * @param marginBottom marge inférieur
     */
    private void addWithGBC (JComponent comp, JComponent container, int marginTop, int marginBottom) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridheight = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridx = 0;
        gbc.gridy = gbcy;
        gbc.insets = new Insets(marginTop, 20, marginBottom, 20);
        gbc.fill = GridBagConstraints.BOTH ;
        container.add(comp,gbc);
        gbcy++;
    }

	/* ------------------
	 -
	 - MENU FIN PARTIE
	 -
	 -------------------*/

	/**
	 * initialise le menu de fin de partie 
	 */
	public void initEndMenu () {
       gbcy = 0;
       endMenu = new MenuPanel(false);
       endMenu.setLayout(new GridBagLayout());
       JPanel contEndMenu = new JPanel(){
        public void paintComponent(Graphics g) {
            g.setColor(new Color(191,159,86));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
        }
    };
    contEndMenu.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridwidth = gbc.gridheight = GridBagConstraints.REMAINDER;
    gbc.weighty = gbc.weightx = 1;
    gbc.gridx = gbc.gridy = 0;
    gbc.ipady = 40;
    gbc.fill = GridBagConstraints.VERTICAL;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.insets = new Insets(100,0,100,0);
    endMenu.add(contEndMenu,gbc);
		// musiques :
    fond.stop();
    fond = new Audio("lib/end.wav");
    fond.loop();
        // Ajout des éléments :
    addWithGBC(initScorePanel(), contEndMenu, 0, 15);
    addWithGBC(initQuitButton(true), contEndMenu, 0, 15);
    changeContentPane(endMenu);
    new Thread(() -> {
        initRegisterToBdd();
    }).start();
}

    /**
     * initialise le panel contenant le score
     * @return panel du score
     */
    private JPanel initScorePanel() {
       JPanel contScore = new JPanel();
       contScore.setOpaque(false);
       contScore.setLayout(new GridBagLayout());
       contScore.setBackground(new Color(56, 147, 240));
       JPanel scores = new JPanel();
       scores.setLayout(new GridLayout(4,1));
       scores.setOpaque(false);
        // contraintes
       GridBagConstraints gbc = new GridBagConstraints();
       gbc.insets = new Insets(0,10,0,10);
       gbc.gridx = gbc.gridy = 0;
       gbc.gridheight =  gbc.gridwidth = GridBagConstraints.REMAINDER;
        // éléments :
       Player[] tab = control.getClassement();
       JLabel[] classPlay = new JLabel[4];
       if (withTeam)
        classPlay = new JLabel[2];
    for (int i = 0; i < classPlay.length; i++) {
        classPlay[i] = new JLabel();
        classPlay[i].setForeground(Color.WHITE);
        if (!withTeam)
            classPlay[i].setText(tab[i] + " a eu " + tab[i].getScore());
        scores.add(classPlay[i]);
    }
    if (withTeam) {
        classPlay[0].setText("La team 1 (J1 & J3) a eu " + tab[0].getScore());
        classPlay[1].setText("La team 2 (J2 & J4) a eu " + tab[1].getScore());
    }
    contScore.add(scores, gbc);
    return contScore;
}

    /**
     * Initialise le bouton de retour au menu principal
     * @return bouton de retour au menu principal
     */
    private JButton initQuitButton(boolean isEndMenu) {
        JButton quit = new JButton("quit");
        quit.addActionListener(e -> {
            Audio ok = new Audio("lib/validate.wav");
            ok.play();
            if (isEndMenu) {
                fond.stop();
                endMenu.stopAnim();
            }
            initMenu();
        });
        return quit;
    }


    /* ------------------
	 -
	 -   CLASSEMENTS    -
	 -
	 -------------------*/

    /**
     * Initialise l'enregistrement du score dans la BDD
     */
    private void initRegisterToBdd() {
        if (control.getGame().canBeClassed()) {
            String nom = null;
            boolean exit = false;
            while (true) {
                nom = (String)JOptionPane.showInputDialog(this, "Veuillez entrer un nom pour " +
                    "enregistrer votre score ", "Selection nom", JOptionPane.QUESTION_MESSAGE, null,
                    null, control.getGame().getRealPlayerName());
                if (nom == null) {
                    exit = true;
                    break;
                } else { // Correctif pour éviter l'erreur
                    break;
                }
            }
            if (!exit) {
                boolean bddOk = false;
                boolean retry = true;
                while (retry) {
                    try {
                        bdd.connectToBDD();
                        bdd.updateScore(nom, control.getGame().getRealPlayerScore());
                        bddOk = true;
                        retry = false;
                    } catch (SQLException e) {
                        e.printStackTrace();
                        retry = wantRetryConnect();
                    }
                }
                if (bddOk) {
                    bdd.updateScore(nom, control.getGame().getRealPlayerScore());
                    JOptionPane.showMessageDialog(this, "Score mis à jour avec succès", "Information",
                            JOptionPane.INFORMATION_MESSAGE);
                }
                bdd.closeConnection();
            }
        }
    }

    /**
     * Appelé en cas d'echec de connexion a la BDD, demande à l'utilisateur si il veut reesayer
     * @return true si l'utilisateur veut ré-essayer la connexion, false sinon
     */
    private boolean wantRetryConnect() {
        int choix = JOptionPane.showConfirmDialog(this, "Connexion à la BDD " +
            "impossible, verifiez votre connexion internet, voulez vous " +
            "retenter la connexion ?", "Connexion BDD impossible", JOptionPane.OK_CANCEL_OPTION);
        if (choix == JOptionPane.NO_OPTION || choix == JOptionPane.CANCEL_OPTION) {
            JOptionPane.showMessageDialog(this, "Abandon de la connexion à la BDD", "Information", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Initialise le Panel pour afficher les scores des joueurs (BDD)
     */
    private void initPrintScorePanel () {
        boolean retry = true;
        boolean connOk = false;
        while (retry) {
            try {
                bdd.connectToBDD();
                connOk = true;
                retry = false;
            } catch (SQLException e) {
                e.printStackTrace();
                retry = wantRetryConnect();
            }
        }
        if (connOk) {
            MenuPanel pan = new MenuPanel(false);
            pan.setLayout(new GridBagLayout());
            JTable scores = initListPanel(10);
            if (scores != null) {
                JPanel sp = new JPanel();
                sp.add(scores);
                sp.setOpaque(false);
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = gbc.gridy = 0;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.gridheight = GridBagConstraints.RELATIVE;
                sp.setMinimumSize(new Dimension(400,400));
                pan.add(sp,gbc);
                gbc.gridy = 1;
                gbc.gridheight = GridBagConstraints.REMAINDER;
                pan.add(initQuitButton(false), gbc);
                setContentPane(pan);
            }
        }
        bdd.closeConnection();
    }

    /**
     * init JTable contenant les scores a l'interieur du panel global de score
     * @param limit nombre de scores à afficher
     * @return la JTable contenant tout les scores
     */
    private JTable initListPanel (int limit) {
        String [] titles = {"Nom", "Score"};
        boolean connOk = false;
        boolean retry = true;
        LinkedList<String> data = null;
        while (retry) {
            try {
                bdd.connectToBDD();
                data = bdd.printAllScores(limit);
                connOk = true;
                retry = false;
            } catch (SQLException e) {
                e.printStackTrace();
                retry = wantRetryConnect();
            }
        }
        if (connOk) {
            Object[][] res = new Object[data.size() / 2][2];
            for (int i = 0; i < data.size(); i++) {
                res[i/2][i%2] = data.get(i);
            }
            return new JTable(res, titles);
        }
        bdd.closeConnection();
        return null;
    }

    class FrameListen implements ComponentListener {

      public void componentHidden(ComponentEvent arg0) {}
      public void componentMoved(ComponentEvent arg0) {}
      public void componentShown(ComponentEvent arg0) {}

      public void componentResized(ComponentEvent e) {
          menu.setFramesize(getWidth());
      }
  }

  public void printBoard() {}
  public void play() {}
}
