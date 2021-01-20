
package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

import common.Index;
import common.Logs;
import common.Message;
import common.StorageAnnonce;
import common.Domaine;
import common.Annonce;

/**
 * Class to handle Server request
 *
 * @author Marais-Viau
 */
public class Handler extends Thread {
	private final static int TIMEOUT = 43_200_000; // 12h
	private Socket s = null;
	private BufferedReader in = null;
	private PrintWriter out = null;
	private boolean wantAnExit = false;

	private String addr = null;
	private String name = null;
	private Index index = null;
	private StorageAnnonce store = null;

	/**
	 * Constructor
	 *
	 * @param s the socket to communicate with the client
	 */
	public Handler(Socket s) {
		if (s != null) {
			this.s = s;
			this.addr = s.getInetAddress().toString();
			setInAndOut();
			this.index = Index.getIndex();
			this.store = StorageAnnonce.getStore();
			Logs.log("Socket bind to a new thread -> manage the new connection for " + addr);
		} else {
			Logs.error("Socket s is null -> exit the Thread for " + addr);
			wantAnExit = true;
		}
	}

	private void setInAndOut() {
		try {
			this.in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			this.out = new PrintWriter(s.getOutputStream());
			this.s.setSoTimeout(TIMEOUT);
		} catch (IOException e) {
			Logs.error("Can't set input and output flux for -> exit the thread for " + addr);
			wantAnExit = true;
		}
	}

	private boolean notConnected() {
		if(name == null) {
			Message m = new Message(Message.MessageType.NOT_CONNECTED);
			write(m);
			Logs.warning("Access without connection from " + addr);
		}
		return name == null;
	}

	private void write(Message m) {
		String packet = m.toNetFormat();
		out.print(packet);
		out.flush();
	}

	private Message read() {
		String buffer = "";
		String data = "";
		try {
			while ((data = in.readLine()) != null && !data.equals(".")) {
				buffer += (data + "\n");
			}
			if (data == null) {
				Logs.error("Client has been disconnected for " + addr + " -> closing connection");
				wantAnExit = true;
				return null;
			}
			buffer = buffer.trim();
			return Message.stringToMessage(buffer);
		} catch (IllegalArgumentException e) {
			Logs.warning("Can't Handle the format for " + addr + " -> drop message");
			return null;
		} catch (NullPointerException e) {
			Logs.warning("Write on a close channel for " + addr + " -> drop channel");
			wantAnExit = true;
			return null;
		} catch(SocketTimeoutException e) {
			Logs.warning("Socket timeout for " + addr + " -> closing connection");
			wantAnExit = true;
			return null;
		} catch (IOException e) {
			Logs.warning("IOException for " + addr + " -> closing connection");
			wantAnExit = true;
			return null;
		}
	}

	private void disconnect() {
		if (s != null) {
			try {
				if(name != null) {
					index.removeUser(name);
				}
				s.close();
				Logs.log("Thread socket closed for " + addr);
			} catch (IOException e) {
				Logs.warning("Failed to close the thread socket -> exit thread");
			}
		} else {
			Logs.warning("Thread socket already closed for " + addr);
		}
	}

	private void sendConnect(String[] args, boolean newUser, boolean send) {
		Message msg = null;
		if(send) {
			if (newUser) {
				msg = new Message(Message.MessageType.CONNECT_NEW_USER_OK, args);
			} else {
				msg = new Message(Message.MessageType.CONNECT_OK, args);
			}
			Logs.log("Connection complete with " + name + " on " + addr);
		} else  {
			if(newUser) {
				msg = new Message(Message.MessageType.CONNECT_NEW_USER_KO);
			} else {
				msg = new Message(Message.MessageType.CONNECT_KO);
			}
			Logs.warning("Connection failed with " + addr + " -> retrying with new method");
		}
		write(msg);
	}

	private void connect(Message m) {
		String[] args = m.getArgs();
		boolean send = true, newUser = false;
		if (args == null || args.length != 1 || args[0] == null) {
			send = false;
		} else {
			String [] argSend = { "" };
			if (args[0].charAt(0) == '#') { // ------------ Token
				argSend = new String[2];
				argSend[0] = args[0].substring(1);
				if(index.isValidToken(argSend[0])) {
					name = index.getUserFromToken(argSend[0]);
					if (name == null) { send = false; }
					argSend[1] = name;
					index.updateIp(name, addr);
				} else { send = false; }
			} else { // ------------------------------------ User
				name = args[0];
				if(index.isValidUser(name)) {
					argSend[0] = index.getToken(name);
					if(argSend[0] == null) { argSend[0] = index.initNewToken(name); }
					index.updateIp(name, addr);
				} else {
					newUser = true;
					if(index.addUser(name, addr)) {
						argSend[0] = index.getToken(name);
					} else {
						send = false;
					}
				}
			}
			sendConnect(argSend, newUser, send);
		}
	}

	private void unknown(Message m) {
		Message unknown = new Message(Message.MessageType.UNKNOWN_REQUEST);
		write(unknown);
		Logs.warning("Unknown header for " + addr + " -> skipping\n" + m);
	}

