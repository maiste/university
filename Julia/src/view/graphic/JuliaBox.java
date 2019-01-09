package view.graphic;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.Grid;

import java.util.LinkedList;
import java.util.List;

/**
 * Noeud graphique de chaque fonction de type julia
 *
 * @author marais bello
 */
public final class JuliaBox extends FunctionBox {

    private int polyId; // numéro du dernier polynome
    private List<TextField> polyList;

    private ScrollPane scroll;
    private HBox function;
    private CheckBox infinite;

    /**
     * Créer un JuliaBox non initialisé
     *
     * @param root      Panel principal
     * @param container VBox contenant toutes les fonctions
     */
    public JuliaBox(BorderPane root, VBox container) {
        super(root, container);
        polyId = 1;
        polyList = new LinkedList<>();
        // Paramètres de la FunctioBox :
        setPrefSize(MainStage.leftWidth, MainStage.windowHeight * 0.07d);
        container.getChildren().add(this);
        // zone paramétrage fonction
        function = new HBox();
        // scroll
        scroll = new ScrollPane();
        scroll.setPrefHeight(getPrefHeight());
        scroll.setStyle("-fx-background-color:transparent;");
        scroll.setContent(function);
        scroll.setFitToHeight(true);
        // fonction
        function.setSpacing(10);
        function.setPrefHeight(scroll.getPrefHeight());
        infinite = new CheckBox("infini");
        infinite.setPrefHeight(scroll.getPrefHeight());
        infinite.setIndeterminate(false);
        iterations = new TextField();
        iterations.setPromptText("ITERATIONS");
        iterations.setPrefSize(MainStage.leftWidth / 5, scroll.getPrefHeight());
        real = new TextField();
        real.setPromptText("REEL");
        real.setPrefSize(MainStage.leftWidth / 5, scroll.getPrefHeight());
        imaginary = new TextField();
        imaginary.setPromptText("IMAGINAIRE");
        imaginary.setPrefSize(MainStage.leftWidth / 5, scroll.getPrefHeight());
        function.getChildren().addAll(infinite, iterations, real, imaginary, getPoly());
        getChildren().add(scroll);
        initMenu();
    }

    /**
     * Crée un JuliaBox pré - configurer
     *
     * @param root      panel principal
     * @param container vbox contenant toute les fonctions
     * @param r         partie réel
     * @param i         patrie immaginaire
     * @param polysVal  polynome
     */
    public JuliaBox(BorderPane root, VBox container, int iters, String r, String i, double... polysVal) {
        this(root, container);
        iterations.setText(iters + "");
        real.setText(r);
        imaginary.setText(i);
        function.getChildren().remove(4);
        polyList.remove(0);
        polyId--;
        for (double val : polysVal) function.getChildren().add(getPoly(val));
        function.getChildren().add(getPoly());
    }

    @Override
    protected void handlePlay(ActionEvent event) {
        final double t[] = new double[3];
        // récupère les part reel et imaginaire
        try {
            t[0] = Integer.parseInt(iterations.getText());
            t[1] = Double.parseDouble(real.getText());
            t[2] = Double.parseDouble(imaginary.getText());
        } catch (Exception ignored) {
        }
        // transforme le bouton play en stop
        sonVBox.getChildren().removeAll(play, delete);
        sonVBox.getChildren().addAll(stop, delete);
        // recupère les coefficients des polynomes
        double[] coeffs = new double[polyList.size() + 1];
        for (int i = 0; i < polyList.size(); i++) {
            try {
                coeffs[i + 1] = Double.parseDouble(polyList.get(i).getText());
            } catch (Exception ignored) {
            }
        }
        // lance le calcul
        new Thread(() -> {
            // on crée la grille
            Grid.GridBuilder gb = (infinite.isSelected()) ?
                    (Grid.builder().infinity()) : (Grid.builder().iteration((int) t[0]));
            grid = gb
                    .size((int) MainStage.windowHeight, (int) MainStage.windowHeight)
                    .function(coeffs, t[1], t[2])
                    .build(); // Build l'objet
            currentGrid = grid;
            Image res = grid.renderSceneMultiThreads(); // calcul
            // gère l'arrêt du calcul
            if (!isStopped) {
                image = new ImageView(res);
                currentImage = image;
            } else isStopped = false;
            // transforme le bouton stop en play
            Platform.runLater(() -> {
                sonVBox.getChildren().removeAll(stop, delete);
                sonVBox.getChildren().addAll(play, delete);
                root.setCenter(currentImage);
            });
        }).start();
    }

    /**
     * Retourne un le monome suivant
     *
     * @return monome suivant
     */
    private TextField getPoly() {
        TextField res = new TextField();
        res.setPrefSize(MainStage.leftWidth / 5, scroll.getPrefHeight());
        res.setPromptText("x" + polyId);
        res.textProperty().addListener(this::handlePoly);
        polyId++;
        polyList.add(res);
        return res;
    }

    /**
     * Retourne le monome suivant initialisé à value
     *
     * @param value valeure du monome
     * @return monome suivant
     */
    private TextField getPoly(double value) {
        TextField res = new TextField(value + "");
        res.setPrefSize(MainStage.leftWidth / 5, scroll.getPrefHeight());
        res.textProperty().addListener(this::handlePoly);
        polyId++;
        polyList.add(res);
        return res;
    }

    /**
     * Gère l'ajout / la suppression du dernier polynome de saisie
     * si le dernier n'est pas vide, on en crée un nouveau
     * si l'avant dernier est vide, on supprime le dernier
     *
     * @param e ignoré
     */
    private void handlePoly(Observable e) {
        // le dernier polynome est vide
        if (!polyList.get(polyList.size() - 1).getText().equals(""))
            function.getChildren().add(getPoly());
        // l'avant dernier polynome est vide
        if (polyList.get(polyList.size() - 2).getText().equals("")) {
            function.getChildren().remove(polyList.get(polyId - 2));
            polyList.remove(polyList.size() - 1);
            polyId--;
        }
    }
}
