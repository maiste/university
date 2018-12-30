package model;

import controller.Intel;
import controller.Message;
import model.players.*;
import java.awt.*;
import java.util.LinkedList;


/**
 * Classe qui gère le plateau de jeu de
 * façon indépendante
 * @author Blokus_1
 */
public class Blokus {

  public final static Color[] colors = {Color.BLUE, Color.YELLOW, Color.RED, Color.GREEN};
  private final boolean withTeam;
  private boolean trainingMode;
  private Player[] players;
  private Player[][] board;
  private int turn;
  private int playerTurn;

/******************
 * Constructeurs  *
 ******************/

  /**
   * Constructeur par défaut
   */
  public Blokus (boolean withTeam) {
    Piece.resetIdReference();
    this.withTeam = withTeam;
    this.trainingMode = false;
    players = new Player[4];
    board = new Player[20][20];
    turn = 0;
    playerTurn = 0;
  }

  /**
   * Constructeur
   * @param initTab tableau de taille 4 contenant les spécificités d'initialisation (IA ou players)
   * @param withTeam si on joue 2vs2 ou non
   */
  public Blokus (Intel[] initTab, boolean withTeam) {
    this(withTeam);
    if (initTab.length != 4) {
      System.out.println("Erreur dans la construction des joueurs");
      System.exit(-1);
    }
    for (int i = 0 ; i < initTab.length ; i++){
      players[i] = initPlayerChoice(initTab[i].getLevel(),initTab[i].getName(),i+1);
    }
  }

  /**
   * Constructeur qui fait une copie du Blokus courant
   * @param toCopy le Blokus à copier
   */
  public Blokus (Blokus toCopy) {
    this.withTeam = toCopy.withTeam;
    this.players = toCopy.players;
    this.turn = toCopy.turn;
    this.trainingMode = toCopy.trainingMode;
    this.board = new Player[20][20];
    updateCalculBlokus(toCopy);
  }


  /***********
   * Setters *
   ***********/

  /** renvoie le tour */
  public void setTurn (int add) { this.turn += add; }

  /** met le tour par défaut */
  public void setPlayerTurn(int playerTurn) { this.playerTurn = playerTurn; }

  /** définit le mode entrainement */
  public void setTrainingMode(boolean train) { this.trainingMode = train; }

/***********
 * Getters *
 ***********/

  /** Récupère le joueur en train de jouer */
  public Player getCurrentPlayer () {
    return players[playerTurn];
  }

  /** Récupère le plateau */
  public Player[][] getBoard (){
    return this.board;
  }

  /** on récupère le nombre de tours déjà fait */
  public int getTurn() {
    return turn;
  }

  /** Récupère le tableau de players trié selon leurs scores */
  public Player[] getPlayersByScore(){
    Player[] score = players.clone();
    for (int i = 1; i < score.length; i++) {
      Player memoire = score[i];
      for (int j = i-1; j >= 0; j--) {
        if(memoire.getScore() > score[j].getScore()){
          score[j+1]=score[j];
          if(j==0) score[j]=memoire;
        }
        else {
          score[i-(i-j-1)]=memoire;
          break;
        }
      }
    }
    return score;
  }

  /** Retourne l'id du joueur courant */
  public int getPlayerTurn () {
    return playerTurn;
  }

  /** Retourne le numéro du joueur à l'indice i */
  public Player getPlayerAt (int i) {
    if(i <= 3 && i >= 0 ){
      return players[i%4];
    }
    return null;
  }

  /**
   * Retourne la pièce du joueur à l'indice i
   * @param id
   * @return
   */
  public Piece getPieceFromID (int id) {
    return players[playerTurn].getPieceAt(id);
  }

  /**
   * uniquement pour le système de score
   * C'est à dire que ça renvoie les points de scores coefficientés s'il y a un
   * joueur réel et que des IAs.
   * @return le score du joueur réel, -1 sinon
   */
  public int getRealPlayerScore() {
    Player[] joueurs = this.getWinner();
    if(withTeam) return -1;
    double score = 1;
    boolean firstPlayer = false;
    boolean onePlayer = true;
    for (int i = 0; i < joueurs.length; i++) {
      if(joueurs[i].isIA()){
        if(firstPlayer) score = score * joueurs[i].getCoef();
      } else {
        if (onePlayer){
          onePlayer = ! onePlayer;
          firstPlayer = true;
          score = score * joueurs[i].getCoef();
        } else { return -1; }
      }
    }
    return (int) score;
  }