	private void postAnc(Message m) {
		if(notConnected()) return;
		String[] args = m.getArgs();
		Message response = null;
		if(args != null && args.length == 4) {
			try {
				Annonce anc = new Annonce(name, args[0], args[1], args[2], args[3]);
				if(store.addAnnonce(anc)) {
					String[] argSent = { anc.getId() };
					response = new Message(Message.MessageType.POST_ANC_OK, argSent);
					Logs.log("Create a new anc for " + addr + " with " + name);
				}
			} catch(IllegalArgumentException e) {}
		}
		if(response == null) {
			Logs.warning("Failed posting new anc from " + addr + " with " + name);
			response = new Message(Message.MessageType.POST_ANC_KO);
		}
		write(response);
	}

	private void majAnc(Message m) {
		if(notConnected()) return;
		String[] args = m.getArgs();
		Message response = null;
		if(args != null && args.length > 0) {
			Annonce anc = store.find(args[0]);
			if(anc != null && anc.getUser().equals(name)) {
				if(anc.updateWithArgs(args)) {
					String[] argSent = { anc.getId() };
					response = new Message(Message.MessageType.MAJ_ANC_OK, argSent);
					Logs.log("Update anc for " + addr + " with " + name);
				}
			}
		}
		if(response == null) {
			response = new Message(Message.MessageType.MAJ_ANC_KO);
			Logs.warning("Failed updating anc for " + addr + " with " + name);
		}
		write(response);
	}

	private void deleteAnc(Message m) {
		if(notConnected()) return;
		String[] args = m.getArgs();
		Message response = null;
		if(args != null && args.length == 1) {
			Annonce anc = store.find(args[0]);
			if(anc != null && anc.getUser().equals(name)) {
				if(store.deleteAnnonce(anc)) {
					String[] argSent = { anc.getId() };
					response = new Message(Message.MessageType.DELETE_ANC_OK);
					Logs.log("Delete anc for " + addr + " with " + name);
				}
			}
		}
		if(response == null) {
			response = new Message(Message.MessageType.DELETE_ANC_KO);
			Logs.warning("Failed deleting anc for " + addr + " with " + name);
		}
		write(response);
	}

	private void requestDomain() {
		if(notConnected()) return;
		String[] argsSent = store.getDomaines();
		Message response = null;
		if(argsSent.length > 0) {
			response = new Message(Message.MessageType.SEND_DOMAINE_OK, argsSent);
			Logs.log("Send domains to " + addr + " with " + name);
		} else {
			response = new Message(Message.MessageType.SEND_DOMAIN_KO);
			Logs.warning("Failed sending domains to " + addr + " with " + name);
		}
		write(response);
	}

	private void requestAnc(Message m) {
		if(notConnected()) return;
		String[] args = m.getArgs();
		Message response = null;
		if(args != null && args.length == 1) {
			try {
				Domaine.DomaineType d = Domaine.fromString(args[0]);
				String[] argsSent = store.getAncFromDomaine(d);
				if(argsSent != null) {
					response = new Message(Message.MessageType.SEND_ANC_OK, argsSent);
					Logs.log("Request anc for " + addr + " with " + name);
				}
			} catch(IllegalArgumentException e) {}
		}
		if(response == null) {
			response = new Message(Message.MessageType.SEND_ANC_KO);
			Logs.warning("Failed requesting anc for " + addr + " with " + name);
		}
		write(response);
	}

	private void requestOwnAnc() {
		if(notConnected()) return;
		String[] argsSent = store.getUserAnc(name);
		Message response = null;
		if(argsSent != null) {
			response = new Message(Message.MessageType.SEND_OWN_ANC_OK, argsSent);
			Logs.log("Request own anc for " + addr + " with " + name);
		} else {
			response = new Message(Message.MessageType.SEND_OWN_ANC_KO);
			Logs.warning("Failed requesting own anc for " + addr + " with " + name);
		}
		write(response);
	}

	private void requestIp(Message m) {
		if(notConnected()) return;
		String[] args = m.getArgs();
		Message response = null;
		if(args != null && args.length == 1) {
			Annonce anc = store.find(args[0]);
			if(anc != null) {
				String ip = index.getIpFromUser(anc.getUser());
				if(ip != null) {
					String[] argsSent = new String[2];
					argsSent[0] = ip.substring(1);
					argsSent[1] = anc.getUser();
					response = new Message(Message.MessageType.REQUEST_IP_OK, argsSent);
					Logs.log("Request IP for " + addr + " with " + name + "-> " + argsSent[1] + "@" + argsSent[0]);
				}
			}
		}
		if(response == null) {
			response = new Message(Message.MessageType.REQUEST_IP_KO);
			Logs.warning("Failed requesting IP for " + addr + " with " + name);
		}
		write(response);
	}

	private void handler(Message m) {
		switch (m.getType()) {
			case CONNECT:
				connect(m);
				break;
			case DISCONNECT:
				wantAnExit = true;
				Logs.log("Ask for deconnection for " + addr);
				break;
			case POST_ANC:
				postAnc(m);
				break;
			case MAJ_ANC:
				majAnc(m);
				break;
			case DELETE_ANC:
				deleteAnc(m);
				break;
			case REQUEST_DOMAIN:
				requestDomain();
				break;
			case REQUEST_ANC:
				requestAnc(m);
				break;
			case REQUEST_OWN_ANC:
				requestOwnAnc();
				break;
			case REQUEST_IP:
				requestIp(m);
				break;
			default:
				unknown(m);
				break;
		}
	}

	/**
	 * Implements Thread class run method
	 *
	 * Run the application for one thread
	 */
	@Override
	public void run() {
		boolean run = true;
		while (run && !wantAnExit) {
			Message m = read();
			if (m != null) {
				handler(m);
			} else {
				continue;
			}
		}
	disconnect();
	}
}
