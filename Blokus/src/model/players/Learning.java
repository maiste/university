package model.players;

import controller.Message;
import data.Bdd;
import model.Blokus;
import model.Matrix;
import model.Piece;

import java.awt.*;
import java.util.LinkedList;
import java.util.Random;

/**
 * Classe de gestion de la stratégie de l'IA à apprentissage autonome
 * @author Blokus_1
 */
public class Learning implements Strategy{
    private Blokus game;
    private Matrix[] matrices;
    private Player player;
    private Bdd baseDeDonnees = new Bdd();

    private final int variableOfChange = 600; // variable correspondant à l'affinement de l'apprentissage
    private final double variableOfBinaryAnswer = 0.5; // variable pour savoir si une node vaut 1 ou 0


    /** Constructeur de l'IA */
    public Learning(Blokus game) {
        this.game = game;
        this.player = null;
        matrices = baseDeDonnees.getMatrixFromBDD();
    }

    /** choisit le player correspondant à l'IA*/
    public void setPlayer(Player p) { this.player = p; }

    @Override
    public boolean needPlayer() { return true; }

    @Override
    /** retourne le coeffiient pour l'enregistrement dans la bdd */
    public double getCoef () { return 1; }

    /** fournit la marge d'erreur pour modifier l'IA */
    public int getErrorMarging(int best, int length) {
        double error = (best-player.getScore())/(double)best;
        System.out.println("Modification de l'ia de " + error*100 + " %");
        return (int)error*length;
    }

    @Override
    public Message getStrategy(LinkedList<LinkedList<Message>> possibilite, int joueur) {
        Matrix manipulation = this.entranceToMatrix(possibilite);
        for (int i = 0; i < 5; i++) {
            manipulation = binaryValue((matrices[i*2].productWith(manipulation)).sum(matrices[i*2+1]));
        }
        return traductionOfExit(matrices[10].productWith(manipulation), possibilite);
    }




    /**
     * Traduction du résultat obtenu par l'IA Learning en un message
     * @param exit matrice correspondant à la sortie des différentes couches
     * @param possibilite liste en fonction du poids de liste des messages possibles
     * @return Renvoie le message qui traduit le résultat obtenu par l'IA Learning
     */
    private Message traductionOfExit(Matrix exit, LinkedList<LinkedList<Message>> possibilite){
        int listOfMessage = getIntOfIntervalle(exit.getCaseValue(0,0), possibilite.size());
        if(listOfMessage < 0) return null;
        int intOfMessage = getIntOfIntervalle(exit.getCaseValue(1,0), possibilite.get(listOfMessage).size());
        if (intOfMessage < 0) return null;
        return possibilite.get(listOfMessage).get(intOfMessage);
    }

    /**
     * on détermine dans quel intervalle se situe la valeur après être passée par la sigmoide.
     * @param value valeur d'entrée
     * @param length nombre de pas pour la subdivision
     * @return retourne l'indice de l'intervalle dans lequel se situe value.
     */
    private int getIntOfIntervalle(int value, int length){
        double resultOfSigmoide = this.sigmoideBis(value);
        if (length <= 0) { return -1; }
        for (int i = 0; i < length; i++) {
            double pas = (1./length);
            if(resultOfSigmoide < (i+1)*pas  && resultOfSigmoide >= i*pas){ return i; }
        }
        return length-1;
    }

    /**
     * On transforme la matrice qu'on a obtenu après être passé par une couche
     * en une matrice formée de 1 et de 0
     * @param matrice matrice après calcul
     * @return matrice formée de 1 et de 0
     */
    private Matrix binaryValue(Matrix matrice){
        for (int i = 0; i < matrice.getRows(); i++) {
            for (int j = 0; j < matrice.getColumns(); j++) {
                int value = (sigmoideBis(matrice.getCaseValue(i,j)) >= variableOfBinaryAnswer)? 1 : 0;
                matrice.setValueAt(i,j,value);
            }
        }
        return matrice;
    }

    /**
     * Fonction d'activation similaire à la sigmoide mais adaptée au cas du Blokus :
     * $$s(x) = \frac{1}{1+e^{(-0.2)x}}$$
     * @param value valeur qu'on veut rentrer dans la fonction
     * @return valeur que nous renvoie la fonction
     */
    private double sigmoideBis(int value){
        double result = (1/(1+Math.exp((-0.2)*(value/1000.))));
        return result;
    }

    /**
     * On traduit les données d'entrées pour l'IA en une matrice.
     * @param possibilite liste en fonction du poids de liste des messages possibles
     * @return  Matrice correspondant à l'entrée.
     */
    private Matrix entranceToMatrix(LinkedList<LinkedList<Message>> possibilite){
        int[][] input = new int[805][1];
        int[] myPlace = this.whereAmIInBoard();
        int[] gotTheScore = this.whereAreTheOthers();
        int i = 0;
        for ( int isMe : myPlace) {
                input[i][0] = isMe;
                i++;
        }
        for (int target : gotTheScore) { input[i][0] = target; }
        for (LinkedList<Message> list : possibilite) { input[i][0] = list.size(); }
        return new Matrix(input);
    }

