package common;

/**
 * Class to represent announces
 *
 * @author Marais - Viau
 */
public class Annonce {
	private String idAnnonce;
	private String domaine;
	private final String user;
	private String titre;
	private String descriptif;
	private String prix;

	/**
	 * Constructor
	 *
	 * @param user
	 * @param domaine
	 * @param titre
	 * @param descriptif
	 * @param prix
	 *
	 * @throws IllegalArgumentException if a field is null or if the price isn't correct
	 */
	public Annonce(String user, String domaine, String titre, String descriptif, String prix) {
		if(user == null || domaine == null || titre == null || descriptif == null || prix == null || !isWellFormat(prix)) {
			throw new IllegalArgumentException("Annonce must be full");
		}
		this.domaine = domaine.toUpperCase();
		this.user = user;
		this.titre = titre;
		this.descriptif = descriptif;
		this.prix = prix;
		this.idAnnonce = user + this.hashCode();
	}

	private boolean isWellFormat(String prix) {
		try {
			Float.parseFloat(prix);
		} catch(IllegalArgumentException e) {
			return false;
		}
		return true;
	}

	/**
	 * User getter
	 *
	 * @return the user
	 */
	public String getUser() {
		return this.user;
	}

	/**
	 * Domain getter
	 *
	 * @return the domain
	 */
	public String getDomaine() {
		return this.domaine;
	}

	/**
	 * Domain setter
	 * @param domaine the domain, not changed in case of null
	 */
	public void setDomaine(String domaine) {
		if(domaine != null) {
			this.domaine = domaine;
		}
	}

	/**
	 * Title getter
	 *
	 * @return the title
	 */
	public String getTitre() {
		return this.titre;
	}

	/**
	 * Title setter
	 *
	 * @param titre the title of the announce, not changed if it's null
	 */
	public void setTitre(String titre) {
		if(titre != null) {
			this.titre = titre;
		}
	}

	/**
	 * Description getter
	 *
	 * @return the description
	 */
	public String getDescriptif() {
		return this.descriptif;
	}

	/**
	 * Description setter
	 *
	 * @param descriptif the description of the announce, not changed if it's null
	 */
	public void setDescriptif(String descriptif) {
		if (descriptif != null) {
			this.descriptif = descriptif;
		}
	}

	/**
	 * Price getter
	 *
	 * @return a String that represents the price
	 */
	public String getPrix() {
		return this.prix;
	}

	/**
	 * Price setter
	 *
	 * @param prix the price, not changed if null or not well format
	 */
	public void setPrix(String prix) {
		if(prix != null && isWellFormat(prix)) {
			this.prix = prix;
		}
	}

	/**
	 * announce id getter
	 *
	 * @return a String that represents the ID
	 */
	public String getId() {
		return this.idAnnonce;
	}

	/**
	 * Convert the announce into a sendable string args table
	 *
	 * @return an array of length five
	 */
	public String[] toStringArgs() {
		String[] args = new String[5];
		args[0] = getId();
		args[1] = getDomaine();
		args[2] = getTitre();
		args[3] = getDescriptif();
		args[4] = getPrix();
		return args;
	}

	/**
	 * Convert a received args table into an Annonce
	 *
	 * @param user the user that sends the announce
	 * @param args the description of the announce
	 * @return an Annonce or null in case of error
	 */
	public static Annonce fromStringArgs(String user, String []args) {
		if(args.length != 4) {
			return null;
		} else {
			try {
				return new Annonce(user, args[0], args[1], args[2], args[3]);
			} catch(IllegalArgumentException e) {
				return null;
			}
		}
	}

	private String filterString(String str) {
		return (str.equals("null")) ? null : str;
	}

	/**
	 * Update an announce with a received table
	 *
	 * @param args the array received
	 * @return true if the array if well format
	 */
	public boolean updateWithArgs(String[] args) {
		if(args.length != 5) {
			return false;
		}
		setDomaine(filterString(args[1]));
		setTitre(filterString(args[2]));
		setDescriptif(filterString(args[3]));
		setPrix(filterString(args[4]));
		return true;
	}
}
