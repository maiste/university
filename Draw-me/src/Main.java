import ast.AST;
import ast.ValueEnv;
import lexer.Lexer;
import parser.LookAhead1;
import parser.Parser;
import creator.CreateImage;

import java.io.*;
import java.awt.*;
import javax.swing.*;
import java.lang.Exception;


/**
 * Classe Principale
 * @author DURAND - MARAIS
 */
public class Main {

    /** 
     * Méthode d'affichage 
     */
    private static void initAndShow(String filename) {
        JFrame frame = new JFrame("ADS4");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      
        frame.getContentPane().setPreferredSize(new Dimension(800,600));
        frame.getContentPane().add(new MyCanvas(filename));

        frame.pack();
        frame.setVisible(true);
    }

    /** Main */
    public static void main(String[] args) {
        if(args.length < 2){
            System.out.println("Il manque des arguments.");
        }
        else{
            if(args[1].equals("0")){
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        initAndShow(args[0]);
                    }
                });
            }
            else{
                CreateImage image = new CreateImage(args[0]);
                image.createFile();
                System.out.println("\nInstruction pour image créée dans test/");
            }
        }
    }
}

@SuppressWarnings("serial")
class MyCanvas extends JComponent {

    private AST ast;

    public MyCanvas(String filename) {
        ast = null;
        if(new AST(0,0).debugMode()) {System.out.println("\n=> Mode debug On");}
        ValueEnv registre = new ValueEnv();
        try {
            File input = new File(filename);
            Reader reader = new FileReader(input);
            Lexer lexer = new Lexer(reader);
            LookAhead1 look = new LookAhead1(lexer);
            Parser parser = new Parser(look);
            System.out.println("\n= Début parsing =");
            ast = parser.progNonTerm(); // Axiome 
            System.out.println("\n= Fin parsing =");

        } catch (Exception e) {
            System.out.println("** Erreur de compilation **\n" + e.getMessage()+"\n");
            System.exit(-1);
        }
        try {
            System.out.println("\n= Début vérification des types =");
            ast.verifyAll(registre);
            System.out.println("\n= Fin vérification des types =");
        }
        catch(Exception e){
            System.out.println("** Erreur de type **\n" + e.getMessage()+"\n");
            System.exit(-1);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        if (g instanceof Graphics2D)
            {
                Graphics2D g2d = (Graphics2D)g;

                try{
                    ValueEnv registre = new ValueEnv();
                    System.out.println("\n= Début de l'exécution =");
                    ast.exec(g2d,registre);
                    System.out.println("\n= Fin de l'exécution =");
                } catch (Exception e) {
                    System.out.println("** Erreur d'exécution **\n" + e.getMessage()+"\n");
                    System.exit(-1);
                }
            }
    }

}
