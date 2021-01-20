package client;

import client.gui.GUI;
import common.Logs;
import common.Message;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.function.Consumer;

/**
 * DataProvider - Parse and handle actions with message
 *
 * @author Marais-Viau
 */
public class DataProvider {
	private GDTService service;
	private static GUI gui;

	public DataProvider(GDTService service) {
		this.service = service;
		getTokenFromDisk();
	}

	public static void setGui(GUI gui) {
		DataProvider.gui = gui;
		ProductViewer.setGui(gui);
	}

	String userName = null;
	String token = null;

	/**
	 * Handle a basic request
	 *
	 * @param message  the original message
	 * @param expected the type expected for the message
	 * @param error    the type expected in case of error
	 * @param success  the action in case of success
	 * @param failure  the action in case of failure
	 */
	private static Message basicRequest(Message message, Message.MessageType expected, Message.MessageType error,
			Consumer<Message> success, Consumer<Message> failure) {
		if (message == null) {
			Logs.log("TIMEOUT : Server does not respond \n Disconnection...");
			System.exit(1);
		}
		if (message.getType() == expected) {
			success.accept(message);
		} else if (message.getType() == error) {
			failure.accept(message);
			return null;
		} else if (message.getType() == Message.MessageType.NOT_CONNECTED) {
			Logs.warning("Communication error \nServer response :" + message.toNetFormat());
			gui.println("You need to be connected first\n");
			return null;
		} else if (message.getType() == Message.MessageType.UNKNOWN_REQUEST) {
			Logs.error("Unknown Request");
			gui.println("The send message is not compatible with the server");
		} else {
			Logs.error("Communication error \nServer response :" + message.toNetFormat());
			return null;
		}
		return message;
	}

