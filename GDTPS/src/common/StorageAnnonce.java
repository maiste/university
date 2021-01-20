package common;

import common.Annonce;
import common.Domaine;

import java.util.TreeMap;
import java.util.Hashtable;
import java.util.Set;
import java.util.LinkedList;

/**
 * Class to represent the storage of informations
 * This class is a singleton
 *
 * @author
 */
public class StorageAnnonce {
	private TreeMap<Domaine.DomaineType, Hashtable<String, Annonce>> storage;
	private static StorageAnnonce singleton = null;

	private StorageAnnonce (){
		storage = new TreeMap<Domaine.DomaineType, Hashtable<String, Annonce>>();
		for(Domaine.DomaineType d : Domaine.DomaineType.values()) {
			storage.put(d, new Hashtable<String, Annonce>(256));
		}

	}

	/**
	 * Return the storage object
	 */
	public static synchronized StorageAnnonce getStore() {
		if(singleton == null) {
			singleton = new StorageAnnonce();
		}
		return singleton;
	}

	/**
	 * Find an anounce thanks to the domain
	 *
	 * @param domaine the domain
	 * @param idAnnonce the id of the anounce
	 * @return an Annonce if it finds it else null
	 */
	public synchronized Annonce findWithDomain(String domaine, String idAnnonce) {
		Domaine.DomaineType d = Domaine.fromString(domaine);
		if(storage.containsKey(d)) {
			return storage.get(d).get(idAnnonce);
		}
		return null;
	}

	/**
	 * Find an anounce everywhere in the database
	 *
	 * @param idAnnonce the id of the anounce
	 * @return the Annonce if it finds it else null
	 */
	public synchronized Annonce find(String idAnnonce) {
		for(Hashtable<String, Annonce> index : storage.values()) {
			Annonce anc = index.get(idAnnonce);
			if(anc != null) {
				return anc;
			}
		}
		return null;
	}

	/**
	 * Add an anounce to the store
	 *
	 * @param anc the anounce to add
	 * @return true if ti works else false
	 */
	public synchronized boolean addAnnonce(Annonce anc) {
		Domaine.DomaineType d = null;
		try {
			d = Domaine.fromString(anc.getDomaine());
		} catch(IllegalArgumentException e) {
			return false;
		}
		if(storage.containsKey(d)) {
			storage.get(d).put(anc.getId(), anc);
			return true;
		}
		return false;
	}

	/**
	 * Delete an anounce form the store
	 *
	 * @param anc the anounce
	 * @return true if it works else false
	 */
	public synchronized boolean deleteAnnonce(Annonce anc) {
		Domaine.DomaineType d = null;
		try {
			d = Domaine.fromString(anc.getDomaine());
		} catch(IllegalArgumentException e) {
			return false;
		}
		if(storage.containsKey(d)) {
			storage.get(d).remove(anc.getId());
		}
		return true;
	}

	/**
	 * Retrieve anounces from a domain
	 *
	 * @param d the domain type
	 * @return a String array if it works else null
	 */
	public synchronized String[] getAncFromDomaine(Domaine.DomaineType d) {
		Hashtable<String, Annonce> ancs = storage.get(d);
		LinkedList<Annonce> res = new LinkedList<Annonce>();
		int length = 0;
		if(ancs == null) return null;
		for(Annonce anc : ancs.values()) {
			res.push(anc);
			length++;
		}
		String[] args = new String[length*5];
		for(int i = 0 ; i < args.length ; i=i+5) {
			String[] argsAnc = res.pop().toStringArgs();
			args[i] = argsAnc[0];
			args[i+1] = argsAnc[1];
			args[i+2] = argsAnc[2];
			args[i+3] = argsAnc[3];
			args[i+4] = argsAnc[4];
		}
		return args;
	}

	/**
	 * Get the domain list
	 *
	 * @return a String array containing all the domains
	 */
	public synchronized String[] getDomaines() {
		Set<Domaine.DomaineType> domaines = storage.keySet();
		String[] args = new String[domaines.size()];
		int i = 0;
		for(Domaine.DomaineType domaine : domaines) {
			args[i] = domaine.toString();
			i++;
		}
		return args;
	}

	/**
	 * Retrieve user anounce from the store
	 *
	 * @param user the username
	 * @return an array of the anounces
	 */
	public synchronized String[] getUserAnc(String user) {
		LinkedList<Annonce> userAnc = new LinkedList<Annonce>();
		int length = 0;
		for(Hashtable<String, Annonce> domaine : storage.values()) {
			for(Annonce anc : domaine.values()) {
				if(anc.getUser().equals(user)) {
					userAnc.push(anc);
					length++;
				}
			}
		} 
		String[] args = new String[length*5];
		for(int i = 0 ; i < args.length ; i=i+5) {
			String[] argsAnc = userAnc.pop().toStringArgs();
			args[i] = argsAnc[0];
			args[i+1] = argsAnc[1];
			args[i+2] = argsAnc[2];
			args[i+3] = argsAnc[3];
			args[i+4] = argsAnc[4];
		}
		return args;
	}
}
