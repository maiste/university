package view.menu;

import view.terminal.ScanTerminal;
import java.util.LinkedList;

/** Classe de gestion de Menu avec des chaines de caractères */
public class StringMenu extends MenuType {

    private LinkedList<String> table;
    private String demande;


/****************
 * Constructeur *
 ****************/

  /** Constructeur */
  public StringMenu(){
      super();
      table = new LinkedList<>();
      demande = "";
  }

/**************
 * Spécifique *
 **************/

  /** Retourne la question générale */
    public String getDemande(){ return this.demande;  }

  /**
   * Ajoute la question avec le pattern à matcher
   * @param question la question à poser
   * @param pattern le pattern lié
   * @return this - le menu courant
   */
  public StringMenu addQuestion(Question question, String pattern) {
    super.addQuestion(question);
    if (pattern == null || pattern.equals(""))  {  pattern = Integer.toString(table.size()-1); }
    table.add(pattern);
    return this;
  }

  /** Change le pattern de la question
   * @param question la question à trouver
   * @param pattern le nouveau pattern
   * @return le Menu courant
   */
  public StringMenu setQuestionPattern (Question question, String pattern) {
    int index = options.indexOf(question);
    table.remove(index);
    table.add(index, pattern);
    return this;
  }


/************
 * Override *
 ************/

  @Override
  public StringMenu removeQuestion(Question question) {
    int index = options.indexOf(question);
    super.removeQuestion(question);
    table.remove(index);
    return this;
  }

  @Override
  public int askChoice() {
    String choice = (ScanTerminal.askString("Rentrer votre choix : ")).toUpperCase();
    this.demande = choice;
    for (int i = 0 ; i < table.size() ; i++) {
      if (choice.matches(table.get(i).toUpperCase())) {  return i+1;  }
    }
    return -1;
  }

  @Override
  public StringMenu printQuestions () {
    for (int i = 0 ; i < options.size() ; i++ ) {
      if (table.get(i).matches("[0-9].*")){
        System.out.print("n) ");
      } else {
        System.out.print(table.get(i)+ ") ");
      }
      options.get(i).ask();
    }
    return this;
  }
}
