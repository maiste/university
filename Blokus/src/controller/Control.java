package controller;

import java.awt.EventQueue;

import model.Blokus;
import model.Piece;
import model.players.Player;
import view.GameView;
import view.graphic.GraphicView;
import view.terminal.TerminalView;


/**
 * Classe du contrôleur de jeu
 * @author Blokus_1
 */
public class Control{

  private Blokus game;
  private GameView view;

  /**
   * Crée la GameView view, et met le Blokus à null, la view crée elle même le blokus selon les
   * paramètres passés par le joueur
   */
  public Control (boolean graphic) {
    if (graphic) {
      EventQueue.invokeLater(() -> view = new GraphicView(this));
    }	else { view = new TerminalView(this); }
  }

  /** Retourne l'attribut du jeu */
  public Blokus getGame() {	return game; }

  /** Retourne l'attribut vue du jeu*/
  public GameView getView() {	return view;  }

  /** Retourne le classement des joueurs */
  public Player[] getClassement(){ return game.getWinner(); }

  /** Change le jeu associé */
  public void setGame(Blokus g) { game = g; }

  /**
   * Informe si le joueur courant est une IA ou un joueur réel
   * @return true si c'est une IA
   */
  public boolean isIA(){  return game.isIA(); }

  /**
   * Envoie un message de la vue au modèle lui demandant d'agir sur la pièce contenue dans le Message
   * @param message Message à transmettre au Blokus
   * @return true si la pièce a été modifiée ou non
   */
  public boolean sendMessage(Message message){
    if(message == null || message.getPiece()==null) return false;
    if (message.getPoint() == null) {
      switch (message.getRequest()){
        case Message.TURN : message.getPiece().rotate();break;
        case Message.MIRROR: message.getPiece().mirror();break;
        default:break;
      }
      return false;
    }
    else {
      if (game.verifyPosition(message)){
        game.updateBoard(message,true);
        return true;
      }
      return false;
    }
  }

  /**
   * Regarde s'il existe une position où la pièce peut être posée
   * @param piece1 piece dont on veut savoir si elle peut être posée
   * @return true s'il existe au moins une possibilité, false sinon
   */
  public boolean canPlayPiece(Piece piece1){
    if(piece1 == null){
      return false;
    }
    else {
      Piece piece = (Piece) piece1.clone();
      for (int i = 0; i < 2; i++) {
        for (int j = 0; j < 4; j++) {
          if (game.isPlayablePiece(piece)) { return true; }
          piece.rotate();
        }
        piece.mirror();
      }
      return false;
    }
  }


}