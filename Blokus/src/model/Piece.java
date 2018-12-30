package model;

import model.players.Utilitaries;

import java.awt.Point;
import java.util.LinkedList;

/**
 * Classe correspondant à une piece
 * elle gère la construction de toutes les pièces du jeu
 * @author Blokus_1
 */
public class Piece implements Cloneable{

  private Point[][] coordinates;
  private LinkedList<Point> points;
  private final int id;
  private static int reference = 0;
  private static Piece[][] piecesList;

  /****************
   * Constructeur *
   ****************/

  static {
    piecesList = new Piece[4][21];
    for (int i = 0 ; i < piecesList.length ; i++) {
      piecesList[i] = createPieceTab();
    }
  }

  /**
   * Constructeur
   * Il crée une pièce copiant le tableau de coordonnées
   * @param coordinates tableau de coordonnées copié pour la création d'une nouvelle pièce
   */
  public Piece(Point[][] coordinates) {
    this.id = reference;
    reference++;
    this.coordinates = new Point[coordinates.length][coordinates[0].length];
    for (int i = 0; i < coordinates.length; i++) {
      for (int j = 0; j < coordinates[i].length; j++) {
        if(coordinates[i][j]!=null) this.coordinates[i][j]=new Point((int)coordinates[i][j].getX(),(int)coordinates[i][j].getY());
        else this.coordinates[i][j]=null;
      }
    }
    updatePoint();
  }


/*********
 * Objet *
 ********/
  /**
   * Cloner la piece this
   * @return clone de this
   */
  @Override
  public Object clone() {
    Piece piece = null;
    try {
      piece = (Piece) super.clone();
      int taille = piece.length();
      piece.coordinates = new Point[taille][taille];
      for (int i = 0; i < taille; i++) {
        for (int j = 0; j < taille; j++) {
          if(coordinates[i][j] != null) piece.coordinates[i][j]=(Point)coordinates[i][j].clone();
          else piece.coordinates[i][j]=null;
        }
      }
      piece.updatePoint();
    }
    catch (Exception e){
      e.printStackTrace();
      System.exit(0);
    }
    return piece;
  }

  /**
   * on retourne l'égalité entre les deux pièces
   * @param obj pièce donnée en comparaison
   * @return true si l'id de la pièce est le même.
   */
  @Override
  public boolean equals(Object obj) {
    if(obj instanceof Piece){
      return this.id == ((Piece) obj).id;
    }
    return this == obj;
  }


  /**********
   * Getter *
   **********/

  /**
   * getter
   * @return Case de la pièce étant utilisée comme référence pour placer la pièce
   */
  public Point getReference(){  return coordinates[2][2]; }

  /**
   * getter
   * @return tableau de coordonnées d'une pièce
   */
  public Point[][] getCoordonates() {  return coordinates;  }

  /**
   * getter
   * @return l'id unique
   */
  public int getId(){  return this.id; }

  /**
   * getter
   * @return taille du tableau de coordonnées
   */
  public int length(){ return coordinates.length;  }

  /**
   * getter
   * @return poids de la pièce (nombre de case la composant)
   */
  public int weight(){  return points.size(); }

  /**
   * getter
   * @return la liste de points de la pièce
   */
  public LinkedList<Point> getPoints() {  return points; }

  /**
   * Retourne la liste de pièces
   * @param i indice de la liste de pièces
   * @return la liste de pièces de poids i+1
   */
  public static Piece[] getPiecesList(int i){ return (i >= 0 && i < piecesList.length)?piecesList[i]:null; }

  /**
   * on récupère les coordonnées des cases (par rapport à la case de référence de position (2,2))
   * qui sont en coin avec la pièce.
   * @return liste des ces points.
   */
  public LinkedList<Point> getCornerOfPiece(){
    this.initCoordinate();
    LinkedList<Point> tab = new LinkedList<>();
    for (Point m : points){
      int i = (int) m.getY();
      int j = (int) m.getX();
      if(this.isCornerOfPiece(i-1,j-1,m)){
        tab.add(new Point(j-3,i-3));
      }
      if(this.isCornerOfPiece(i+1,j-1,m)){
        tab.add(new Point(j-3,i-1));
      }
      if(this.isCornerOfPiece(i+1,j+1,m)){
        tab.add(new Point(j-1,i-1));
      }
      if(this.isCornerOfPiece(i-1,j+1,m)){
        tab.add(new Point(j-1,i-3));
      }
    }
    return tab;
  }


  /**
   * Récupère tous les coins d'une pièce
   * @return LinkedList de point ayant pour coordonnées (x,y) les cases étant des coins (x = abscisse, y = ordonnee)
   */
  public LinkedList<Point> cornerOfPiece(){
    LinkedList<Point> tab = new LinkedList<>();
    for (Point m : points){
      int i = m.y;
      int j = m.x;
      if(this.isCornerOfPiece(i-1,j-1,m)
              || this.isCornerOfPiece(i+1,j-1,m)
              || this.isCornerOfPiece(i+1,j+1,m)
              || this.isCornerOfPiece(i-1,j+1,m)){
        tab.add(new Point(j,i));
      }
    }
    return tab;
  }

/***********
 * Updates *
 ***********/

