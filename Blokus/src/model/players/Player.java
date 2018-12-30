package model.players;

import java.awt.Color;
import java.util.LinkedList;

import model.Piece;

/**
 * Classe du joueur
 * @author Blokus_1
 */
public class Player {

  protected LinkedList<LinkedList<Piece>> pieces;
  protected Piece[] index;
  protected Color color;
  protected String name;
  protected int score = 0;
  protected boolean canPlay = true;
  public final int id;


  /*****************
   * Constructeurs *
   *****************/

  /**
   * Constructeur
   * @param color la couleur du joueur
   * @param id le numéro du joueur {1,2,3,4}
   * @param name le nom du joueur
   */
  public Player(Color color, int id, String name) {
    this.color = color;
    this.id = id;
    this.name = name;
    this.index = Piece.getPiecesList(id-1);
    this.initialisePieces();
  }

/***********
 * Getters *
 ***********/

  /** Renvoie le nom du joueur */
  public String getName () {
    return name;
  }

  /** renvoie le coefficient d'un joueur, ici c'est le score du joueur
   * car on a besoin du coefficient que lorsqu'il y a un seul joueur
   * @return le score du joueur
   */
  public double getCoef () {
    return this.score;
  }

  /** Renvoie la couleur du joueur */
  public Color getColor() {
    return color;
  }

  /** Renvoie si le joueur peut jouer */
  public boolean getCanPlay () {
    return canPlay;
  }

  /**  Renvoie le score du joueur */
  public int getScore() {
    return score;
  }

  /** Renvoie l'id du joueur (différent de l'indice dans le tableau) */
  public int getId() {
    return id;
  }

  /**
   * Récupère la pièce dans l'index
   * @param id l'id de la pièce
   * @return la pièce ou null
   */
  public Piece getPieceAt(int id){
    int localid = id%21;
    return index[localid];
  }

  /** on retourne un boolean pour savoir si on est un IA */
  public boolean isIA(){
    return false;
  }

/***********
 * Setters *
 ***********/

  /** Fixe le score */
  public void setScore(int score){ this.score = score; }

  /** Change si le joueur peut jouer */
  public void setCanPlay(boolean canPlay) { this.canPlay = canPlay; }

  /** Récupère l'ensemble des pièces du joueurs */
  public LinkedList<LinkedList<Piece>> getPieces () {
    return pieces;
  }


  /******************
   * Actions joueur *
   ******************/


  /** Initialise l'ensemble des pièces du joueur */
  private void initialisePieces(){
    pieces = new LinkedList<LinkedList<Piece>>();
    for (int i = 0 ; i < 5 ; i++) { pieces.add(new LinkedList<Piece>()); }
    pieces.get(0).add(index[0]);
    pieces.get(1).add(index[1]);
    pieces.get(2).add(index[2]);
    pieces.get(2).add(index[3]);
    for (int i = 4 ; i < 9 ; i++) { pieces.get(3).add(index[i]); }
    for (int i = 9 ; i < 21 ; i++) { pieces.get(4).add(index[i]); }
  }

  /**
   * Enleve la pièce de la liste des pièces jouables
   * @param piece la pièce à enlever
   */
  public void removePiece(Piece piece){
    for (LinkedList<Piece> i : pieces){
      if(i.contains(piece)){
        i.remove(piece);
        break;
      }
    }
    if(this.pieceIsEmpty() && piece.weight() == 1) { score+= 5; }
    else { score += piece.weight(); }
  }

  /**
   * On regarde s'il reste des pieces au joueur
   * @return true s'il ne reste plus de rien, false s'il en reste au moins une.
   */
  private boolean pieceIsEmpty(){
    for (LinkedList<Piece> pieceLinkedList : pieces){
      if(! pieceLinkedList.isEmpty()) { return false; }
    }
    return true;
  }

  /**
   * ajoute une pièce dans la liste des pièces du joueur
   * @param piece pièce à ajouter
   */
  public void addPiece(Piece piece){
    piece.initCoordinate();
    this.pieces.get(piece.weight()-1).add(piece);
    score-= piece.weight();
  }




  /*********
   * Tests *
   **********/

  /**
   * Utile pour voir l'état du joueur, à utiliser pour des debugs éventuels
   * @return string décrivant le joueur
   */
  public String printDebug() {
    String ret = "Player{" +
            pieces.hashCode() +":pieces=";
    for (LinkedList<Piece> i :pieces){
      ret += i.hashCode()+i.toString();
    }
    return ret +
            "\n, color=" + color +
            "\n, score=" + score +
            "\n, canPlay=" + canPlay +
            "\n, id=" + id +
            '}';
  }

  /**
   * on met à jour la bdd si le joueur est IA Learning
   */
  public void updateLearning(){}

  /**
   * on renvoie un string ayant les éléments suivants du joueur :
   * - nom
   * - couleur
   * - id
   */
  @Override
  public String toString() {
    String ret = this.name.substring(0,1).toUpperCase()+this.name.substring(1);
    ret += ", ayant pour couleur le ";
    ret += (color.equals(Color.BLUE))? "bleu" :
            (color.equals(Color.YELLOW))? "jaune" :
                    (color.equals(Color.RED))? "rouge" :
                            "vert";
    ret += " et pour id "+id;
    return ret;
  }
}