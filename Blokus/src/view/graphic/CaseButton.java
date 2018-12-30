package view.graphic;

import model.players.Player;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class CaseButton extends JButton {

	private final int x;
	private final int y;
	private Player[][] board;
	
	public CaseButton (int y, int x, Player[][] b) {
		board = b;
		this.x = x;
		this.y = y;
	}

	public int getCoorX() {
		return x;
	}

	public int getCoorY() {
		return y;
	}

	// actualise l'image en fonction du numéro du joueur sur la case du model si isEmpty = false
	public void paintComponent (Graphics g) {
	    super.paintComponent(g);
        if (board[y][x] != null) {
            try {
                Image img = ImageIO.read(new File("lib/" + board[y][x].getId() + ".png"));
                g.drawImage(img, 0,0,getWidth(),getHeight(), null);
                g.drawImage(img, 0, 0, getWidth(), getHeight(), null);
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}
