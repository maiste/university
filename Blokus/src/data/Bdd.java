package data;

import model.Matrix;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.LinkedList;

/**
 * Cette classe implémente la connexion avec la BDD et gère également les
 * interactions avec celle-ci.
 * Elle contient des méthodes pour :
 * - Gérer le score des utilisateurs
 * - Gérer les sauvegardes d'objets
 */
public class Bdd {

	/** représente la connexion a la BDD */
	private Connection conn;

	
	/** Initialise la connexion avec la BDD	 */
	public void connectToBDD() throws SQLException {
		String url = "jdbc:postgresql://127.0.0.1:5432/blokus";
		String user = "blokus";
		String mdp = "blokus1";
		conn = DriverManager.getConnection(url, user, mdp);
	}

	/** Ferme la connexion avec la BDD si celle ci existe */
	public void closeConnection() {
		if (conn != null) {
			try { 
				conn.close(); 
				conn = null;
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	/* *******************************************************************************
		MÉTHODES DE GESTION DES SCORES :
	 ******************************************************************************* */

	/**
	 * Return le score des meilleurs joueurs au nombre de limite (argument)
	 * @param limite nombre des meilleurs joueurs à afficher
	 * @return liste de scores sous la forme (joueur i, score i, joueur j, score j...)
	 */
	public LinkedList<String> printAllScores(int limite) throws SQLException {
		LinkedList<String> res = new LinkedList<>();
		Statement state = conn.createStatement();
		ResultSet result = state.executeQuery("SELECT * FROM scores ORDER BY score DESC LIMIT " + limite);
		ResultSetMetaData resultMeta = result.getMetaData(); // récupère toute les données
		while (result.next()) {
			for (int i = 1; i <= resultMeta.getColumnCount(); i++) {
				res.add(result.getObject(i).toString());
			}
		}
		result.close();
		state.close();
		return res;
	}
	
	/**
	 * Crée un nouveau score pour le pseudo si le pseudo n'existe pas
	 * si le score est le meilleur score pour le joueur pseudo, il le remplace
	 * sinon, il ne fait rien.
	 * @param pseudo nom de l'utilisateur
	 * @param score score à sauvegarder
	 * @return true si le score a été sauvegardé, false sinon
	 */
	public boolean updateScore(String pseudo, int score) {
		try {
			pseudo = pseudo.toLowerCase();
			if (!playerAlreadyExist(pseudo)) {
				Statement state = conn.createStatement();
				state.executeUpdate("INSERT INTO scores VALUES('" + pseudo + "','" + score + "')");
				state.close();
				return true;
			}
			else if (isBestScore(pseudo, score)) {
				Statement state = conn.createStatement();
				state.executeUpdate("UPDATE scores SET score=" + score + " WHERE pseudo='" + pseudo + "'");
				state.close();
				return true;
			}
			else
				return false;
		} 
		catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Renvoie si le joueur pseudo existe déjà dans la BDD
	 * @param pseudo joueur à vérifier
	 * @return true si il a deja un score, false sinon
	 */
	private boolean playerAlreadyExist(String pseudo) {
		try {
			Statement state = conn.createStatement();
			ResultSet result = state.executeQuery("SELECT * FROM scores WHERE pseudo='" + pseudo + "'");
			if (!result.next()) return false;
			result.close();
			state.close();
			return true;
		}
		catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Renvoie si le score passé en paramètre est meilleur que le score actuel du joueur pseudo
	 * @param pseudo joueur pour qui on veut vérifier le score
	 * @param score score que le joueur vient de faire
	 * @return true si le paramètre est meilleur, false sinon
	 */
	private boolean isBestScore(String pseudo, int score) {
		try {
			Statement state = conn.createStatement();
			ResultSet result = state.executeQuery("SELECT * FROM scores WHERE pseudo='" + pseudo + "'");
			int scoreActu = -1;
			if (result.next())
				scoreActu = Integer.parseInt(result.getObject(2).toString());
			result.close();
			state.close();
			if (score > scoreActu)
				return true;
			else 
				return false;
		}
		catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/* *******************************************************************************
		MÉTHODES DE GESTION DES SAUVEGARDES :
	 ******************************************************************************* */

	/**
	 * On enregistre dans la base de données un tableau de Matrix
	 * @param mat tableau de Matrix qu'on enregistre
	 * @param score description du tableau de Matrix
	 */
    public void saveMatrixtoBDD(Matrix [] mat, String score) {
    	saveOneMatrixToBDD(mat,score);
    }

	/**
	 * On récupère le dernier tableau de Matrix qu'on a stocké dans la BDD
	 * @return dernier tableau de Matrix qu'on a stocké dans la BDD
	 */
	public Matrix [] getMatrixFromBDD () {
		return getOneMatrixFromBDD(getLastIdFromBDD());
    }

	/**
	 * Sauvegarde d'un objet de matrix avec une description score dans la BDD
	 * @param o objet qu'on stocke dans la BDD
	 * @param score description de l'objet de Matrix
	 */
	private void saveOneMatrixToBDD (Object o, String score) {
    	try {
			connectToBDD();
			byte [] save = Serialization.saveObject(o); // tableau de byte de l'objet
			String saveBdd = Base64.getEncoder().encodeToString(save); // String qu'on va stocker dans la BDD
			Statement state = conn.createStatement();
			state.executeUpdate("INSERT INTO matrices(matrice, score) VALUES('" + saveBdd + "','" + score + "')");
			state.close();
			closeConnection();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
        }
	/**
	 * Sauvegarde d'un objet de matrix avec une description score dans la BDD
	 * @param o objet qu'on stocke dans la BDD
	 * @param score description de l'objet de Matrix
	 */
	public void saveOneMatrixToBDDInit (Object o, String score) {
    	try {
			connectToBDD();
			byte [] save = Serialization.saveObject(o); // tableau de byte de l'objet
			String saveBdd = Base64.getEncoder().encodeToString(save); // String qu'on va stocker dans la BDD
			Statement state = conn.createStatement();
			state.executeUpdate("INSERT INTO matrices(id,matrice, score) VALUES(0,'" + saveBdd + "','" + score + "')");
			state.close();
			closeConnection();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
        }

    

	/**
	 * On vide la table de Matrix
	 */
	public void removeAllMatrix() {
		try {
			connectToBDD();
			Statement state = conn.createStatement();
			state.executeUpdate("DELETE FROM matrices");
			state.close();
			closeConnection();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Permet de récupérer un Object dans la BDD à partir de son id
	 * @param id id de la matrice à récupérer
	 * @return On retourne le tableau de Matrix ayant pour id
	 */
	private Matrix[] getOneMatrixFromBDD (int id) {
		try {
			connectToBDD();
			Statement state = conn.createStatement();
			ResultSet result = state.executeQuery("SELECT matrice FROM matrices WHERE id = '" + id + "'");
			result.next(); // on se place sur le résultat
			String obj = result.getObject(1).toString(); // on récupère la String
			byte[] nouv = Base64.getDecoder().decode(obj); // on convertit en byte[]
			result.close();
			state.close();
			closeConnection();
			return (Matrix[])Serialization.getObject(nouv); // on retourne l'object extrait du byte[]
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Renvoie l'id qu'on doit donner à la prochaine matrice,
	 * @return id qu'on doit donner à la prochaine matrice, -1 si la connexion échoue
	 */
	private int getLastIdFromBDD () {
		try {
			connectToBDD();
			Statement state = conn.createStatement();
			ResultSet result = state.executeQuery("SELECT max(id) FROM matrices");
			result.next(); // on se place sur le résultat
			int res = Integer.parseInt(result.getObject(1).toString()); // on récupère la String
			return res;
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;
	}

	/**
	 * main pour vider la table de Matrix
	 * @param args pas d'importance
	 */
	public static void main (String [] args) {
		Bdd b = new Bdd();
		b.removeAllMatrix();
	}

}
