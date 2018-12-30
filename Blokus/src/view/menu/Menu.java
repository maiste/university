package view.menu;

import view.terminal.ScanTerminal;

/**
 * Classe pour le menu dans l'affichage terminal
 * @see MenuType
 * @author Blokus_1
 */
public class Menu extends MenuType {

  /** Constructeur  */
  public Menu() {
    super();
  }


  /*************************
   * Actions sur questions *
   *************************/

  @Override
  public int askChoice() {
    return ScanTerminal.askSpecificInt("Rentrer votre choix", "Veuillez rentrer un nombre entre" +
            "1 et " + options.size(), 1, options.size());
  }

  @Override
  public Menu addQuestion(Question question) {
    super.addQuestion(question);
    return this;
  }

  /*************
   * Affichage *
   *************/

  @Override
  public Menu printQuestions() {
    int i = 0;
    for (Question q : options) {
      System.out.print((i + 1) + ") ");
      q.ask();
      i++;
    }
    return this;
  }

}
