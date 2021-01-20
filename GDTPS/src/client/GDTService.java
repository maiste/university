package client;

import common.Logs;
import common.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * This class handle all the back-end part It brings a methods to communicate
 * with the server to get or update product post.
 *
 * @author Marais-Viau
 */
public class GDTService implements Runnable {
	public Socket socket;
	int SERV_PORT;
	String SERV_ADDR;

	BufferedReader in;
	PrintWriter out;

	private int timeOut;

	public GDTService(String serverAddress, int serverPort, int timeOut) {
		this.SERV_PORT = serverPort;
		this.SERV_ADDR = serverAddress;
		this.timeOut = timeOut;
	}

	public GDTService(String addr, int port) {
		this(addr, port, 5);
	}

	private String readMessage() throws IOException {
		String message = "";
		String packet = "";
		while ((packet = in.readLine()) != null && !packet.equals(".")) {
			message += (packet + "\n");
		}
		Logs.log("received :\n" + message);
		return message;
	}

	public void send(Message message) {
		out.print(message.toNetFormat());
		out.flush();
	}

	public CompletableFuture<Message> askFor(Message request) {
		return CompletableFuture.supplyAsync(() -> {
			Logs.log("sending :\n" + request.toNetFormat());

			out.print(request.toNetFormat());
			out.flush();
			try {
				return Message.stringToMessage(readMessage());
			} catch (Exception e) {
				Logs.error("Server interruption");
				System.exit(1);
			}
			return null;
		}

		).completeOnTimeout(null, timeOut, TimeUnit.SECONDS);
	}

	@Override
	public void run() {
		try {
			socket = new Socket(InetAddress.getByName(SERV_ADDR), SERV_PORT);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			Logs.error("GDTService -> " + e.getMessage());
			System.exit(1);
		}
	}
}