	/**
	 * Get the token and the user name from $HOME/.config/gdtp/token
	 *
	 */
	private void getTokenFromDisk(){
		File file = new File(System.getenv("HOME")+"/.config/gdtp/token");
		if(!file.exists()){
			return;
		}
		try {
			Scanner scanner = new Scanner(file);
			if(scanner.hasNextLine()){
				this.userName = scanner.nextLine();
			}
			if(scanner.hasNextLine()){
				this.token = scanner.nextLine();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Write the user name and his token in $HOME/.config/gdtp/token
	 *
	 * @param userName The user name of the user
	 * @param token Token of the user
	 */
	private void writeTokenInDisk(String userName, String token){
		File file = new File(System.getenv("HOME")+"/.config/gdtp/token");
		if(!file.exists()){
			try {
				file.getParentFile().mkdirs();
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write(userName);
			fileWriter.append("\n"+token);
			fileWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	 /**
	 * Handle the connection with a username and without
	 *
	 * @param userName the username of the user
	 * @return the userName;
	 */
	public String genericConnect(String [] tokens) {
		if (tokens.length < 2) {
			connect();
		} else {
			connect(tokens[1]);
		}
		return this.userName;
	}

	private void connect() {
		if (token == null) {
			gui.println("The first time you connect, you have to choose a user name. \n connect [user name]");
			return;
		}
		Message message = new Message(Message.MessageType.CONNECT, new String[] { token });
		service.askFor(message).thenAccept(m -> {
			basicRequest(m, Message.MessageType.CONNECT_OK, Message.MessageType.CONNECT_KO,
					v -> gui.println("Connected! Welcome back " + userName),
					v -> gui.println("Connection refused by the server"));
		}).join();
	}

	private void connect(String userName) {
		this.userName = userName;
		Message message = new Message(Message.MessageType.CONNECT, new String[] { userName });
		service.askFor(message).thenAccept(m -> {
			Message check = basicRequest(m, Message.MessageType.CONNECT_NEW_USER_OK, Message.MessageType.CONNECT_NEW_USER_KO,
					v -> {
						this.token = "#" + m.getArgs()[0];
						writeTokenInDisk(this.userName,this.token);
						gui.println("Connected! Welcome " + userName + "\nYour token is : " + token);
					}, v -> gui.println("Connection refused by the server"));
			if (check == null) {
				basicRequest(m, Message.MessageType.CONNECT_OK, Message.MessageType.CONNECT_KO, v -> {
					this.token = "#" + m.getArgs()[0];
                    writeTokenInDisk(this.userName,this.token);
                    gui.println("Connected! Welcome back " + userName + "\nYour token is : " + token);
				}, v -> gui.println("Connection refused by the server"));
			}
		}).join();
	}

	/**
	 * Discnonnect the client
	 */
	public void disconnect() {
		service.send(new Message(Message.MessageType.DISCONNECT));
		gui.println("Disconnected.");
		System.exit(0);
	}

	/**
	 * Get the product thanks to the domain
	 *
	 * @param domain the domain you want to use
	 */
	public void getProductByDomain(String domain) {
		Message message = new Message(Message.MessageType.REQUEST_ANC, new String[] { domain });
		Consumer<Message> successBehavior = answer -> {
			ProductViewer.displayProducts(answer.getArgs());
		};
		Consumer<Message> failBehaviour = answer -> {
			gui.println("Sorry this domain doesn't exist");
		};
		service.askFor(message).thenAccept(answer -> {
			basicRequest(answer, Message.MessageType.SEND_ANC_OK, Message.MessageType.SEND_ANC_KO, successBehavior,
					failBehaviour);
		}).join();
	}

	/**
	 * Get your own announces
	 */
	public void getMyAn() {
		Message message = new Message(Message.MessageType.REQUEST_OWN_ANC);
		var answer = service.askFor(message);
		Consumer<Message> successBehavior = m -> {
			gui.println("My posts are :");
			ProductViewer.displayProducts(m.getArgs());
		};
		Consumer<Message> failBehavior = m -> gui.println("Post not found");
		answer.thenAccept(m -> basicRequest(m, Message.MessageType.SEND_OWN_ANC_OK, Message.MessageType.SEND_OWN_ANC_KO,
				successBehavior, failBehavior)).join();
	}

	/**
	 * Try to post your announce
	 *
	 * @param annonce the array that represents your announce
	 */
	public void postAnc(String[] annonce) {
		Message message = new Message(Message.MessageType.POST_ANC, annonce);
		var answer = service.askFor(message);
		Consumer<Message> successBehavior = m -> gui.println("Announce posted !");
		Consumer<Message> failBehavior = m -> gui.println("The server refused your annonce, sorry");
		answer.thenAcceptAsync(m -> basicRequest(m, Message.MessageType.POST_ANC_OK, Message.MessageType.POST_ANC_KO,
				successBehavior, failBehavior)).join();
	}

	/**
	 * Try to update your announce
	 *
	 * @param annonce the array that represents your announce to update
	 */
	public void updateAnc(String[] annonce) {
		Message message = new Message(Message.MessageType.MAJ_ANC, annonce);
		var answer = service.askFor(message);
		Consumer<Message> successBehavior = m -> gui.println("Annonce updated !");
		Consumer<Message> failBehavior = m -> gui.println("The server refused your request");
		answer.thenAcceptAsync(m -> basicRequest(m, Message.MessageType.MAJ_ANC_OK, Message.MessageType.MAJ_ANC_KO,
				successBehavior, failBehavior)).join();
	}

	/**
	 * Try to delete your announce
	 *
	 * @param ancId the id of the announce your are trying to delete
	 */
	public void deleteAnc(String ancId) {
		Message message = new Message(Message.MessageType.DELETE_ANC, new String[] { ancId });
		var answer = service.askFor(message);
		Consumer<Message> successBehavior = m -> gui.println("Post succesfuly deleted");
		Consumer<Message> failBehavior = m -> gui.println("The server refused your request");
		answer.thenAcceptAsync(m -> basicRequest(m, Message.MessageType.DELETE_ANC_OK, Message.MessageType.DELETE_ANC_KO,
				successBehavior, failBehavior)).join();
	}

	/**
	 * Get all the domains from the server
	 */
	public void getDomains() {
		Message message = new Message(Message.MessageType.REQUEST_DOMAIN);
		var answer = service.askFor(message);
		Consumer<Message> successBehavior = m -> {
			gui.println("Here your domains :");
			ProductViewer.printDomains(m.getArgs());
		};
		Consumer<Message> failBehavior = m -> gui.println("The server can't retrieve domains");
		answer.thenAcceptAsync(m -> basicRequest(m, Message.MessageType.SEND_DOMAINE_OK,
				Message.MessageType.SEND_DOMAINE_OK, successBehavior, failBehavior)).join();
	}

	/**
	 * Try to request the address of a user, thanks to the id of his announce
	 *
	 * @param postID the id of the announce
	 */
	public Message requestIP(String postID) {
		Message message = new Message(Message.MessageType.REQUEST_IP, new String[] { postID });
		var answer = service.askFor(message);
		Consumer<Message> successBehavior = m -> Logs.log("IP for announce " + postID + " is " + m.getArgs()[0]);
		Consumer<Message> failBehavior = m -> Logs.warning("The server refused your request and can't retrieve the announce or an IP");
		try {
		 return answer.thenApply(m -> basicRequest(m, Message.MessageType.REQUEST_IP_OK, Message.MessageType.REQUEST_IP_KO,
				successBehavior, failBehavior)).get();
		} catch(Exception e) {
			return null;
		}
	}
}

