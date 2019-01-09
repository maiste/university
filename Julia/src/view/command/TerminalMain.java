package view.command;

import javafx.scene.image.Image;
import model.Grid;
import view.ImageSaver;

import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;


/**
 * TerminalMain est la classe principal pour la version en lignes de commandes du programme
 * , elle permet de lancer l'exportation au format .png d'un Julia ou Mandlebrot paramétrable.
 *
 * @author marais bello
 */
public class TerminalMain {

    static Scanner sc;

    /**
     * Pose la question tant que la réponse n'est pas un double
     *
     * @param question question à posée
     * @return double résultat
     */
    private static double askDouble(String question) {
        double res;
        sc = new Scanner(System.in);
        do {
            System.out.println(question);
            if (sc.hasNextDouble()) break;
            else sc.next();
        }
        while (true);
        res = sc.nextDouble();
        return res;
    }

    /**
     * Traite le menu principal
     */
    public static void menu() {
        System.out.println("Bonjour.");
        String q = "Voulez vous télécharger un ensemble de Julia ou de Mandelbrot ?\n" +
                "Entrez 1 pour Julia, 2 pour Mandelbrot";
        int res = (int) askDouble(q);
        while (res != 1 && res != 2)
            res = (int) askDouble(q);
        if (res == 1) julia();
        else mandelbrot();
    }

    /**
     * Traite l'exportation d'un julia
     */
    private static void julia() {
        int haut = (int) askDouble("Veuillez entrer la hauteur en pixels svp.");
        int larg = (int) askDouble("Veuillez entrer la largeur en pixels svp.");
        String q = "Voulez vous le mode de calcul infini ?\n" +
                "1 oui, 0 non";
        boolean inf = ((int) askDouble(q) > 0);
        int iter = 0;
        if (!inf) iter = (int) askDouble("Veuillez entrer le nombre d'itérations svp.");
        double reel = askDouble("Veuillez entrer la partie réel svp.");
        double im = askDouble("Veuillez entrer la partie immaginaire svp");
        List<Double> polys = new LinkedList<>();
        polys.add(0.);
        int num = 0;
        while (true) {
            q = "Voulez vous entrer une valeur pour x^" + (num + 1) + " ?\n1 oui, 0 non";
            boolean cont = (int) askDouble(q) == 1;
            if (!cont) break;
            q = "Veuillez entrer la valeur de x^" + (num + 1);
            polys.add(askDouble(q));
            num++;
        }
        double[] polys2 = new double[polys.size() + 1];
        for (int i = 0; i < polys.size(); i++) polys2[i + 1] = polys.get(i);
        Grid grid;
        Grid.GridBuilder gb = (inf) ?
                (Grid.builder().infinity()) : (Grid.builder().iteration(iter));
        grid = gb
                .size(larg, haut)
                .function(polys2, reel, im)
                .build(); // Build l'objet
        Image image = grid.renderSceneMultiThreads();
        Scanner sc = new Scanner(System.in);
        System.out.println("Veuillez enter un nom de fichier svp.");
        String name = sc.next();
        System.out.println("Exportation du fichier .png, veuillez patienter.");
        ImageSaver.saveImage(image, name);
        end();
    }

    /**
     * Traite l'exportation d'un mandelbrot
     */
    private static void mandelbrot() {
        int haut = (int) askDouble("Veuillez entrer la hauteur en pixels svp.");
        int larg = (int) askDouble("Veuillez entrer la largeur en pixels svp.");
        int iter = (int) askDouble("Veuillez entrer le nombre d'itérations svp.");
        Grid grid = Grid.builder()
                .iteration(iter)
                .size(larg, haut)
                .mandelbrot()
                .build();
        System.out.println("Calcul en cours, veuillez patienter.");
        Image im = grid.renderSceneMultiThreads();
        Scanner sc = new Scanner(System.in);
        System.out.println("Veuillez enter un nom de fichier svp.");
        String name = sc.next();
        System.out.println("Exportation du fichier .png, veuillez patienter.");
        ImageSaver.saveImage(im, name);
        end();
    }

    /**
     * Traite la fin du programme
     */
    private static void end() {
        System.out.println("Fin du programme.");
        System.exit(0);
    }

    public static void main(String[] args) {
        menu();
    }
}
