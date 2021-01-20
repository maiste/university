package client;

import client.gui.GUI;
import common.Logs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.OptionalInt;
import java.util.function.Supplier;

/**
 * Class to display product well
 *
 * @author Marais-Viau
 */
public class ProductViewer {
	static GUI gui;

	public static void setGui(GUI gui) {
		ProductViewer.gui = gui;
	}

	private ProductViewer() {
	}


	/**
	 * Display the product list
	 *
	 * @param annonces the lsit of announces
	 */
	public static void displayProducts(String[] annonces) {
		if (annonces == null) {
			gui.println("NONE");
			return;
		}
		Logs.log("ENTER DISPLAY annonces size : " + annonces.length);
		String[] labels = new String[] { "Product ID", "Domain", "Title", "Description", "Price" };
		ArrayList<String[]> m_annonces = new ArrayList<>();
		ArrayList<String> ids = new ArrayList<>();
		ArrayList<String> domains = new ArrayList<>();
		ArrayList<String> titles = new ArrayList<>();
		ArrayList<String> descriptions = new ArrayList<>();
		ArrayList<String> prices = new ArrayList<>();
		ids.add(labels[0]);
		domains.add(labels[1]);
		titles.add(labels[2]);
		descriptions.add(labels[3]);
		prices.add(labels[4]);
		m_annonces.add(labels);
		for (int i = 0; i < annonces.length; i += 5) {
			ids.add(annonces[i]);
			domains.add(annonces[i + 1]);
			titles.add(annonces[i + 2]);
			descriptions.add(annonces[i + 3]);
			prices.add(annonces[i + 4]);
			m_annonces.add(Arrays.copyOfRange(annonces, i, i + 5));
		}
		int largerId = largerOf(ids);
		int largerDomain = largerOf(domains);
		int largerTitle = largerOf(titles);
		int largerDescription = largerOf(descriptions);
		int largerPrice = largerOf(prices);

		Supplier<String> printGates = () -> {
			String s = "";
			s += printGate(largerId);
			s += printGate(largerDomain);
			s += printGate(largerTitle);
			s += printGate(largerDescription);
			s += printGate(largerPrice);
			return s + "+"  ;
		};
		String finalString = "";
		for (String[] strings: m_annonces) {
			finalString += printGates.get();
			finalString += String.format

					(
							"\n|%" + -largerId + "s" + "|%" + -largerDomain + "s" + "|%" + -largerTitle + "s" + "|%"
									+ -largerDescription + "s" + "|%" + -largerPrice + "s|\n",
							strings[0], strings[1], strings[2], strings[3], strings[4]);
			if (strings[0].contains("Product")) {
				finalString += printGates.get();
				finalString += "\n";
			}
			;
		}
		finalString += printGates.get();
		finalString += "\n";
		gui.println(finalString);
	}

	/**
	 * Print a dthe list of domains
	 *
	 * @param domains the list of domains
	 */
	public static void printDomains(String[] domains) {
		String final_s = "";
		String content_s = "";
		int gateSize = Arrays.stream(domains).mapToInt(String::length).sum() + domains.length - 1;
		final_s += printGate(gateSize);
		final_s += "+";
		for (String s : domains) {
			content_s += "|" + s;
		}
		final_s += "\n" + content_s + "|";
		final_s += "\n"+printGate(gateSize);
		final_s += "+";
		gui.println(final_s);
	}

	private static String printGate(int size) {
		String gate = "";
		for (int i = 0; i < size; i++) {
			gate += '-';
		}
		return ("+" + gate);
	}

	private static int largerOf(ArrayList<String> args) {
		OptionalInt m = args.stream().mapToInt(String::length).max();
		return m.getAsInt();
	}

}
