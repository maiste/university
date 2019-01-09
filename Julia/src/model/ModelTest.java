package model;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.function.Function;

/**
 * Classe de Test du model
 *
 * @author marais
 */
final class ModelTest {

    private static void testComplexe() {
        System.out.println("== COMPLEXE ==");
        Complex c = new Complex(1.0, 2.0);
        System.out.println("Generate Complex -> " + c);
        Complex cp = new Complex(c);
        System.out.println("Copy -> " + cp);
        System.out.println("Addition c + cp = 2.0+4.0i => " + cp.plus(c) + " = " + c.plus(cp));
        System.out.println("Multiplication * cp = -3+4i => " + cp.times(c) + " = " + c.times(cp));
        System.out.println("Pow c^3 = -11-2i => " + c.pow(3));
        System.out.println("Soustraction c - cp = 0 => " + cp.minus(c) + " = " + c.minus(cp));
        System.out.println("Module |c| = 2.236 => " + c.module());
    }

    private static void testPolynomial() {
        System.out.println("\n== LAMBDA FACTORY ==");
        double[] coeff = {0.0, 2.0, 1.0};
        Function<Complex, Complex> f = PolynomialFactory.makeFunction(coeff, new Complex(-1.0, 2.4));
        System.out.println("Factory result test : 13-5.6i =>" + f.apply(new Complex(3.0, -1.0)));
    }

    private static void testCalculus() {
        System.out.println("\n== COLOR ==");
        System.out.println("Color from int : " + Color.rgb(100, 45, 245).hashCode() + " => "
                + Calculus.intToColor(10).hashCode());

        // Julia
        double[] coeff = {0.0, 0.0, 1.0};
        Function<Complex, Complex> f = PolynomialFactory.makeFunction(coeff, new Complex(0.285, 0.01));
        Calculus c1 = new Julia(f, 300, 2.0);
        System.out.println("Calculus 1 : " + c1.getIteration() + " / " + c1.getLimite());
        c1.start();
        Color c = c1.getColorFrom(new Complex(0.6008708272859217, -0.6589259796806967));
        c1.stop();
        System.out.println("Color 1 =  " + Calculus.intToColor(136).hashCode() + " => " + c.hashCode());

        // Mandelbrot
        Calculus c2 = new Mandelbrot(300);
        System.out.println("Calculus 2 : " + c2.getIteration() + " / " + c2.getLimite());
        c2.start();
        Color c_m = c2.getColorFrom(new Complex(0.6008708272859217, -0.6589259796806967));
        c1.stop();
        System.out.println("Color 2 =  " + Calculus.intToColor(3).hashCode() + " => " + c_m.hashCode());
    }

    private static void testGrid() {
        System.out.println("\n== GRID ==");
        System.out.println("= Builder =");
        double[] coeff = {0.0, 0.0, 1.0};
        Grid g = Grid.builder()
                .function(coeff, 0.285, 0.01)
                .size(600, 600)
                .iteration(300)
                .build();
        System.out.println("DONE");
        System.out.println("== Timer ==");
        System.out.println("Temps rendu d'image en multithreads : ");
        long beg = System.currentTimeMillis();
        Image r = g.renderSceneMultiThreads();
        long end = System.currentTimeMillis();
        System.out.println("|-Temps sur " + Runtime.getRuntime().availableProcessors() + " processeurs -> " + (end - beg) + "ms");

        System.out.println("Temps rendu d'image en multithreads zoom: ");
        beg = System.currentTimeMillis();
        g.zoomGrid(40);
        r = g.renderSceneMultiThreads();
        end = System.currentTimeMillis();
        System.out.println("|-Temps sur " + Runtime.getRuntime().availableProcessors() + " processeurs -> " + (end - beg) + "ms");

        System.out.println(" Temps rendu d'image en multithreads origine : ");
        beg = System.currentTimeMillis();
        g.moveOrigin(10, 10);
        g.renderSceneMultiThreads();
        end = System.currentTimeMillis();
        System.out.println("|-Temps sur " + Runtime.getRuntime().availableProcessors() + " processeurs -> " + (end - beg) + "ms");

    }


    public static void main(String[] args) {
        testComplexe();
        testPolynomial();
        testCalculus();
        testGrid();
    }

}
