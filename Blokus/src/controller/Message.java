package controller;

import model.Piece;

import java.awt.*;

/**
 * Classe Message, elle sert à transmettre les informations de la vue au modè	le
 * @author Blokus_1
 */
public class Message{

	private Point point;
	private Piece piece;
	public static final int TURN = 1;
	public static final int MIRROR = 2;
	private int request;

	/**
	 * Constructeur de Message
	 * @param p coordonnées où l'on pose la pièce sur le board
	 * @param pi pièce qu'on pose
	 * @param r entier correspondant à une action que l'on fait sur la pièce
	 *          (s'il est à 0, on pose la pièce, s'il correspond aux variables static TURN et MIRROR
	 *          on fait l'action correspondante)
	 */
	public Message (Point p, Piece pi, int r) {
		point = p;
		piece = pi;
		request = r;
	}

	/** Récupère le point */
	public Point getPoint() {
		return point;
	}

	/** Récupère la pièce */
	public Piece getPiece() {
		return piece;
	}

	/** Récupère la requête */
	public int getRequest() {
		return request;
	}

	@Override
	public String toString() {
		return point.toString()+" : "+piece.hashCode()+"-- hc : "+piece;
	}

	/**
	 * on traite l'égalité de deux messages si les pièces ont le même id et, si les positions
	 * sont les mêmes.
	 * @param obj le message avec lequel on compare this
	 * @return true si les deux messages sont égaux.
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Message){
			return this.point.equals(((Message) obj).getPoint()) && this.piece.equals(((Message) obj).getPiece());
		}
		return obj == this;
	}
}