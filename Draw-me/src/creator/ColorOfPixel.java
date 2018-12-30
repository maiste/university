package creator;

import java.awt.image.BufferedImage;

/**
 * Classe pour une couleur dans le créator
 * @author DURAND-MARAIS
 */

public class ColorOfPixel{
    private int red;
    private int green;
    private int blue;
    private int hascode;

    // entier d'approximation entre les différentes couleurs
    public static final int diffOfPixel = 8;

    /**
     * Constructeur d'un objet 'ColorOfPixel' correspondant à la couleur d'un pixel d'une image
     * @param  x     coordonnée en ordonnée du pixel dont on veut la couleur
     * @param  y     coordonnée en abscisse du pixel dont on veut la couleur
     * @param  image image dont on veut la couleur des pixels
     */
    public ColorOfPixel(int x, int y, BufferedImage image) {
        int color = image.getRGB(y,x);
        hascode = color;
        blue = color & 0xff;
        green = (color & 0xff00) >> 8;
        red = (color & 0xff0000) >> 16;
        blue =(blue >= (diffOfPixel/2))? blue + (diffOfPixel/2) - (blue + (diffOfPixel/2)) % diffOfPixel - 1 : 0;
        red =(red >= (diffOfPixel/2))? red + (diffOfPixel/2) - (red + (diffOfPixel/2))%diffOfPixel -1: 0;
        green =(green >= (diffOfPixel/2))? green + (diffOfPixel/2) - (green + (diffOfPixel/2))%diffOfPixel -1: 0;
    }

    /**
     * On redéfinie la fonction 'equals' entre deux objets 'ColorOfPixel'
     * @param  obj qu'on veut comparer avec 'this'
     * @return     true si la couleur de 'this' et de 'obj' est la même.
     */
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ColorOfPixel){
            ColorOfPixel compare = (ColorOfPixel) obj;
            return this.red == compare.red
                    && this.green == compare.green
                    && this.blue == compare.blue;

        }
        else return super.equals(obj);
    }

    /**
     * On redéfinie la fonction 'hashCode' car on a redéfinie 'equals'
     * @return le hashCode correspondant à la couleur véritable du pixel
     */
    @Override
    public int hashCode() {
        return hascode;
    }

    @Override
    public String toString() {
        String r = Integer.toHexString(red).toUpperCase();
        String g = Integer.toHexString(green).toUpperCase();
        String b = Integer.toHexString(blue).toUpperCase();
        r = (r.length() <= 1)? "0"+r : r;
        g = (g.length() <= 1)? "0"+g : g;
        b = (b.length() <= 1)? "0"+b : b;
        return "#"+r+g+b;
    }
}
