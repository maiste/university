package client;

import client.gui.GUI;
import common.Logs;

import java.io.IOException;
import java.util.IllegalFormatException;

/**
 * Client CLI Manager - Main class
 *
 * @author Marais-Viau
 */
public class Client {
	public static void main(String[] args) throws IOException {
		if (args.length == 4) {
			boolean debug = (args[0] != null && args[0].equals("no")) ? false : true;
			String addr = args[1];
			int port = 1027;
			int port_udp = 7201;

			try {
				port = Integer.parseInt(args[2]);
				port_udp = Integer.parseInt(args[3]);
			} catch (IllegalFormatException e) {
				System.out.println("Port format error");
			}
			if (!debug) {
				Logs.debugOff();
			}
			client.GDTService GDTService = new GDTService(addr, port);
			client.PeerService peerService = new PeerService(port_udp, 5000);
			new Thread(peerService).start();
			GDTService.run();
			DataProvider dataProvider = new DataProvider(GDTService);
			GUI gui = new GUI(dataProvider);
			DataProvider.setGui(gui);
			gui.run();
			System.out.println("Running client [OK]");
		} else {
			System.out.println("Not enough args for client: [debug] [addr] [port]");
		}
	}
}
