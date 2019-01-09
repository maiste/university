package view;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import model.Grid;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * ImageSaver est une classe permettant de sauvegarder une image
 * au format png
 *
 * @author marais bello
 */
public final class ImageSaver {

    /**
     * Convertit une chaine en int
     *
     * @param s la chaine à convertir
     * @return l'int
     */
    private static int convertInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Convertit une chaine en double
     *
     * @param s la chaine à convertir
     * @return le double
     */
    private static double convertDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Fait le rendu de l'imahe
     *
     * @param build le builder de base
     * @param coord les différents double de références
     * @param type  le type julia / mandelbrot
     * @param name  le nom du fichier
     */
    private static void renderImage(Grid.GridBuilder build, double[] coord, String type, String name) {
        if (type.equals("julia")) {
            double[] coeff = {0.0, 0.0, 1.0};
            build.function(coeff, coord[3], coord[4]);
        } else if (type.equals("mandelbrot"))
            build.mandelbrot();
        else {
            System.out.println("Erreur de type : julia ou mandelbrot");
            return;
        }
        Grid g = build.build();
        g.imageZoom(coord[2]);
        g.moveOrigin(coord[0], coord[1]);
        saveImage(g.renderSceneMultiThreads(), name);
    }

    /**
     * Génère une image grâce aux arguments
     *
     * @param args les arguments
     */
    public static void generateImageFromArgs(String args[]) {
        int x = convertInt(args[1]), y = convertInt(args[2]), iteration = convertInt(args[6]);
        double[] coord;
        if (args[0].equals("julia")) {
            coord = new double[5];
            coord[0] = convertDouble(args[3]);
            coord[1] = convertDouble(args[4]);
            coord[2] = convertDouble(args[5]);
            coord[3] = convertDouble(args[8]);
            coord[4] = convertDouble(args[9]);
        } else {
            coord = new double[3];
            coord[0] = convertDouble(args[3]);
            coord[1] = convertDouble(args[4]);
            coord[2] = convertDouble(args[5]);
        }
        if (iteration == -1 || x == -1 || y == -1 || coord[2] == 0) {
            System.out.println("Erreur args");
            return;
        }
        Grid.GridBuilder build = Grid.builder().iteration(iteration).size(x, y);
        renderImage(build, coord, args[0], args[7]);
    }

    /**
     * sauvegarde l'image img dans le répertoire "Projet_Julia/save/"
     *
     * @param img  fichier image
     * @param name nom de l'image
     */
    public static void saveImage(Image img, String name) {
        try {
            File out = new File("save/" + name + ".png");
            out.mkdirs();
            BufferedImage buff = SwingFXUtils.fromFXImage(img, null);
            ImageIO.write(buff, "png", out);
            System.out.println("Fichier sauvegarder dans : " + out.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
