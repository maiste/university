package model;

import data.Bdd;

import java.awt.*;
import java.io.Serializable;

/**
 * Classe de simulations des matrices
 * @author Blokus_1
 */
public class Matrix implements Serializable {

    private int rows;
    private int columns;
    private int length;
    private int[][] matrix;


/****************
 * Constructeur *
 ****************/

    /**
     * Constructeur
     * @param rows nombres de lignes
     * @param columns nombre de colonnes
     */
    public Matrix  (int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        this.length = rows*columns;
        matrix = new int[rows][columns];
    }

    /**
     * Constructeur de copie
     * @param toCopy la matrice à copier
     */
    public Matrix (Matrix toCopy) {
        this(toCopy.rows, toCopy.columns);
        for (int i = 0 ; i < toCopy.matrix.length ; i++) {
            for (int j = 0 ; j < toCopy.matrix[i].length ; j++) {
                this.matrix[i][j] = toCopy.matrix[i][j];
            }
        }
    }

    /**
     * Créer un objet matrix à partir d'un tableau
     * @param matrix le tableau matrice
     */
    public Matrix (int[][] matrix) {
        this.rows = matrix.length;
        this.columns = matrix[0].length;
        this.length = rows* columns;
        this.matrix = matrix;
    }


 /**********
  * Getter *
  **********/

    /**
     * Le nombre de ligne
     * @return int - nombre de ligne
     */
    public int getRows() {
        return rows;
    }

    /**
     * Nombre de colonnes
     * @return int - le nombre de colonne
     */
    public int getColumns() {
        return columns;
    }

    /**
     * Recupère la matrice
     * @return le tableau de int
     */
    public int[][] getMatrix() {
        return matrix;
    }

    /**
     * Donne la valeur de la case
     * @param i la ligne
     * @param j la colonne
     * @return la valeur de la case i j
     */
    public int getCaseValue(int i, int j) {
        if (i < rows || i >= 0 || j <= 0 || j < columns) {
            return matrix[i][j];
        } return -1;
    }

    public int getLength() {
        return this.length;
    }

  /**********
   * Setter *
   **********/

    /**
     * Change la matrice et adapte les colonnes et lignes
     * @param matrix la matrice
     */
  public void setMatrix(int[][] matrix) {
      this.matrix = matrix;
      this.rows = matrix.length;
      this.columns = matrix[0].length;
  }

    /**
     * Change la valeur de la matrice dans une case
     * @param i la ligne
     * @param j la colonne
     * @param value la valeur
     */
  public void setValueAt(int i, int j, int value){
      if (i >= 0 && i < rows && j >= 0 && j < columns){
          matrix[i][j] = value;
      }
  }


/**********
 * Calculs *
 **********/

    /**
     * Retourne le produit de deux matrices
     * @param m2 la matrice à changer
     * @return la matrice produit
     */
    public Matrix productWith (Matrix m2) {
        int[][] res = new int[this.rows][m2.columns];
        if (this.columns != m2.rows){ return null ; }
        for (int i = 0 ;  i < res.length ; i++) {
            for (int j = 0 ; j < res[i].length ; j++) {
                for (int k = 0 ; k < res[i].length ; k++) {
                    res[i][j] += matrix[i][k]*m2.matrix[k][j];
                }
            }
        }
        return new Matrix(res);
    }


    /**
     * Retourne une matrice resultant de la somme de this et m2
     * @param m2 la matrice à ajouter à this
     * @return null ou la matrice somme
     */
    public Matrix sum (Matrix m2){
        int[][] res = new int [rows][columns];
        if (rows == m2.rows && columns == m2.columns) {
           for (int i = 0 ; i < matrix.length ; i++) {
               for (int j = 0 ; j < matrix[i].length ; j++) {
                   res[i][j] = this.matrix[i][j] + m2.matrix[i][j];
               }
           }
            return new Matrix(res);
        }
        return null;
    }

    public Matrix produitAdamar (Matrix m2) {
        int[][] res = new int [rows][columns];
        if (rows == m2.rows && columns == m2.columns) {
            for (int i = 0 ; i < matrix.length ; i++) {
                for (int j = 0 ; j < matrix[i].length ; j++) {
                    res[i][j] = this.matrix[i][j] * m2.matrix[i][j];
                }
            }
            return new Matrix(res);
        }
        return null;
    }


 /*************
  * Affichage *
  *************/

    /** Affiche la matrice */
    public void print(){
        for (int i = 0 ; i < matrix.length ; i++) {
            System.out.print("|");
            for (int j = 0 ; j < matrix[i].length ; j++) {
                System.out.print(matrix[i][j] + "|");
            }
            System.out.println();
        }
        System.out.println();
    }
}
