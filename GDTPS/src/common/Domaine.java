package common;

/**
 * Class to manage Domain
 *
 * @author Marais-Viau
 */
public class Domaine {

	/**
	 * Convert a String to a DomainTpe
	 *
	 * @param domaine the String with the domain name
	 * @return the domain in the DomainType format
	 * @throws IllegalArgumentException if the domain is unknown
	 */
	 public static DomaineType fromString(String domaine) {
			switch(domaine.toUpperCase()) {
				 case "AUTRE":
						return DomaineType.AUTRE;
				 case "IMMOBILIER":
						return DomaineType.IMMOBILIER;
				 case "LOGICIEL":
						return DomaineType.LOGICIEL;
				 case "LOISIR":
						return DomaineType.LOISIR;
				 case "MEUBLE":
						return DomaineType.MEUBLE;
				 case "MODE":
						return DomaineType.MODE;
				 case "MULTIMEDIA":
						return DomaineType.MULTIMEDIA;
				 case "VEHICULE":
						return DomaineType.VEHICULE;
				 default:
						throw new IllegalArgumentException("Domain unknown");
			}
	 }

	 /**
		* An enumeration to represents the list of the domains
		*/
	 public enum DomaineType {
			AUTRE("AUTRE"),
			IMMOBILIER("IMMOBILIER"),
			LOGICIEL("LOGICIEL"),
			LOISIR("LOISIR"),
			MEUBLE("MEUBLE"),
			MODE("MODE"),
			MULTIMEDIA("MULTIMEDIA"),
			VEHICULE("VEHICULE");


			private String name;

			private DomaineType(String name) {
				 this.name = name;
			}

			@Override
			public String toString() {
				 return this.name;
			}
	 }
}