  /**
   * Uniquement pour le système de scores
   * @return le nom du joueur réel
   */
  public String getRealPlayerName() {
    for (int i = 0; i < players.length; i++) {
      if (!(players[i].isIA())) { return players[i].getName(); }
    }
    return null;
  }

  /**
   * Permet de renvoyer un tableau de joueurs correspondant
   * au jeu en cours (en équipe ou pas)
   * @return le tableau des joueurs
   */
  public Player[] getWinner (){
    Player[] score;
    if(withTeam){
      Player[] scoreBoard = new Player[2];
      scoreBoard[0] = new Player(colors[0], 1, "Equipe 1 : J1 & J3");
      scoreBoard[0].setScore(players[0].getScore() + players[2].getScore());
      scoreBoard[1] = new Player(colors[1], 2, "Equipe 2 : J2 & J3");
      scoreBoard[1].setScore(players[1].getScore() + players[3].getScore());
      score = scoreBoard;
    } else { score = this.getPlayersByScore(); }
    return score;
  }

  /**
   * Retourne les coordonnées des cases étant en coin avec une pièce du joueur courant
   * et où on peut jouer une pièce
   * @return LinkedList<Point> de toutes ces coordonnés
   */
  public LinkedList<Point> getGoodCase(){
    LinkedList<Point> goodCase = this.getPlayableCase();
    goodCase.removeIf((Point point) -> noPieceOnCase(point));
    return goodCase;
  }

  /**
   * Récupère les coordonnées des cases étant un coin d'une case du joueur courant
   * @return LinkedList<Point> de toutes ces coordonnés
   */
  public LinkedList<Point> getPlayableCase () {
    return this.getPlayableCase(getCurrentPlayer());
  }

  /**
   * Recupère les coordonnées des cases étant un coin d'une case du joueur donné en paramètre
   * @param player joueur dont on veut les cases jouables
   * @return LinkedList<Point> de toutes ces coordonnés
   */
  public LinkedList<Point> getPlayableCase (Player player) {
    LinkedList<Point> tab = new LinkedList<>();
    if (isFirstTurn()) { // Sinon impossible de jouer
      tab.add(getCornerPlayer(player.getId()-1));
    } else {
      for (int i = 0; i < board.length; i++) {
        for (int j = 0; j < board[i].length; j++) {
          if (board[i][j] == player) {
            if (isCorner(i-1, j-1, i, j, player)) {
              tab.add(new Point(j-1, i-1));
            }
            if (isCorner(i+1, j-1, i, j, player)) {
              tab.add(new Point(j-1, i+1));
            }
            if (isCorner(i+1, j+1, i, j, player)) {
              tab.add(new Point(j+1, i+1));
            }
            if (isCorner(i-1, j+1, i, j, player)) {
              tab.add(new Point(j+1, i-1));
            }
          }
        }
      }
    }
    return tab;
  }

/*****************
 * Vérifications *
 *****************/

  /**
   * Vérifie s'il s'agit d'un joueur ou d'une IA
   * @return true si c'est une IA
   */
  public boolean isIA (){ return players[playerTurn].isIA(); }

  /**
   * Vérifie si on joue en mode : 1 Joueur réel contre 3 IA. Pour
   * les classement.
   * @return True si 3 IA, False sinon
   */
  public boolean canBeClassed () {
    int compt = 0;
    for (int i = 0; i < players.length; i++) {
      if (players[i].isIA()) { compt++; }
    }
    return compt == 3;
  }

  /**
   * Permet de savoir s'il s'agit du premier tour
   * @return true si c'est le premier tour
   */
  public boolean isFirstTurn (){ return turn < 1; }

  /**
   * Teste si le jeu termine
   * @return false si un joueur peut encore jouer
   */
  public boolean gameIsFinish() {
    for (Player p : players) { if (p.getCanPlay()) { return false; } }
    if (trainingMode) {
      for (Player player : players) {  player.updateLearning(); }
    }
    return true;
  }

