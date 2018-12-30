package controller;

/**
 * Classe d'informations joueurs
 * @author Blokus_1
 */
public class Intel {

    private int level;
    private String name;

    /**
     * On construit un objet Intel possédant un int et un string
     * @param name nom du joueur qu'on stocke
     * @param level niveau du joueur qu'on stocke (0->joueur reel, 1->ia easy,...)
     */
    public Intel(String name, int level){
        this.level = level;
        this.name = name;
    }

    /**
     * getter
     * @return retourne le level
     */
    public int getLevel() {
        return level;
    }

    /**
     * setter
     * @param level le nouveau level
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * getter
     * @return retourne le nom
     */
    public String getName() {
        return name;
    }

    /**
     * setter
     * @param name nouveau nom
     * @param id id du joueur servant au nouveau joueur si name est vide
     */
    public void setName(String name, int id) {
        this.name = (name == null || name.equals(""))?type()+" "+(id+1):name;
    }

    /**
     * on recupère le nom en fonction du level
     * @return nom correspondant au level
     */
    private String type(){
        switch (level){
            case 0:
                return "joueur";
            case 1:
                return "easy";
            case 2:
                return "medium";
            case 3:
                return "hard";
            case 4:
                return "learning";
            default:
                return "default";
        }
    }

}
