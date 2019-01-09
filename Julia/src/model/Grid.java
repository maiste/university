package model;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

/**
 * Classe de gestion de la grille de calcul de Julia
 *
 * @author marais bello
 */
public final class Grid {

    private final double sizeX; // Taille de l'axe X
    private final double sizeY; // Taille de l'axe Y
    private final Calculus colorUnit; // Unité de calcul
    private final Image render; // Image
    private Complex origin; // Origin du repère
    private double zoom; // Zoom
    private boolean run, infinity;
    /**
     * Constructeur
     *
     * @param g le builder de Grid
     */
    private Grid(GridBuilder g) {
        this.sizeX = g.correctSizeX();
        this.sizeY = g.correctSizeY();
        this.origin = new Complex(0, 0);
        this.zoom = 1;
        this.render = new WritableImage(g.x, g.y);
        this.colorUnit = g.unit;
        this.run = false;
        this.infinity = g.infinity;
        initImage();
    }

    /**
     * Créer un builder de Grid
     *
     * @return un GridBuilder
     */
    public static GridBuilder builder() {
        return new GridBuilder();
    }

    /**
     * Renvoie la valeur du zoom
     *
     * @return le zoom
     */
    public double getZoom() {
        return this.zoom;
    }

    /**
     * Renvoie l'état de la grille
     *
     * @return true si en calcul d'image
     */
    public boolean isRunning() {
        return run;
    }

    /**
     * Calcule les coordonées de X dans le plan complexe
     *
     * @param j la coordonées de x dans la grille
     * @return la coordonée de x dans le plan complexe
     */
    private double calculateXCoordinate(double j) {
        double xs = j - (render.getWidth() / 2);
        return (xs * sizeX) / (render.getWidth() * zoom);
    }

    /**
     * Calcule les coordonées de y dans le plan complexe
     *
     * @param i la coordonées de y dans la grille
     * @return la coordonée de y dans le plan complexe
     */
    private double calculateYCoordinate(double i) {
        double ys = (render.getHeight() / 2) - i;
        return (ys * sizeY) / (render.getHeight() * zoom);
    }

    /**
     * Initialise l'image avec des pixels noirs
     */
    private void initImage() {
        PixelWriter writer = ((WritableImage) render).getPixelWriter();
        for (int i = 0; i < render.getHeight(); i++) {
            for (int j = 0; j < render.getWidth(); j++) {
                writer.setColor(j, i, Color.BLACK);
            }
        }
    }

    /**
     * Modifie le zoom du plan complexe
     *
     * @param z le zoom (en %)
     */
    public void zoomGrid(double z) {
        if (this.zoom + (this.zoom * 10 / z) > 0) {
            this.zoom += this.zoom * 10 / z;
        }
    }

    /**
     * Modifie le zoom pour le rendu d'image
     *
     * @param z le zoom en pourcentage
     */
    public void imageZoom(double z) {
        if (z >= 0)
            this.zoom = 1;
        this.zoom = z / 100;
    }

    /**
     * Déplace l'origine du plan complexe
     *
     * @param x le décalage en abscisse dans l'image
     * @param y le décalage en ordonnée dans l'image
     */
    public void moveOrigin(double x, double y) {
        this.origin = new Complex(
                origin.getReal() + (x / zoom),
                origin.getImg() + (y / zoom)
        );
    }

    /**
     * Arrête le calcul du rendu de l'image
     */
    public void stop() {
        this.run = false;
        this.colorUnit.stop();
    }

    /**
     * Créer la liste des Threads à lancer dans la pool
     *
     * @return la liste des tâches
     */
    private List<ForkJoinTask> createTask(int nbProcessors) {
        List<ForkJoinTask> tasksList = new LinkedList<>();
        ForkJoinPool pool = new ForkJoinPool(nbProcessors);
        for (int i = 0; i < nbProcessors; i++) {
            tasksList.add(
                    pool.submit(
                            new GridThread(
                                    i * ((int) render.getHeight()) / nbProcessors,
                                    (i + 1) * ((int) render.getHeight()) / nbProcessors
                            )
                    )
            );
        }
        return tasksList;
    }

