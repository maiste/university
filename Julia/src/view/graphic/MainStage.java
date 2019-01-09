package view.graphic;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import view.ImageSaver;
import view.command.TerminalMain;

import java.io.InputStream;


/**
 * MainStage représente la scène principale, elle organise les différents
 * panels de la fenêtre et gère les dimensions de ceux-ci
 *
 * @author marais bello
 */
public final class MainStage extends Application {

    public final static double windowHeight = Screen.getPrimary().getVisualBounds().getHeight() * 0.93;
    /*
     * Dimensions de la fenêtre et des des panels principaux
     */
    private final static double windowWidth = Screen.getPrimary().getVisualBounds().getWidth() * 0.99;
    public final static double leftWidth = windowWidth * 0.3d;

    public static void main(String[] args) {
        if (args.length != 0 && args[0].equals("interactif"))
            TerminalMain.menu();
        else if (args.length != 0 && args[0].equals("graphique"))
            launch(args);
        else if (args.length == 10 && args[0].equals("julia")
                || args.length == 8 && args[0].equals("mandelbrot"))
            ImageSaver.generateImageFromArgs(args);
        else
            System.out.println("=== MANDELBROT JULIA ===\n\n" +
                    "Cmdline \n" +
                    "|> ./fractal <julia|mandelbrot> width height origin_x origin_y zoom(%) iteration name [reel] [img]\n\n" +
                    "Interatif\n" +
                    "|> ./fractal interactif\n\n" +
                    "Graphique\n" +
                    "|> ./fractal graphique\n\n" +
                    "========================");
        System.exit(0);
    }

    @Override
    public void start(Stage primaryStage) {
        // Fenêtre
        primaryStage.setMaximized(true);
        primaryStage.setResizable(false);
        primaryStage.setTitle("Ensembles de Julia");
        // Layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(2));
        // Scène principale
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
        // Initialisation éléments fenêtre :
        initLeft(root); // menu à gauche
    }

    /**
     * Initialise la partie gauche de la fenêtre
     *
     * @param root panel principal
     */
    private void initLeft(BorderPane root) {
        // Panel principal du menu
        BorderPane left = new BorderPane();
        left.setPrefSize(leftWidth, windowHeight);
        root.setLeft(left);

        // Menu en haut
        HBox topHBox = new HBox(10);
        topHBox.setAlignment(Pos.CENTER_RIGHT);
        topHBox.setPrefSize(leftWidth, windowHeight * 0.05d);
        MenuBar menuBar = new MenuBar();
        InputStream is = getClass().getResourceAsStream("../lib/plus.png");
        ImageView addImage = new ImageView(new Image(is, (topHBox.getPrefWidth() * 0.30) / 5,
                topHBox.getPrefHeight() * 0.70, false, false));
        Menu add = new Menu(null, addImage);
        MenuItem julia = new MenuItem("Julia");
        MenuItem mandlebrot = new MenuItem("Mandlebrot");
        add.getItems().addAll(julia, mandlebrot);
        menuBar.getMenus().add(add);
        topHBox.getChildren().add(menuBar);
        left.setTop(topHBox);

        // Scroll contenant la VBox principale
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true); // desactivate Hscroll
        scroll.setPrefSize(leftWidth, windowHeight);
        left.setCenter(scroll);

        // VBox contenant les functionBox
        VBox functionsVBox = new VBox(10);
        functionsVBox.setSpacing(30);
        functionsVBox.setPadding(new Insets(0, leftWidth * 0.05, 0, leftWidth * 0.05));
        functionsVBox.setAlignment(Pos.CENTER);
        functionsVBox.setPrefSize(leftWidth, windowHeight);
        scroll.setContent(functionsVBox);

        // listener du bouton ajouté :
        julia.setOnAction(e -> new JuliaBox(root, functionsVBox));
        mandlebrot.setOnAction(e -> new MandelbrotBox(root, functionsVBox));
        new JuliaBox(root, functionsVBox, 300, "0.285", "0.01", 0.0, 1.0);
        new JuliaBox(root, functionsVBox, 300, "-0.8", "0.156", 0.0, 1.0);
        new JuliaBox(root, functionsVBox, 300, "-0.70176", "-0.3842", 0.0, 1.0);
    }
}