  /**
   * Fait une translation de tous les points du tableau de coordonnées tel que le point de
   * référence soit au niveau du point donné en paramètre
   * @param reference Point sur lequel doit être le point de reference de la piece
   * @return Piece(this) ayant les coordonnées de ses points changés
   */
  public Piece updateCoordonate(Point reference){
    this.initCoordinate();
    int xTranslation = (int)reference.getX() - (int)getReference().getX();
    int yTranslation = (int)reference.getY() - (int)getReference().getY();
    for (Point i : points){
      i.translate(xTranslation,yTranslation);
    }
    return this;
  }

  /**
   * Met le compteur d'id à zéro
   */
  public static void resetIdReference(){ reference = 0; }



  /*****************
   * Vérifications *
   *****************/
  /**
   * la case de coordonnées iref et jref dans le tableau coordonates est un coin de la pièce
   * @param i ordonnée de la case en coin de la case de coin de la pièce qui doit être vide
   * @param j abscisse de la case en coin de la case de coin de la pièce qui doit être vide
   * @param corner coordonnées de la case pouvant être un coin
   * @return true si la case est un coin, false sinon.
   */
  public boolean isCornerOfPiece (int i, int j, Point corner) {
    int iref = corner.y;
    int jref = corner.x;
    boolean b = true;
    if (i < coordinates.length && i >= 0 && j < coordinates.length && j >= 0) {
      b = this.coordinates[i][j] == null;
    } if (i < coordinates.length && i >= 0) {
      b &= this.coordinates[i][jref] == null;
    } if (j < coordinates.length && j >= 0) {
      b &= this.coordinates[iref][j] == null;
    }
    return b;
  }


  /*******************
   * Transformations *
   *******************/

  /**
   * Faire le miroir de la pièce selon un axe vertical
   * @return Pièce(this) ayant les coordonnées de ses points changées
   */
  public Piece mirror(){
    Point ref = new Point((int)this.getReference().getX(), (int)this.getReference().getY());
    coordinates = Utilitaries.mirrorVertical(coordinates);
    return this.updateCoordonate(ref);
  }

  /**
   * Faire une rotation de 90 degrés de la pièce
   * @return Pièce(this) ayant les coordonnées de ses points changées
   */
  public Piece rotate(){
    Point ref = new Point((int)this.getReference().getX(), (int)this.getReference().getY());
    coordinates = Utilitaries.rotate(coordinates);
    return this.updateCoordonate(ref);
  }

  /**
   * Normalise toutes les coordonnées : chaque point du tableau a
   * pour coordonnée la position en x et y dans le tableau de coordonnées.
   * @return this - l'objet courant
   */
  public Piece initCoordinate(){
    for (int i = 0; i < coordinates.length; i++) {
      for (int j = 0; j < coordinates[i].length; j++) {
        if(coordinates[i][j]!=null) coordinates[i][j].setLocation(j,i);
      }
    }
    return this;
  }

  /**
   * Met à jour la LinkedList de points
   * @return this - l'objet courant
   */
  public Piece updatePoint(){
    LinkedList<Point> points = new LinkedList<>();
    for (int i = 0; i < coordinates.length; i++) {
      for (int j = 0; j < coordinates.length; j++) {
        if(coordinates[i][j] != null) { points.add(coordinates[i][j]); }
      }
    }
    this.points = points;
    return this;
  }

  /**
   * Calcule la distance minimale entre chaque point de la pièce avec le point corner
   * @param corner coin par rapport auquel on cherche la distance minimale
   * @param reference où la pièce sera posée pour calculer la distance minimale
   * @return distance minimal
   */
  public int minimumOfDistanceWith(Point corner, Point reference){
    this.updateCoordonate(reference);
    int distance = 0;
    double local;
    for (Point point : points){
      local = corner.distance(point);
      if(local < distance){  distance = (int) local; }
    }
    this.initCoordinate();
    return distance;
  }


  /*************
   * Affichage *
   **************/

  @Override
  public String toString() {
    String a = this.hashCode() +", "+id+ " : ";
    for (int i = 0; i < coordinates.length; i++) {
      for (int j = 0; j < coordinates.length; j++) {
        if(coordinates[i][j] != null){
          a += coordinates[i][j].hashCode()+" : "+coordinates[i][j].toString();
          if(i==2 && j==2) { a+= " : ref"; }
          a+=", ";
        }
      }
    }
    a+="\n\n";
    for (Point b: points){  a+=b.toString()+", "; }
    a+="\n\n";
    return a;
  }

