package view.graphic;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.File;

public class PieceButton extends JButton {

    private Image img;
    private int num;
    private Graphics g;

    public int getNum() {
        return num;
    }

    public void setImg(Image image) {
        img = image;
    }

    public PieceButton(String link) {
        num = Integer.parseInt(link.substring(0, link.length()-2));
        setContentAreaFilled(false);
        try {
            img = ImageIO.read(new File("lib/" + link + ".png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PieceButton(int link) {
        setContentAreaFilled(false);
        try {
            img = ImageIO.read(new File("lib/" + link + ".png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void paintComponent (Graphics g) {
        this.g = g;
        super.paintComponent(g);
        g.drawImage(img, 0,0,getWidth(),getHeight(), null);
    }

    public void rotate() {
        AffineTransform t1 = AffineTransform.getTranslateInstance(0, 0);
        t1.rotate(Math.toRadians(90));
        Graphics2D g2d = (Graphics2D)g;
        g.drawImage(img, 0,0,getWidth(),getHeight(), null);
        g2d.drawImage(img, t1, null);
        repaint();
    }
}