    /**
     * Version de rendu multithreads
     *
     * @return la grille de pixels
     */
    public Image renderSceneMultiThreads() {
        if (this.run)
            return render;
        this.run = true;
        colorUnit.start();
        for (ForkJoinTask task : createTask(Runtime.getRuntime().availableProcessors() - 1)) {
            try {
                task.join();
            } catch (Exception e) {
                System.out.println("Error with multithreading");
            }
        }
        this.run = false;
        colorUnit.stop();
        return render;
    }

    /**
     * Classe interne, builder de Grid
     */
    public static final class GridBuilder {
        private int x = 0;
        private int y = 0;
        private Calculus unit = new Julia((xn) -> new Complex(0, 0).plus(xn.times(xn)), 100, 2.0);
        private boolean mandelbrot = false;
        private boolean infinity = false;

        /**
         * Constructeur privé
         */
        private GridBuilder() {
        }

        /**
         * Adapte la taille de x
         *
         * @return la taille de x proportionnelle à y dans le plan complexe 4,4
         */
        private double correctSizeX() {
            if (x == 0 || y == 0)
                x = y = 600;
            if (x <= y)
                return 4.0;
            else
                return 4.0 * ((double) x / (double) y);
        }

        /**
         * Adapte la taille de y
         *
         * @return la taille de y proportionnelle à x dans le plan complexe 4,4
         */
        private double correctSizeY() {
            if (x == 0 || y == 0)
                x = y = 600;
            if (y <= x)
                return 4.0;
            else
                return 4.0 * ((double) y / (double) x);
        }

        /**
         * Définit le nombre d'itérations de l'unité de calculs
         *
         * @param iteration le nouveau nombre d'itérations
         * @return this
         */
        public GridBuilder iteration(int iteration) {
            if (iteration > 0) {
                if (mandelbrot) {
                    unit = new Mandelbrot(iteration);
                } else {
                    unit = new Julia(unit.getFunction(), iteration, unit.getLimite());
                }
            }
            return this;
        }

        /**
         * Définit la fonction quadratique de l'unité de calculs
         *
         * @param coeffs les coefficients du polynome
         * @param real   la partie de la fonction quadratique
         * @param img    la partie imaginaire de la fonction quadratique
         * @return this
         */
        public GridBuilder function(double[] coeffs, double real, double img) {
            mandelbrot = false;
            unit = new Julia(
                    PolynomialFactory.makeFunction(coeffs, new Complex(real, img)),
                    unit.getIteration(),
                    unit.getLimite());
            return this;
        }

        /**
         * Définit une unité de calcul de Mandelbrot
         *
         * @return this
         */
        public GridBuilder mandelbrot() {
            mandelbrot = true;
            unit = new Mandelbrot(unit.getIteration());
            return this;
        }

        /**
         * Définit la taille de la grille en fonction de la taille de la fênetre
         *
         * @param x la taille en x
         * @param y la taille en y
         * @return this
         */
        public GridBuilder size(int x, int y) {
            if (x != 0 && y != 0) {
                this.x = x;
                this.y = y;
            }
            return this;
        }

        /**
         * Initialise le mode de calcul infini
         *
         * @return this
         */
        public GridBuilder infinity() {
            this.infinity = true;
            return this;
        }

        /**
         * Calcule l'objet Grid associé
         *
         * @return un objet Grid
         */
        public Grid build() {
            return new Grid(this);
        }

    }

    /**
     * Classe de gestion des taches de calculs multithreads
     */
    private class GridThread extends RecursiveAction {
        int begin, end;

        private GridThread(int begin, int end) {
            this.begin = begin;
            this.end = end;
        }

        private void writeColor(PixelWriter writer, int j, int i, Color c) {
            synchronized (Grid.this) {
                writer.setColor(j, i, c);
            }
        }

        @Override
        protected void compute() {
            PixelWriter writer = ((WritableImage) render).getPixelWriter();
            for (int i = begin; Grid.this.run && i < end; i++) {
                for (int j = 0; Grid.this.run && j < render.getWidth(); j++) {
                    Complex tmp = new Complex(calculateXCoordinate(j), calculateYCoordinate(i))
                            .plus(origin);
                    Color c = (infinity) ? colorUnit.getInfiniteColorFrom(tmp) : colorUnit.getColorFrom(tmp);
                    writeColor(writer, j, i, c);
                }
            }
        }
    }

}