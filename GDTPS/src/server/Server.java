package server;

import common.Logs;
import common.Index;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;

/**
 * Class to manage Server
 * Main class
 *
 * @author Marais-Viau
 */
public class Server {
	private ServerSocket server = null;

	/**
	 * Constructor
	 *
	 * @param port the port on which the server listens
	 */
	public Server(int port) {
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			Logs.error("Can't bind socket on port: " + port);
		}
		Logs.log("Server created on port: " + port);
	}

	/**
	 * Runs the server service
	 */
	public void serve() {
		Logs.log("Server started");
		while (true) {
			Logs.log("Server is listenning...");
			try {
				Socket s = server.accept();
				Logs.log("New connection with the server");
				new Handler(s).start();
			} catch (IOException e) {
				Logs.warning("Try to handle new connection but failed -> continue");
				continue;
			}
		}
	}

	/**
	 * Main method that runs the server on a choosen port
	 */
	public static void main(String[] args) {
		int port = 1027;
		if (args.length != 1) {
			Logs.error("One argument requiered: [port]");
			System.exit(1);
		} else {
			try {
				port = Integer.parseInt(args[0]);
				if (port < 0 || port > 65535)
					throw new NumberFormatException();
			} catch (NumberFormatException e) {
				Logs.error("Error: 0 < port < 65535");
				System.exit(1);
			}
		}

		new Server(port).serve();
	}
}