  /*************************
   * Création de pièces    *
   *************************/

  /**
   * Retourne l'ensemble des pièces du jeu
   * @return LinkedList de LinkedList de pièces
   */
  public static Piece[] createPieceTab(){
    return fivePiecesTab();
  }

  /**
   * Crée les pièces avec un poids de 1
   * @return LinkedList de LinkedList ces pièces
   */
  private static Piece[] onePieceTab(){
    Point[][] point = new Point[5][5];
    point[point.length/2][point[0].length/2] = new Point();
    Piece [] list = new Piece[21];
    list [0] = new Piece(point).initCoordinate().updatePoint();
    return list;
  }

  /**
   * Crée les pièces avec un poids de 2
   * @return LinkedList de LinkedList de ces pièces
   */
  private static Piece[] twoPiecesTab(){
    Piece[] list = Piece.onePieceTab();
    list[1] = new Piece(list[0].getCoordonates());
    int taille = list[1].length();
    list[1].getCoordonates()[taille/2][taille/2-1] = new Point();
    list[1].initCoordinate().updatePoint();
    return list;
  }

  /**
   * Crée les pièces avec un poids de 3
   * @return LinkedList de LinkdeList de ces pièces
   */
  private static Piece[] threePiecesTab(){
    Piece[] list = Piece.twoPiecesTab();
    list[2] = new Piece(list[1].getCoordonates());
    list[3] = new Piece(list[1].getCoordonates());
    int taille = list[1].length();
    list[2].getCoordonates()[taille/2][taille/2+1] = new Point();
    list[3].getCoordonates()[taille/2-1][taille/2-1] = new Point();
    list[2].initCoordinate().updatePoint();
    list[3].initCoordinate().updatePoint();
    return list;
  }

  /**
   * Crée les pièces avec un poids de 4
   * @return LinkedList de LinkedList de ces pièces
   */
  private static Piece[] fourPiecesTab(){
    Piece[] list = Piece.threePiecesTab();
    list[4] = new Piece(list[2].getCoordonates());
    list[5] = new Piece(list[3].getCoordonates());
    list[6] = new Piece(list[2].getCoordonates());
    list[7] = new Piece(list[2].getCoordonates());
    list[8] = new Piece(list[3].getCoordonates());
    int taille = list[2].length();
    list[4].getCoordonates()[taille/2][taille/2+2] = new Point();
    list[5].getCoordonates()[taille/2-1][taille/2] = new Point();
    list[6].getCoordonates()[taille/2-1][taille/2] = new Point();
    list[7].getCoordonates()[taille/2-1][taille/2+1] = new Point();
    list[8].getCoordonates()[taille/2-1][taille/2-2] = new Point();
    for (int i = 4 ; i < 9 ; i++) { list[i].initCoordinate().updatePoint(); }
    return list;
  }


  /**
   * Crée les pièces avec un poids de 5
   * @return LinkedList de LinkedList de ces pièces
   */
  private static Piece[] fivePiecesTab(){
    Piece[] list = Piece.fourPiecesTab();
    list[9] =new Piece(list[4].getCoordonates());
    list[10] = new Piece(list[5].getCoordonates());
    list[11] = new Piece(list[6].getCoordonates());
    list[12] = new Piece(list[7].getCoordonates());
    list[13] = new Piece(list[8].getCoordonates());
    list[14] = new Piece(list[7].getCoordonates());
    list[15] = new Piece(list[7].getCoordonates());
    list[16] = new Piece(list[8].getCoordonates());
    list[17] = new Piece(list[7].getCoordonates());
    list[18] = new Piece(list[7].getCoordonates());
    list[19] = new Piece(list[6].getCoordonates());
    list[20] = new Piece(list[4].getCoordonates());

    int taille = list[9].length();
    list[9].getCoordonates()[taille/2][taille/2-2] = new Point();
    list[10].getCoordonates()[taille/2-2][taille/2-1] = new Point();
    list[11].getCoordonates()[taille/2-2][taille/2] = new Point();
    list[12].getCoordonates()[taille/2][taille/2-2] = new Point();
    list[13].getCoordonates()[taille/2][taille/2+1] = new Point();
    list[14].getCoordonates()[taille/2-2][taille/2+1] = new Point();
    list[15].getCoordonates()[taille/2+1][taille/2-1] = new Point();
    list[16].getCoordonates()[taille/2+1][taille/2] = new Point();
    list[17].getCoordonates()[taille/2-1][taille/2-1] = new Point();
    list[18].getCoordonates()[taille/2+1][taille/2] = new Point();
    list[19].getCoordonates()[taille/2+1][taille/2] = new Point();
    list[20].getCoordonates()[taille/2-1][taille/2] = new Point();
    for (int i = 9 ; i < 21 ; i++){ list[i].initCoordinate().updatePoint(); }
    return list;
  }
}