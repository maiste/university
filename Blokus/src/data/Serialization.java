package data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Cette classe possède deux méthodes statiques permettant de : 
 * - Convertir un Object en byte [] stockable dans la BDD
 * - Convertir un byte [] récupèrable depuis la BDD en Object utilisable
*/

public class Serialization {
	/**
	 * Convertit un objet en byte[] sérialisé
	 * @param o Object à sauvegarder
	 * @return le byte[] à stocker dans la BDD.
	 */
	public static byte[] saveObject(Object o) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(o);
			oos.flush();
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bos.toByteArray();
	}

	/**
	 * Convertit un objet sérialisé (byte[]) en Object
	 * @param tab tableau représentant la sauvegarde
	 * @return l'object récupéré tel qu'il l'était avant la sauvegarde
	 */
	public static Object getObject(byte[] tab) {
		Object res = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(tab));
			res = ois.readObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}
}