  /**
   * Vérifie si le joueur peut jouer à la position voulue
   * @param message contient la pièce et la position du joueur
   * @return true si le joueur peut placer la pièce à la position définie dans le message
   */
  public boolean verifyPosition (Message message) {
    boolean goodMove;
    Piece currentPiece = message.getPiece();
    currentPiece.updateCoordonate(message.getPoint());
    if (isFirstTurn()) {
      goodMove = isInBoard(currentPiece) && isAtGoodCorner(currentPiece);
    } else {
      goodMove = isInBoard(currentPiece) &&
              isNotOverSomeonePiece(currentPiece) &&
              sidesAvailable(currentPiece) &&
              anglesAvailable(currentPiece);
      currentPiece.initCoordinate();
    }
    return goodMove;
  }

  /**
   * Vérifie si une pièce est jouable avec l'orientation prédéfinie.
   * @param piece pièce qu'on teste
   * @return true si la piece est jouable sur toutes les cases étant des coins, false sinon
   */
  public boolean isPlayablePiece (Piece piece) {
    if (isFirstTurn()) { return true; }
    for (Point p : getPlayableCase())
      if (!(allPositionByPiece(p,piece).isEmpty())) { return true; }
    return false;
  }

  /**
   * Permet de récupérer toutes les positions de la pièce au point a
   * @param a point de référence
   * @param piece pièce à tester
   * @return LinkedList de messages
   */
  public LinkedList<Message> allPositionByPiece (Point a, Piece piece) {
    piece.initCoordinate();
    LinkedList<Message> messages = new LinkedList<>();
    for (Point i : piece.cornerOfPiece()) {
      Point ref = new Point((int)(a.getX()+(2-i.getX())),(int)(a.getY()+(2-i.getY())));
      Piece tmp = (Piece) piece.clone();
      Message test = new Message(ref,tmp,0);
      if (this.verifyPosition(test)) { messages.add(test); }
    }
    return messages;
  }

  /**
   * On regarde si la pièce donnée en paramètre peut être posé de telle façon qu'une des cases
   * de la pièce soit sur le Point 'point'
   * @param point Point en coin d'une des cases du joueur
   * @param piece Pièce qu'on vérifie
   * @return true s'il existe une position, false sinon.
   */
  public boolean haveAGoodPosition(Point point, Piece piece){
    piece.initCoordinate();
    for (Point i : piece.cornerOfPiece()) {
      Point ref = new Point((int)(point.getX()+(2-i.getX())),(int)(point.getY()+(2-i.getY())));
      Piece tmp = (Piece) piece.clone();
      Message test = new Message(ref,tmp,0);
      if (this.verifyPosition(test)) { return true; }
    }
    return false;
  }

  /**
   * Dit si la pièce est dans le plateau
   * @param p la pièce à tester
   * @return true si elle est dedans
   */
  private boolean isInBoard (Piece p){
    for (Point i : p.getPoints()){
      if (outPiece(i)) { return false; }
    }
    return  true;
  }

  /**
   * Teste si la pièce est posée sur une pièce
   * @param p la pièce à poser
   * @return false si la pièce en argument chevauche une pièce sur le plateau
   */
  private boolean isNotOverSomeonePiece(Piece p){
    for (Point i : p.getPoints()){
      if (board[(int)i.getY()][(int)i.getX()] != null) {
        // Inversion x / y à cause des coordonnées du plateau
        return false;
      }
    }
    return  true;
  }

  /**
   * Retourne si la pièce n'a pas de coté interdit
   * @param p la pièce à tester
   * @return true si elle ne touche pas
   */
  private boolean sidesAvailable (Piece p){
    for (Point i : p.getPoints()){
      if (oneSideNext((int)i.getY()+1, (int)i.getX()) ||
              oneSideNext((int)i.getY()-1, (int)i.getX()) ||
              oneSideNext((int)i.getY(), (int)i.getX()+1) ||
              oneSideNext((int)i.getY(), (int)i.getX()-1)) {
        return false;
      }
    }
    return  true;
  }

