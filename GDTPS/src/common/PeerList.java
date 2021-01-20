package common;

import java.util.TreeMap;
import java.util.Hashtable;
import java.net.InetSocketAddress;
import java.util.Set;
import common.Logs;

/**
 * Class to manage peers
 *
 * @author Marais-Viau
 */
public class PeerList {
	private static PeerList peerList                       = null;
	private TreeMap<String, Long> cache                    = null;
	private Hashtable<String, InetSocketAddress> addresses = null;
	private final static int TIME_REMOVE  = 3_600_000; // 1H

	private PeerList() {
		cache = new TreeMap<String, Long>();
		addresses = new Hashtable<String, InetSocketAddress>(256);
	}

	/**
	 * Get or create a unique instance of PeerList
	 *
	 * @return the PairList instance
	 */
	public static synchronized PeerList get() {
		if(peerList == null) {
			peerList = new PeerList();
			new GC().start();
		}
		return peerList;
	}

	private void updateTime(String peer) {
		cache.put(peer, System.currentTimeMillis());
	}

	/**
	 * Adds or updates the value for a peer
	 *
	 * @param peer the name of the peer
	 * @param addr address of the peer
	 * @return true if it's added, else false
	 */
	public synchronized boolean addOrUpdate(String peer, InetSocketAddress addr) {
		if(cache.get(peer) != null) {
			updateTime(peer);
			return false;
		}
		if(addr != null) {
			cache.put(peer, System.currentTimeMillis());
			addresses.put(peer, addr);
		}
		return true;
	}

	/**
	 * Returns the peers connected with the client
	 *
	 * @return the set of peers
	 */
	public Set<String> getPeerList() {
		return cache.keySet();
	}

	/**
	 * Return the inet address of the peer
	 *
	 * @param peer the name of the peer
	 * @return the address or null if it's not found
	 */
	public synchronized InetSocketAddress getIp(String dest)  {
		if(dest != null) {
			return addresses.get(dest);
		}
		return null;
	}

	private synchronized void removeOld() {
		long reference = System.currentTimeMillis();
		for(String dest : cache.keySet()) {
			long timestamp = cache.get(dest);
			if(timestamp+TIME_REMOVE > reference) {
				cache.remove(dest);
				addresses.remove(dest);
			}
		}
	}

	private static class GC extends Thread {
		public void run() {
			while(true) {
				try {
					Logs.log("Garbage collection.");
					Thread.sleep(TIME_REMOVE);
				} catch(InterruptedException e) {
					Logs.error("Garbage collector down -> exit.");
					System.exit(1);
				}
				peerList.removeOld();
			}
		}
	}
}
