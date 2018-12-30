package view.menu;

import java.util.LinkedList;

/**
 * Classe abstraite de gestion du Menu
 * @author Blokus_1
 */
public abstract class MenuType {


/************
 * Attibuts *
 ************/
    protected LinkedList<Question> options;
    protected boolean runMenu;
    protected boolean mainMenu;


/****************
 * Constructeur *
 ****************/

  /** Constructeur */
  public MenuType(){
    runMenu = false;
    mainMenu = false;
    options = new LinkedList<Question>();
  }

/*********************
 * Changer État Menu *
 *********************/

  /** Change l'état du menu  */
  public void changeMenuType() { this.mainMenu = !mainMenu; }

  /** Lance ou arrête le menu */
  public void invertRun() { this.runMenu = !runMenu; }


/***********
 * Actions *
 ***********/

  /**
   * Permet d'ajouter une question au menu
   * @param question la question à ajouter
   * @return le Menu courant
   */
  protected MenuType addQuestion(Question question) {
    options.add(question);
    return this;
  }

  /**
   * Enleve une question du Menu
   * @param question la question du menu
   * @return le Menu courant
   */
  public MenuType removeQuestion(Question question) {
    if  (options.contains(question)) {
      options.remove(question);
    }
    return this;
  }

  /**
   * Pause la question du joueur
   * @param i l'indice de la question
   * @return le Menu courant
   */
    public MenuType askQuestion(int i) {
      options.get(i).action();
      return this;
    }

  /**
   * Demande au joueur son choix de question
    * @return le numéro du choix
   */
  public abstract int askChoice();

  /**
   * Affiche la question
   * @return le Menu Courant
   */
  public abstract MenuType printQuestions();

  /**
   * Affiche toutes les questions du Menu
   * @return le Menu courant
   */
  public MenuType printAllQuestions() {
    if (mainMenu) {
      System.out.println("****************************************");
      System.out.println("*            Menu du Blokus            *");
      System.out.println("*        Version 1.2 - Blokus_1        *");
    }
    System.out.println("****************************************");
    printQuestions();
    System.out.println("****************************************");
    return this;
  }

  /** Lance le Menu  */
    public void run() {
      runMenu = true;
      while (runMenu) {
        printAllQuestions();
        int demande = askChoice();
        if (demande < 1) {
          System.out.println("Erreur => Demande invalide.");
          System.out.println("****************************************");
        } else {
          askQuestion(demande-1);
        }
      }
    }


}
