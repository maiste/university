package common;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Random;

/**
 * Class to describe users
 * It's a singleton class to avoid duplication.
 *
 * @author Marais-Viau
 */
public class Index {
	private static Index index = null;
	private Hashtable<String, String> users = null;
	private Hashtable<String, String> cache = null;

	private Index() {
		cache = new Hashtable<>(256);
		users = new Hashtable<>(256);
	}

	/**
	 * General constructor
	 *
	 * @return the index
	 */
	public synchronized static Index getIndex() {
		if (index != null) {
			return index;
		} else {
			index = new Index();
			return index;
		}
	}

	/**
	 * Build a new token for a user
	 *
	 * @param user the username
	 * @return a String that represents the token
	 */
	private synchronized String buildToken(String user) {
		String number = Integer.toString((new Random()).nextInt(10000));
		String token = user + user.hashCode() + number;
		while (users.contains(token)) {
			number = Integer.toString((new Random()).nextInt(10000));
			token = user + user.hashCode() + number;
		}
		return token;
	}

	/**
	 * Add a user to the index
	 *
	 * @param user the username
	 * @param ip the ip of the user
	 * @return false if the index aready contains the user
	 */
	public synchronized boolean addUser(String user, String ip) {
		if (users.containsKey(user))
			return false;
		String token = buildToken(user);
		users.put(user, token);
		cache.put(user, ip);
		return true;
	}

	/**
	 * Put the ip into the cache
	 * If the user is already register, it will wipe the previous value
	 *
	 * @param user the username
	 * @param ip the ip
	 */
	public synchronized void updateIp(String user, String ip) {
		cache.put(user, ip);
	}

	/**
	 * Initialize a new token for a user and add it to the index
	 *
	 * @param user the username
	 * @return the token
	 */
	public synchronized String initNewToken(String user) {
		String token = buildToken(user);
		users.put(user, token);
		return token;
	}

	/**
	 * Remove a user from the cache
	 *
	 * @param user the username
	 */
	public synchronized void removeUser(String user) {
		cache.remove(user);
	}

	/**
	 * Erease a user from the index
	 *
	 * @param user the username
	 */
	public synchronized void ereaseUser(String user) {
		removeUser(user);
		users.remove(user);
	}

	/**
	 * Get the user token
	 *
	 * @param user the username
	 * @return the token or null
	 */
	public synchronized String getToken(String user) {
		return users.get(user);
	}

	/**
	 * Get the ip of the user
	 *
	 * @param user the username
	 * @return the ip or null
	 *
	 */
	public synchronized String getIpFromUser(String user) {
		return cache.get(user);
	}

	/**
	 * Get the user from his ip
	 *
	 * @param ip the user ip address
	 * @return the user or null
	 */
	public synchronized String getUserFromIp(String ip) {
		Enumeration<String> userKeys = cache.keys();
		while (userKeys.hasMoreElements()) {
			String user = userKeys.nextElement();
			if (cache.get(user).equals(ip)) {
				return user;
			}
		}
		return null;

	}

	/**
	 * Get the user from his token
	 *
	 * @param token the user token
	 * @return the user or null
	 */
	public synchronized String getUserFromToken(String token) {
		Enumeration<String> userKeys = users.keys();
		while (userKeys.hasMoreElements()) {
			String user = userKeys.nextElement();
			if (users.get(user).equals(token)) {
				return user;
			}
		}
		return null;
	}

	/**
	 * Check if the token exists
	 *
	 * @param token the user token
	 * @return true if it's valid, false otherwise
	 */
	public synchronized boolean isValidToken(String token) {
		Enumeration<String> userKeys = users.keys();
		while (userKeys.hasMoreElements()) {
			String currentUser = userKeys.nextElement();
			if (users.get(currentUser).equals(token)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if the user already exists
	 *
	 * @param user the username
	 * @return true if it exists, false otherwise.
	 */
	public synchronized boolean isValidUser(String user) {
		return users.containsKey(user);
	}
}
