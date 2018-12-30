package creator;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;

/**
 * Classe pour la création d'un fichier suivant la grammaire du projet
 * pour dessiner l'image donnée en argument
 * @author DURAND-MARAIS
 */

public class CreateImage {
	private BufferedImage image;
	private boolean[][] visitedPixel;
	private LinkedList<Point> list = new LinkedList<>();
	private FileWriter resultat;
	private String nomFile = "";
	private int indent = 0;
	private int width;
	private int height;
	private int nombreZone = 0;
	public static final int maxLine = 4000000;
	
	/**
	 * On crée un objet 'CreateImage' avec le chemin vers l'image que l'on veut dessiner
	 * @param  path chemin de l'image qu'on veut dessiner
	 */
	public CreateImage(String path){
		try {
			File file = new File(path);
			String[] tab = file.getAbsolutePath().split("/");
			nomFile = tab[tab.length-1].split("\\.")[0];
			image = ImageIO.read(file);
			height = image.getHeight();
			width = image.getWidth();
			visitedPixel = new boolean[height][width];
			list.add(new Point());
			File ff=new File("test/"+nomFile);
			ff.createNewFile();
			resultat = new FileWriter(ff);
		}
		catch (IOException e){
			System.out.println("Le chemin de l'image n'existe pas.");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * On crée, à partir de cette fonction, le fichier contenant le dessin de l'image donnée en argument.
	 */
	public void createText(){
		int i = 0;
		boolean b = true;
		this.fillList();
		while (i <= maxLine && b){
			b = fillPixels();
		}
	}

	/**
	 * On remplit l'attribut 'list' avec tous les points de l'image
	 */
	private void fillList(){
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				list.add(new Point(i,j));
			}
		}
	}

	/**
	 * On construit le plus grand rectangle possible en suivant l'algorithme suivant :
	 * - on regarde si le point qu'on récupère est déjà dans un rectangle ou pas, si oui on passe au point suivant, sinon :
	 * - on regarde s'il existe une ligne contenant les mêmes pixels
	 * - on fait de même pour la colonne
	 * - et on élargit notre rectangle de cette façon là
	 * quand c'est terminé, on note dans le fichier le rectangle qu'on a obtenu
	 * @return true si on a trouvé un rectangle, false s'il n'existe plus aucun nouveau rectangle dans l'image
	 */
	private boolean fillPixels(){
		Point first = list.pollFirst();
		if(first == null) return false;
		if(visitedPixel[(int) first.getX()][(int) first.getY()]) {
			return true;
		}
		ColorOfPixel color = new ColorOfPixel((int) first.getX(), (int) first.getY(), image);
		visitedPixel[(int) first.getX()][(int) first.getY()] = true;
		boolean horizontal = true;
		boolean vertical = true;
		int transX = 1;
		int transY = 1;
		while (horizontal || vertical){
			if(horizontal) {
				horizontal = goodLineHorizontal(color, new Point((int) first.getX()+transX, (int) first.getY()), transY - 1);
				if (horizontal) transX++;
			}
			if(vertical) {
				vertical = goodLineVertical(color, new Point((int) first.getX(), (int) first.getY() + transY), transX - 1);
				if (vertical) transY++;
			}
		}
		ajoutResultat("FillRect("+(int) first.getY()+ "," +(int) first.getX()+","+transY+","+transX+","+color.toString()+");" + "\n");
		nombreZone++;
		return true;
	}

	/**
	 * On regarde si toutes les couleurs sur une même ligne horizontale sont égales (selon la méthode 'equals')
	 * @param  color    couleur qu'on compare
	 * @param  depart   point de départ de la ligne
	 * @param  distance distance de la ligne
	 * @return          true si toute la ligne est de la même couleur (et on note que la ligne est dans un rectangle), false sinon
	 */
	private boolean goodLineHorizontal(ColorOfPixel color, Point depart, int distance){
		for(int i = 0; i <= distance; i++){
			if( ! compare((int) depart.getX(), (int) depart.getY()+i, color)) {
				return false;
			}
		}
		changeBoolean(depart, distance, true);
		return true;
	}

