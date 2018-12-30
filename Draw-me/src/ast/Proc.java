package ast;

import java.util.LinkedList;
import java.awt.*;

/**
 * Classe de création des procédures
 * @author DURAND-MARAIS
 */
public class Proc extends AST{
	
	private LinkedList<String> args;
	private String name;
	private boolean isVerified = false;

	/**
	 * Constructeur
	 * @param line ligne du token
	 * @param column colonne du token
	 * @param name nom de la fonction
	 * @param content le contenu de la fonction
	 * @param args la liste des arguments de la fonction
	 */
	public Proc(int line, int column, String name, AST content, LinkedList<String> args){
		super(line,column);
		super.addNext(content);
		this.name = name;
		this.args = args;
	}

	/** On récupère le nom des arguments de la procédure */
	public LinkedList<String> getArgs(){
		return this.args;
	}

	/** On récupère le nom de la procédure */
	public String getName(){
		return name;
	}

	/** On récupère le contenu de la procédure */
	public LinkedList<AST> getContent(){
		return super.next;
	}

	/** On regarde si la procédure a déjà été vérifiée au niveau du typage */
	public boolean isVerified(){
		return this.isVerified;
	}

	/** On change le boolean pour dire que le typage de la procédure a été vérifiée */
	public void setVerified(){
		this.isVerified = true;
	}

	@Override
	public void exec(Graphics2D g2d, ValueEnv env) throws Exception{
		env.addProc(this);
	}

	@Override
	public void verifyAll(ValueEnv env) throws Exception{
		env.addProc(this);
	}


	/** Debug */
    public void debug(ValueEnv val, boolean b) {
        System.out.println("Fonction => nom : "+name+", et possède "+args.size()+" arguments.");
    }
	
}