  /**
   * Vérifie si la pièce peut être mise car il y a un angle en coin avec une autre
   * Piece du joueur courant
   * @param p la pièce à tester
   * @return true si elle peut être placée
   */
  private boolean anglesAvailable (Piece p){
    for (Point i : p.getPoints()){
      if (oneSideNext((int)i.getY()+1, (int)i.getX()+1) ||
              oneSideNext((int)i.getY()-1, (int)i.getX()-1) ||
              oneSideNext((int)i.getY()-1, (int)i.getX()+1) ||
              oneSideNext((int)i.getY()+1, (int)i.getX()-1)) {
        return true;
      }
    }
    return  false;
  }

  /**
   * Vérifie si une des cases de la pièce est sur le coin du joueur (premier tour)
   * @param p la pièce à tester
   * @return true si c'est bon
   */
  public boolean isAtGoodCorner (Piece p){
    for (Point dot : p.getPoints()){
      Point player = getCornerPlayer(playerTurn);
      if (dot.equals(player)) { return true; }
    }
    return false;
  }


/*************
 *   Update  *
 *************/

  /** Actualise la référence au joueur */
  public void updatePlayer (){
    playerTurn = (playerTurn+1)%4;
    turn = (playerTurn == 0)?turn+1:turn;
  }

  /**
   * Met à jour le plateau avec l'action du joueur
   * @param message pièce et position à laquelle le joueur joue
   */
  public void updateBoard (Message message, boolean removable) {
    message.getPiece().updateCoordonate(message.getPoint());
    Player current = players[playerTurn];
    for (Point dot : message.getPiece().getPoints()){
      board[(int)dot.getY()][(int)dot.getX()] = current;
    }
    if (removable) {
      current.removePiece(message.getPiece());
    }
    if(current.getCanPlay()){ updatePlayable(current); }
  }


  /**
   * Modifie si le joueur peut jouer ou non
   * @param current le joueur courant
   */
  private void updatePlayable(Player current){
    for (LinkedList<Piece> liste : current.getPieces()){
      for (Piece p : liste){
        Piece tmp = (Piece) p.clone();
        for (int i = 0; i < 2; i++) {
          for (int j = 0; j < 4; j++) {
            if (isPlayablePiece(tmp)) { current.setCanPlay(true); return; }
            tmp.rotate();
          }
          tmp.mirror();
        }
      }
    }
    current.setCanPlay(false);
  }

  /**
   * Change le tableau du joueur par une copie du joueur
   * @param update le blokus à partir duquel on met à jour
   */
  public void updateCalculBlokus (Blokus update) {
    this.turn = update.turn;
    this.playerTurn = update.playerTurn;
    for (int i = 0 ; i < update.board.length ; i++) {
      for (int j = 0 ; j < update.board[i].length ; j++) {
        this.board[i][j] = update.board[i][j];
      }
    }
  }

/*****************
 *    Actions    *
 *****************/

  /**
   * Enlève la pièce du message sur le Blokus
   * @param m le message avec la pièce
   */
  public void cleanBox (Message m){
    m.getPiece().updateCoordonate(m.getPoint());
    for (Point tmp : m.getPiece().getPoints()){
      board[(int)tmp.getY()][(int)tmp.getX()] = null;
    }
  }


/****************************
 * Fonctions de test simple *
 ****************************/

  /**
   * On cherche s'il y a une pièce de posable sur la case de coordonnées 'point'
   * @param point coordonnées de la case dont on cherche si on peut y poser une pièce
   * @return true s'il n'existe pas de pièce, false sinon.
   */
  private boolean noPieceOnCase(Point point){
    for (LinkedList<Piece> pieceLinkedList : getCurrentPlayer().getPieces()) {
      for (Piece piece : pieceLinkedList) {
        for (int i = 0; i < 2; i++) {
          for (int j = 0; j < 4; j++) {
            if(haveAGoodPosition(point,(Piece) piece.clone())){
              return false;
            }
            piece.rotate();
          }
          piece.mirror();
        }
      }
    }
    return true;
  }

  /**
   * Teste si le point est hors du plateau
   * @param p la pièce à tester
   * @return true si la pièce est en dehors
   */
  private boolean outPiece (Point p) {
    return notInTabLength((int)p.getX()) || notInTabLength((int)p.getY());
  }

