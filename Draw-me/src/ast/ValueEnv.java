package ast;

import exception.ParserException;
import expression.Expression;
import parser.Parser;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;


/**
 * Classe de gestion des variables d'environnement
 * @author DURAND - MARAIS
 */
public class ValueEnv {

    private LinkedList<HashMap<String,Expression>> variables;
    private LinkedList<HashMap<String,Expression>> constantes;
    private HashMap<String,Proc> proc;
    private Stack<LinkedList<String>> callStack;
    private int taille;
    

    /** Constructeur par défaut */
    public ValueEnv() {
        variables = new LinkedList<>(); // Liste de variables
        constantes = new LinkedList<>(); // Liste de constantes
        proc = new HashMap<String,Proc>(); // Ensemble des fonctions
        callStack = new Stack<>(); // Pile d'arguments
        callStack.push(new LinkedList<>());
        taille = 0; // Taille des listes
    }


/**************************
 * Méthodes modifications *
 **************************/

	/**
     * Renvoie le type de la constante / variable
     * @param nom le nom de la variable / constante
     * @param line le numéro de la line
     * @param colonne le numéro de la colonne
     * @return le type de la fonction
     */
	public Type getType(String nom, int line, int colonne) throws Exception{
            for (int i = taille-1 ; i >= 0 ; i--) {
                Expression resAltern = variables.get(i).get(nom);
                if (resAltern != null) { resAltern.setType(this); return resAltern.getType(); }
                resAltern = constantes.get(i).get(nom);
                if (resAltern != null) { resAltern.setType(this); return resAltern.getType(); }
            }
            throw new ParserException("L'identificateur "+nom+" n'existe pas, il n'a pas été déclaré",line,colonne);
	}

	/**
     * Vérifie si la liste contient les variables / constantes
     * @param nom le nom de la variable / constante
     * @return true si une d'elles contient
     */
	public boolean contains(String nom){
            for (int i=taille-1 ; i>=0 ; i--) {
                if(variables.get(i).containsKey(nom) || constantes.get(i).containsKey(nom)) return true;
            }
            return false;
	}

	/**
     * Retourne l'expression liée à la variable 
     * @param nom le nom de la variable
     * @param line la ligne d'erreur
     * @param column la colonne d'erreur
     * @return l'expression dans la pile
     * @throws Exception absence d'existence
     */ 
	public Expression get(String nom, int line, int column) throws Exception{
            for (int i = taille-1 ; i >= 0 ; i--) {
                Expression res = variables.get(i).get(nom);
                if (res != null) { return res; }
                res = constantes.get(i).get(nom);
                if (res != null) { return res; }
            }
            throw new ParserException("L'identificateur "+nom+" n'existe pas, il n'a pas été déclaré",line,column);
	}

    /**
     * Change la valeur d'une variable
     * @param nom le nom de la variable
     * @param exp l'expression à changer dans la map
     * @throws Exception mauvais type ou valeur
     */
	public void set(String nom, Expression exp) throws Exception{
            for(int i = taille -1; i >= 0 ; i--) {
                Expression res = variables.get(i).get(nom);
                if(res != null) {
                    if(res.getType() == exp.getType()) { variables.get(i).put(nom,exp.getExpression(this)); return;}
                    else { throw new ParserException("Le type de l'expression est "+exp.getExpression(this).getType()+" or on s'attend à avoir pour l'identificateur "+nom+" un type "+ res.getType() +" n'existe pas, il n'a pas été déclaré",exp.getLine(),exp.getColumn()); }
                }
                res = constantes.get(i).get(nom);
                if (res != null) { throw new ParserException("Vous essayez de changer la valeur d'une constante",exp.getLine(),exp.getColumn()); }
            }
            throw new ParserException("L'identificateur "+nom+" n'existe pas, il n'a pas été initialisé",exp.getLine(),exp.getColumn());
                
	}


    /**
     * Ajouter une nouvelle variable à l'environnement
     * @param nom le nom de la variable à ajouter
     * @param exp le nom de l'expression à changer 
     * @param isConstante s'il s'agit d'une constante ou non
     * @throws Exception existe déjà
     */
	public void put(String nom, Expression exp, boolean isConstante) throws Exception{
        if(!variables.getLast().containsKey(nom) && !constantes.getLast().containsKey(nom) 
            && !callStack.peek().contains(nom)) {
             if(isConstante){
                 constantes.getLast().put(nom,exp.getExpression(this));
             }
             else { variables.getLast().put(nom,exp.getExpression(this)); }
        } else {
            throw new ParserException("L'identificateur " + nom + " a déjà été initialisé", exp.getLine(), exp.getColumn());
        }
    }

    /**
     * ajoute la procédure à la liste des procédures
     * @param toAdd la procédure à déclarer
     */
    public void addProc (Proc toAdd) throws Exception {
        if (proc.containsKey(toAdd.getName())) {
            throw new Exception ("La méthode " + toAdd.getName() + " a déjà été déclarée");
        } 
            proc.put(toAdd.getName(), toAdd);
    }


    /** 
     * Renvoie la procédure dont le nom est spécifié.
     * Remplit la stack d'argument et les variables
     * @param name le nom de la procédure
     * @param args la liste d'arguments disponibles
     * @return l'Ast 
     */
    public Proc call (String name, LinkedList<Expression> args) throws Exception {
        Proc procedure = proc.get(name);
        if (procedure == null) {
            throw new Exception("La procédure " + name + " n'a pas été déclarée avant son appel.");
        }
        LinkedList<String> nameArgs = procedure.getArgs();
        if (nameArgs.size() != args.size()) {
            throw new ParserException ("Les arguments ne correspondent pas", procedure.line(), procedure.column());
        } else {
            this.add();
            this.addStack(new LinkedList<String>());
            for (int i = 0 ; i < nameArgs.size() ; i++) {
                this.put(nameArgs.get(i),args.get(i),false);
            }
            this.cleanStack();
            this.addStack(nameArgs);
        }
        return procedure;
    }




/***************************
 * Gestions Pile et listes *
 ***************************/

	/**
     * Ajoute un cran au LinkedList
     */
	public void add(){
            variables.add(new HashMap<String,Expression>());
            constantes.add(new HashMap<String,Expression>());
            this.taille++;            
	}

	/**
     * Supprime un cran des LinkedList
     */
	public void pollLast() throws Exception{
            variables.removeLast();
            constantes.removeLast();
            this.taille--;
	}


    /** 
     * Remplit la stack
     * @param args les noms des arguments à ajouter
     */
    public void addStack(LinkedList<String> args) {
        callStack.push(args);
    }

    /**
     * Vide la stack
     */
    public void cleanStack() throws Exception {
        callStack.pop();
    }




    @Override
    public String toString(){
        String res = "";
        for (int i = 0; i < taille; i++) {
            res += "Variable "+i+" "+ variables.get(i).size() +"\n";
            res += variables.get(i).values().toString() + "\n";
        }
        for (int i = 0; i < taille; i++) {
            res += "Constante "+i+" "+ constantes.get(i).size() +"\n";
            res += constantes.get(i).values().toString() + "\n";
        }
        return res +"\n\n";
    }
}