	/**
	 * On regarde si toutes les couleurs sur une même colonne verticale sont égales (selon la méthode 'equals')
	 * @param  color    couleur qu'on compare
	 * @param  depart   point de départ de la colonne
	 * @param  distance distance de la colonne
	 * @return          true si toute la colonne est de la même couleur (et on note que la colonne est dans un rectangle), false sinon
	 */
	private boolean goodLineVertical(ColorOfPixel color, Point depart, int distance){
		for(int i = 0; i <= distance; i++){
			if( ! compare((int) depart.getX()+i, (int) depart.getY(), color)) {
				return false;
			}
		}
		changeBoolean(depart, distance, false);
		return true;
	}

	/**
	 * On note que les pixels appartenant à :
	 * - la ligne si horizontal est à true
	 * - la colonne sinon
	 * commançant par le point de coordonnées 'point' et sur une distance de 'distance' sont dans un rectangle.
	 * @param point      coordonnées du pixel
	 * @param distance   taille de la ligne/colonne
	 * @param horizontal boolean déterminant si on manipule une ligne ou une colonne
	 */
	private void changeBoolean(Point point, int distance, boolean horizontal){
		for (int i = 0; i <= distance; i++) {
			if(horizontal) {
				int x = (int) point.getX();
				int y = (int) point.getY() + i;
				if(inTableau(x,y)){
					visitedPixel[x][y] = true;
				}
			}
			else{
				int x = (int) point.getX() + i;
				int y = (int) point.getY();
				if(inTableau(x,y)){
					visitedPixel[x][y] = true;
				}
			}
		}
	}

	/**
	 * On compare la couleur du pixel de coordonnées (x,y) selon un repère informatique et non mathématique avec
	 * la couleur donnée en paramètre
	 * @param  x     coordonnée en ordonnée du pixel
	 * @param  y     coordonnée en abscisse du pixel
	 * @param  color couleur de comparaison
	 * @return       true si les deux éléments sont dans l'image et sont de même couleur, false sinon.
	 */
	private boolean compare(int x, int y, ColorOfPixel color){
		if(inTableau(x,y)){
			ColorOfPixel pixel1 = new ColorOfPixel(x,y,image);
			return color.equals(pixel1);
		}
		else{
			return false;
		}
	}

	/**
	 * On vérifie que les coordonnées (x,y) appartiennent à l'image
	 * @param  x coordonnée en ordonnée du pixel
	 * @param  y coordonnée en abscisse du pixel
	 * @return   true si les deux coordonnées sont dans l'image, false sinon.
	 */
	private boolean inTableau(int x, int y){
		return (y >= 0 && y < width && x < height && x >= 0);
	}

	/**
	 * on ajoute le String en argument dans le fichier contenant le dessin de l'image
	 * On ajoute des éléments 'Begin' et 'End' pour qu'il n'y ait pas de stackoverflow dans le parser
	 * @param ajout instruction qu'on ajoute dans le fichier
	 */
	private void ajoutResultat (String ajout){
		try{
			int max = 200;
			if(indent == 0) resultat.write("Begin\n");
			resultat.write("    "+ajout);
			indent++;
			if(indent == max){
				resultat.write("End;\n");
				indent = 0;
			}
		}
		catch (Exception e) {
			System.out.println("problème dans l'écriture dans le fichier.");
			System.exit(-1);
		}
	}

	/**
	 * On vérifie que le nombre de 'Begin' et de 'End' correspondent bien, et on ferme le fichier dans
	 * lequel on a écrit le dessin de l'image
	 */
	public void createFile(){
		this.createText();
		String res = (indent > 0)? "End;" : "";
		try{
			resultat.write(res);  // écrire une ligne dans le fichier
			resultat.close(); // fermer le fichier à la fin des traitements
		} 
		catch (Exception e) {
			System.out.println("problème dans l'écriture dans le fichier.");
		}
	}
	
}