  /**
   * Teste si un nombre n'est pas entre 0 et 19
   * @param num le nombre à tester
   * @return true si il est en dehors des bornes
   */
  private boolean notInTabLength (int num) {
    return  num < 0 || num > board.length-1;
  }

  /**
   * Vérifie si la case appartient au joueur
   * @param x coordonnée en x
   * @param y coordonnée en y
   * @return true si la case appartient au joueur
   */
  private boolean oneSideNext (int y, int x) {
    if (notInTabLength(x) || notInTabLength(y)) return  false;
    if (board[y][x] != null) {
      return board[y][x].getColor().equals(players[playerTurn].getColor());
    }
    return false;
  }

  /**
   * vérifie si la case aux coordonnées (abscisse = j, ordonnee = i) est un coin du joueur en paramètre.
   * @param player joueur de référence
   * @param i abscisse de la case dans le board
   * @param j ordonnee de la case dans le board
   * @param iref abscisse d'une case possédant le currentPlayer
   * @param jref ordonnee d'une case possédant le currentPlayer
   * @return la case est un coin : elle a des cases null sur ses cotes
   */
  private boolean isCorner (int i, int j, int iref, int jref, Player player) {
    boolean b = false;
    if (!notInTabLength(j) && !notInTabLength(i)) {
      b = board[i][j] == null
              && (board[i][jref] == null || !(board[i][jref].equals(players[playerTurn])))
              && (board[iref][j] == null || !(board[iref][j].equals(players[playerTurn])));
    }
    return b;
  }


/*************************
 * Retourne informations *
 *************************/

  /**
   * Retourne un point correspondant aux coordonnées du coin
   * @param player le numéro du joueur
   * @return Point de coordonnées
   */
  public Point getCornerPlayer (int player) {
    if (player == 0) { return new Point(0,0); }
    else if (player == 1) { return new Point(board.length-1,0); }
    else if (player == 2) { return new Point(board.length-1, board.length-1); }
    else if (player == 3) { return new Point(0, board.length-1); }
    return null;
  }

  /**
   * Permet de créer les joueurs selon les choix
   * disponibles IA ou joueurs
   * @param i le choix du joueurs
   * @param name le nom du joueur
   * @param id le numéro du joueurs
   * @return un joueur ou une IA correspondant
   */
  private Player initPlayerChoice(int i, String name, int id) {
    switch (i){
      case 0:
        return new Player(colors[id-1], id, name);
      case 1:
        return new IAPlayer(colors[id-1], id,this, new Easy(), name);
      case 2:
        return new IAPlayer(colors[id-1], id,this, new Medium(), name);
      case 3:
        return new IAPlayer(colors[id-1], id,this, new Hard(this), name);
      case 4:
        return new IAPlayer(colors[id-1],id,this, new Learning(this),name);
      default:
        return new IAPlayer(colors[id-1], id,this, new Easy(), name);
    }
  }

  /**
   * On transforme les données des joueurs et leurs scores en un String pour donner une
   * description au jeu actuel
   * @return String de description des joueurs du jeu en cours
   */
  public String gameInformation(){
    String description = "";
    for (int i = 0 ; i < players.length ; i++) {
      if (players[i].getCoef() == 1.25){
        description += "Easy:"+players[i].getName()+":"+players[i].getScore();
      } else if (players[i].getCoef() == 1.5) {
        description += "Medium:"+players[i].getName()+":"+players[i].getScore();
      } else if (players[i].getCoef() == 1.75){
        description += "Hard:"+players[i].getName()+":"+players[i].getScore();
      } else if (players[i].getCoef() == 1){
        description += "Learning:"+players[i].getName()+":"+players[i].getScore();
      } else {
        description += "Player:"+players[i].getName()+":"+players[i].getScore();
      }
      if (i < players.length-1){ description += " ; "; }
    }
    return description;
  }


  /********************
   *  Tests et autres *
   ********************/

  @Override
  public String toString(){
    String s =  this.hashCode() +": Ce plateau contient :\n";
    for(int i = 0 ; i < players.length ; i++){
      s += " - " + players[i].toString() + "\n";
    }
    s += "La référence de jeu est actuellement sur " + playerTurn;
    return s;
  }
}

