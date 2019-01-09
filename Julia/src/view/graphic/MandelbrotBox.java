package view.graphic;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.Grid;


/**
 * Noeud graphique de chaque fonction de type Mandelbrot
 *
 * @author marais bello
 */
public final class MandelbrotBox extends FunctionBox {

    /**
     * Créer un MandelbrotBox non initialisé
     *
     * @param root
     * @param container
     */
    public MandelbrotBox(BorderPane root, VBox container) {
        super(root, container);
        // Paramètres de la FunctioBox :
        setPrefSize(MainStage.leftWidth, MainStage.windowHeight * 0.07d);
        container.getChildren().add(this);
        // zone paramétrage fonction
        iterations = new TextField();
        iterations.setPromptText("ITERATIONS");
        iterations.setPrefHeight(getPrefHeight());
        HBox function = new HBox();
        function.setPrefSize(getPrefWidth(), getPrefHeight());
        function.getChildren().add(iterations);
        getChildren().add(function);
        initMenu();
    }

    /**
     * Gère le traitement du bouton play
     *
     * @param event
     */
    @Override
    protected void handlePlay(ActionEvent event) {
        // traitement champ itération
        int[] t = new int[1];
        try {
            t[0] = Integer.parseInt(iterations.getText());
        } catch (Exception ignored) {
        }
        // actu bouton play / stop
        sonVBox.getChildren().removeAll(play, delete);
        sonVBox.getChildren().addAll(stop, delete);
        // Thread du lancement de calcul
        new Thread(() -> {
            // calcul de l'image
            grid = Grid.builder()
                    .iteration(t[0])
                    .size((int) MainStage.windowHeight, (int) MainStage.windowHeight)
                    .mandelbrot()
                    .build();
            currentGrid = grid;
            Image res = grid.renderSceneMultiThreads();
            if (!isStopped) {
                image = new ImageView(res);
                currentImage = image;
            } else isStopped = false;
            // actualise l'IG
            Platform.runLater(() -> {
                sonVBox.getChildren().removeAll(stop, delete);
                sonVBox.getChildren().addAll(play, delete);
                root.setCenter(currentImage);
            });
        }).start();
    }

}