    /**
     * Vérifie si l'IA a gagné
     * @return ture si l'IA Learning est premier, false sinon
     */
    private boolean isWon(){
        Player winner = game.getWinner()[0];
        return winner.isIA() && winner.equals(this.player);
    }

    /**
     * Met à jour la base de données de l'IA
     */
    public void updateLearning() {
        Random rand = new Random();
        if (!isWon()) {
            int taille = 0;
            for (Matrix m : matrices) { taille += m.getLength(); }
            int modification = getErrorMarging(game.getPlayersByScore()[0].getScore(), taille);
            // Modifie un nombre de cases correspondant au pourcentage d'erreur
            while (modification > 0) {
                Matrix tmp = matrices[rand.nextInt(matrices.length)];
                int row = rand.nextInt(tmp.getRows());
                int col = rand.nextInt(tmp.getColumns());
                int value = tmp.getCaseValue(row, col) + (rand.nextInt(variableOfChange) - variableOfChange) / 2;
                if (value > 0) {
                    value = Math.min(5000, value);
                } else {
                    value = Math.max(-5000, value);
                }
                tmp.setValueAt(row, col, value);
                modification--;
            }
        }
        baseDeDonnees.saveMatrixtoBDD(matrices,game.gameInformation());
    }

    @Override
    public String getName() { return "skynet"; }

    /**
     * on prend le tableau de jeu est on retourne un tableau où la valeur est à 1
     * s'il s'agit de l'IA et 0 sinon
     * On transforme le tableau pour qu'il soit du point de vue subjectif de l'IA
     * @return un tableau d'int
     */
    private int[] whereAmIInBoard(){
        Player[][] board = game.getBoard();
        int[] copieBoard = new int[board.length*board.length];
        int k = 0;
        for (int i = 0 ; i < board.length ; i++) {
            for (int j = 0 ; j < board[i].length ; j++) {
                if(board[i][j] != null && board[i][j].equals(player)) {
                    copieBoard[k] = 1;
                } else {
                    copieBoard[k] = 0;
                }
                k++;
            }
        }
        return copieBoard;
    }


    private int[] whereAreTheOthers() {
        Player[][] board = game.getBoard();
        int[] target = new int[board.length*board.length];
        int k = 0;
        for (int i = 0 ; i < board.length ; i++) {
            for (int j = 0 ; j < board[i].length ; j++) {
               if(board[i][j] != null) { target[k] = board[i][j].getScore(); }
               else { target[k] = 0; }
                k++;
            }
        }
        return target;
    }

    public static void main(String[] args) {
        Bdd bdd = new Bdd();
        Random rand = new Random();

        int[][] mat1 = new int[40][805];
        int[][] mat2 = new int[40][1];
        int[][] mat3 = new int[40][40];
        int[][] mat4 = new int[40][1];
        int[][] mat5 = new int[40][40];
        int[][] mat6 = new int[40][1];
        int[][] mat7 = new int[40][40];
        int[][] mat8 = new int[40][1];
        int[][] mat9 = new int[40][40];
        int[][] mat10 = new int[40][1];
        int[][] mat11 = new int[2][40];


        for (int i = 0; i < 40; i++) {
            for (int j = 0; j < 805; j++) {
                mat1[i][j] = rand.nextInt(10000)-5000;
            }
            mat2[i][0] = rand.nextInt(10000)-5000;
            mat4[i][0] = rand.nextInt(10000)-5000;
            mat6[i][0] = rand.nextInt(10000)-5000;
            mat8[i][0] = rand.nextInt(10000)-5000;
            mat10[i][0] = rand.nextInt(10000-5000);
            mat11[0][i] = rand.nextInt(10000)-5000;
            mat11[1][i] = rand.nextInt(10000)-5000;

            for (int j = 0; j < 40; j++) {
                mat3[i][j] = rand.nextInt(10000)-5000;
                mat5[i][j] = rand.nextInt(10000)-5000;
                mat7[i][j] = rand.nextInt(10000)-5000;
                mat9[i][j] = rand.nextInt(10000)-5000;
            }
        }

        Matrix[] matrices = {new Matrix(mat1),
                new Matrix(mat2),
                new Matrix(mat3),
                new Matrix(mat4),
                new Matrix(mat5),
                new Matrix(mat6),
                new Matrix(mat7),
                new Matrix(mat8),
                new Matrix(mat9),
                new Matrix(mat10),
                new Matrix(mat11)};

        bdd.saveOneMatrixToBDDInit(matrices, "initialisation");
    }
}
