package view.graphic;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.Grid;

import java.io.InputStream;
import java.util.List;


/**
 * Noeud graphique de chaque fonction, une fonction peut être une JuliaBox ou
 * une MandelbrotBox
 *
 * @author marais bello
 */
public abstract class FunctionBox extends HBox {

    protected static ImageView currentImage;
    protected static Grid currentGrid;
    protected BorderPane root; // Panel principal
    protected VBox container;
    protected boolean isStopped;
    protected Grid grid;
    protected TextField iterations;
    protected TextField real;
    protected TextField imaginary;
    protected ImageView image;

    protected VBox sonVBox;
    protected Button play;
    protected Button stop;
    protected Button delete;

    /**
     * @param root      panel principal
     * @param container conteneur de la FunctionBox
     */
    public FunctionBox(BorderPane root, VBox container) {
        super();
        this.root = root;
        this.container = container;
        isStopped = false;
        root.setOnKeyReleased(this::handleKey);
    }

    /**
     * initialise le sous menu à droite contenant les boutons
     * play, stop et delete
     */
    protected void initMenu() {
        // VBox du sous menu à droite :
        sonVBox = new VBox();
        sonVBox.setPadding(new Insets(0, 10, 0, 10));
        sonVBox.setPrefSize(container.getPrefWidth() * 0.20d, getPrefHeight());
        sonVBox.setAlignment(Pos.CENTER);
        // boutons du sous menu :
        // play :
        play = new Button();
        InputStream is = getClass().getResourceAsStream("../lib/play-button-1.png");
        play.setGraphic(new ImageView(new Image(is, sonVBox.getPrefWidth() / 3, sonVBox.getPrefHeight() / 2.5, false, false)));
        play.setOnAction(this::handlePlay);
        // pause :
        stop = new Button();
        is = getClass().getResourceAsStream("../lib/stop-1.png");
        stop.setGraphic(new ImageView(new Image(is, sonVBox.getPrefWidth() / 3, sonVBox.getPrefHeight() / 2.5, false, false)));
        stop.setOnAction(this::handleStop);
        // delete
        delete = new Button();
        is = getClass().getResourceAsStream("../lib/error.png");
        delete.setGraphic(new ImageView(new Image(is, sonVBox.getPrefWidth() / 3, sonVBox.getPrefHeight() / 2.5, false, false)));
        delete.setOnAction(this::handleDelete);
        sonVBox.getChildren().addAll(play, delete);
        getChildren().add(sonVBox);
    }

    /*
     * EVENEMENTS
     */

    /**
     * Gère l'evenement d'appuie d'une touche du clavier
     *
     * @param event evènement déclencher par le clavier
     */
    private void handleKey(KeyEvent event) {
        if (currentGrid == null || !currentGrid.isRunning()) {
            if (event.getCode() == KeyCode.PLUS || event.getCode() == KeyCode.EQUALS)
                currentGrid.zoomGrid(40);
            else if (event.getCode() == KeyCode.MINUS) currentGrid.zoomGrid(-40);
            else if (event.getCode() == KeyCode.RIGHT) currentGrid.moveOrigin(0.2, 0);
            else if (event.getCode() == KeyCode.LEFT) currentGrid.moveOrigin(-0.2, 0);
            else if (event.getCode() == KeyCode.UP) currentGrid.moveOrigin(0, 0.2);
            else if (event.getCode() == KeyCode.DOWN) currentGrid.moveOrigin(0, -0.2);
            if (List.of(KeyCode.PLUS, KeyCode.EQUALS, KeyCode.MINUS, KeyCode.RIGHT, KeyCode.LEFT, KeyCode.UP, KeyCode.DOWN)
                    .contains(event.getCode()))
                root.setCenter(new ImageView(currentGrid.renderSceneMultiThreads()));
        }
    }

    /**
     * Gère l'evenement du bouton play
     *
     * @param event evènement déclencher par le bouton
     */
    protected abstract void handlePlay(ActionEvent event);

    /**
     * Gère l'evenement du bouton stop
     *
     * @param event evènement déclencher par le bouton
     */
    private void handleStop(ActionEvent event) {
        currentGrid.stop();
        isStopped = true;
        currentImage = null;
        root.setCenter(currentImage);
    }

    /**
     * Gère l'evenement du bouton delete
     *
     * @param event evènement déclencher par le bouton
     */
    private void handleDelete(ActionEvent event) {
        if (currentGrid == null || !currentGrid.isRunning())
            container.getChildren().remove(((Button) event.getSource()).getParent().getParent());
    }
}