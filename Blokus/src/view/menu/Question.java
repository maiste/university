package view.menu;

/**
 * Classe abstraite gérant les questions du menu
 * @author Blokus_1
 */
public class Question {


/*********************
 * Gestion de classe *
 *********************/

  String question;
  Action action;

  /**
   * Constructeur
   * @param question la question à poser
   * @param action la méthode à lancer pour répondre
   */
  public Question(String question, Action action){
    this.question = question;
    this.action = action;
  }


/***********
 * Actions *
 ***********/

  /** Change l'interface fonctionnelle */
  public void setAction(Action newAction) { this.action = newAction; }

  /** Change la chaine de caratères de la question */
  public void setQuestion(String question) { this.question = question; }

  /** Affiche la question */
  public void ask() {  System.out.println(question); }

  /** L'action liée à la question à lancer */
  public void action(){
      action.doAction();
  }



/****************************
 * Interface fonctionnelle  *
 ****************************/

  @FunctionalInterface
  /** Interface gérant les actions lancées par chaque question  */
  public interface Action{

    /** Effectue une action simple  */
    void doAction();
  }

}
