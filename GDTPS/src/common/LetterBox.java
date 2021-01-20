package common;

import common.Message;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Class to manage in and out Messages
 *
 * @author Marais-Viau
 */
public class LetterBox {
	private static LetterBox box                                = null;
	private ConcurrentHashMap<String, LinkedList<Message>> received       = null;
	private ConcurrentHashMap<Long, MessageSender>  sent = null;
	private final static int MAX_ATTEMPT                        = 3;

	private LetterBox() {
		this.received = new ConcurrentHashMap<String, LinkedList<Message>>();
		this.sent = new ConcurrentHashMap<Long, MessageSender>();
	}
	
	/**
	 * Create or get a unique instance of LetterBox
	 *
	 * @return a unique LetterBox
	 */
	public static synchronized LetterBox get() {
		if(box == null) {
			box = new LetterBox();
		}
		return box;
	}

	/**
	 * Adds a Message to someone into the sending list
	 *
	 * @param m the message sent
	 * @return true if it adds it with succeed
	 */
	public synchronized boolean addToSendingList(Message m) {
		String[] args = m.getArgs();
		if(args != null && args.length >= 2) {
			try {
				long timestamp = Long.parseLong(args[1]);
				sent.put(timestamp, new MessageSender(m));
				Logs.log("Insert new message -> Box");
				return true;
			} catch(NumberFormatException e) {
				Logs.warning("Can't parse message timestamp.");
			}
		}
		return false;
	}

	/**
	 * Get the list of the messages available for sending
	 *
	 * @return the list containing all of the message
	 */
	public synchronized LinkedList<Message> getSendingList() {
		long timestamp = System.currentTimeMillis();
		LinkedList<Message> toSend = new LinkedList<Message>();
		LinkedList<Long> toRemove = new LinkedList<Long>();
		for(long timestampKey : sent.keySet()) {
			MessageSender message = sent.get(timestampKey);
			if(message.next < timestamp) {
				toSend.add(message.message);
			}
			message.updateAttempt();
			if(message.attempt >= MAX_ATTEMPT) {
				toRemove.add(timestampKey);
				continue;
			}
		}
		for(long rmTimestamp : toRemove) {
			sent.remove(rmTimestamp);
		}
		return toSend;
	}


	/**
	 * Removes a message from the list of messages to send
	 *
	 * @param timestamp the date when the message was first sent
	 * @param addr the addr where it has been sent
	 * @return true if it acks the message else false
	 */
	public synchronized boolean ackMsg(Long timestamp, String dest) {
			MessageSender sender = sent.get(timestamp);
			if(sender != null) {
				sent.remove(timestamp);
				Logs.log("Remove new message for " + timestamp + " -> ack");
				return true;
			}
		return false;
	}

	/**
	 * Inserts a message received into the letter box
	 *
	 * @param src the source from whom you received the message
	 * @param m the message you received
	 */
	public synchronized void insertInLetterBox(String src, Message m) {
		LinkedList<Message> messages = received.get(src);
		if(messages == null) {
			messages = new LinkedList<Message>();
			received.put(src, messages);
		}
		Logs.log("Insert new message for " + src);
		messages.add(m);
	}

	/**
	 * Gets the list of the message for a certain user
	 *
	 * @param sender the name of the person who sent the messages
	 * @return the list of all the messages received, return null if empty
	 */
	public synchronized LinkedList<Message> getNewMsgFor(String sender) {
		LinkedList<Message> messages = received.get(sender);
		if(received.size() > 0) {
			received.remove(sender);
		}
		return messages;
	}

	/**
	 * Gets the list of all the messages received
	 *
	 * @return the list of all the messages received, return null if empty
	 */
	public synchronized LinkedList<Message> getAllNewMsg() {
		LinkedList<Message> messages = new LinkedList<Message>();
		for(String sender: received.keySet()) {
			LinkedList<Message> senderMsg = received.get(sender);
			if(senderMsg != null && received.size() > 0) {
				messages.addAll(senderMsg);
				received.remove(sender);
			}
		}
		return messages;
	}

	private class MessageSender {
		int attempt = 0;
		long next = 0;
		Message message = null;

		MessageSender(Message m) {
			this.attempt = 0;
			this.next = System.currentTimeMillis();
			this.message = m;
		}

		void updateAttempt() {
			this.attempt++;
			this.next = System.currentTimeMillis() + (attempt*2);
		}
	}

}
