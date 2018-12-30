package view.graphic;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Cette classe représente le Panel du menu principal, elle gère les animations
 * et le design de celui-ci.
 * 
 */

public class MenuPanel extends JPanel {
	
	private Image img [];
	private int framesize;
	private int y;
	private int y1;
	private int ang1;
	private int ang2;
	private int xsize = 400;
	private int ysize = 70;
	private int xpos;
	private int ypos = 15;
	private boolean isStart;
	private volatile boolean stop;
	
	public MenuPanel(boolean isStartMenu) {
		isStart = isStartMenu;
		img = new Image[4];
		y = -100;
		y1 = -500;
		ang1 = 0;
		ang2 = 0;
		boolean [] t = {true, true, true, true};
		changePics(t);
		startAnim();
	}

	public boolean getStop () {
		return stop;
	}

	public void setFramesize (int size) {
		framesize = size;
	}
	
	/**
	 * Change les images des pièces qui tombent par rapport au tableau t 
	 * (change les pieces ou le boolean vaut true)
	 * @param t chaque case représente une piece
	 */
	private void changePics (boolean [] t) {
		for (int  i = 0; i < img.length; i++) {
			if (t[i])
				img[i] = getRandomPiecePic().getScaledInstance(100, 100, Image.SCALE_DEFAULT);
		}
	}
	
	/**
	 * Envoie l'image d'une pièce de taille 5 aléatoire
	 * @return image de la pièce 
	 */
	private BufferedImage getRandomPiecePic () {
		BufferedImage res = null;
		Random r = new Random();
		int numero = r.nextInt(10)+10;
		int jcoul = r.nextInt(4)+1;
		try {
			res = ImageIO.read(new File("lib/"+numero+"-"+jcoul+".png"));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}
	
	/**
	 * Peint les éléments du panel
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		AffineTransform t1 = AffineTransform.getTranslateInstance(150, y);
		AffineTransform t2 = AffineTransform.getTranslateInstance(175, y1);
		AffineTransform t3 = AffineTransform.getTranslateInstance(getWidth()-250, y);
		AffineTransform t4 = AffineTransform.getTranslateInstance(getWidth()-125, y1);
		t1.rotate(Math.toRadians(ang1));
		t2.rotate(Math.toRadians(ang2));
		t3.rotate(Math.toRadians(ang2));
		t4.rotate(Math.toRadians(ang1));
		Graphics2D g2d = (Graphics2D)g;
		g.drawImage((new ImageIcon("lib/menu.jpg")).getImage(), 0, 0, getWidth(), getHeight(), null);
		g2d.drawImage(img[0], t1, null);
		g2d.drawImage(img[1], t2, null);
		g2d.drawImage(img[2], t3, null);
		g2d.drawImage(img[3], t4, null);
		if (isStart)
			g.drawImage((new ImageIcon("lib/Title.png")).getImage(), (framesize/2 - xsize/2 + xpos), ypos, xsize, ysize, null);
	}
	
	/** 
	 * Démarre l'animation de chute des pièces
	 */
	private void startAnim() {
		new Thread (() -> {
			while (!stop) {
					try {
						Thread.sleep(8);
						repaint();
						Toolkit.getDefaultToolkit().sync();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
					y++;
					y1++;
					if (y % 10 == 0) {
						ang1++;
						ang2--;
					}
					if (y > getHeight() + 200) {
						boolean[] t = {true, false, true, false};
						changePics(t);
						y = -100;
					}
					if (y1 > getHeight() + 200) {
						boolean[] t = {false, true, false, true};
						changePics(t);
						y1 = -100;
					}
			}
		}).start();
		if (isStart) {
			new Thread(() -> {
				xpos = (int) (getSize().getWidth()) / 2;
				while (!stop) {
					try {
						for (int i = 0; i < 10; i++) {
							xpos++;
							if (i % 3 == 0)
								ypos--;
							repaint();
							Toolkit.getDefaultToolkit().sync();
							if (i > 6 && i < 10)
								Thread.sleep(150);
							else
								Thread.sleep(50);
						}
						for (int i = 0; i < 4; i++) {
							ypos++;
							repaint();
							Toolkit.getDefaultToolkit().sync();
							Thread.sleep(100);
						}
						for (int i = 0; i < 10; i++) {
							xpos--;
							if (i % 3 == 0)
								ypos--;
							if (i > 6 && i < 10)
								Thread.sleep(100);
							else
								Thread.sleep(50);
							repaint();
							Toolkit.getDefaultToolkit().sync();
							Thread.sleep(50);
						}
						for (int i = 0; i < 4; i++) {
							ypos++;
							repaint();
							Toolkit.getDefaultToolkit().sync();
							Thread.sleep(100);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}
	
	/**
	 * Stoppe l'animation de chute des pièces
	 */
	public void stopAnim() {
		stop = true;
	}
	
}