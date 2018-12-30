###############################################
# Makefile pour le jeu du blokus en version 1 #
#      © Groupe Blokus 1 - Année 2018         #
###############################################


# Variables
FLAG_1 = -sourcepath
FLAG_2 = -d
JAR = lib/postgresql-42.2.0.jar
JC = javac
JVM = java
BIN = binary
DOC = docs
SOURCE = src/


##############
# Indication #
#############

# Affichage indicatif
all: 
	@echo ''Pour afficher les informations du jeu : make version''
	@echo ''Pour créer la javadoc : make docs''
	@echo ''Pour créer la base de données : make build''
	@echo ''Pour lancer le jeu dans le terminal : make terminal''
	@echo ''Pour lancer le jeu en graphique : make graphic''
	@echo ''Pour supprimer les binaires et la documentation : make clear''
	@echo ''Pour supprimer toutes les données fichiers et bdd : make clean''

###############
# Compilation #
##############

#Compilation unique
compile: 
			@echo ''Création du fichier des binaires''
			@mkdir -p $(BIN)
			@echo -e "[\e[1;34mEn cours\e[0m] Compilation"
			@$(JC) $(FLAG_1) $(SOURCE) -d $(BIN) $(SOURCE)**/*.java $(SOURCE)*.java 2> /dev/null
			@echo -e "[\e[1;32mOK\e[0m] Compilation finie"

# Création base de données :
build: compile
			@echo ''Création de la base de données''
			@echo -e "[\e[1;34mEn cours\e[0m] Création base de données"
			@script/install
			@$(JVM) -cp $(BIN):$(JAR) model.players.Learning 
			@rm -rf binary
			@echo -e "[\e[1;32mOK\e[0m] Base de données créée"

#################
# Lancement jeu #
#################

# Lancement avec compilation si besoin
terminal: clear_binary compile
			@clear
			@$(JVM) -cp $(BIN):$(JAR) Main


graphic: clear_binary compile
			@clear
			@$(JVM) -cp $(BIN):$(JAR) Main 1 1> /dev/null 2> /dev/null


#################
# Documentation #
#################

# créer la doc
docs:
			@clear
			@echo "Création de la documentation"
			-@javadoc -charset utf8 -author -d $(DOC) -Xdoclint:none -sourcepath $(SOURCE) $(SOURCE)**/*.java $(SOURCE)*.java  $(SOURCE)**/**/*.java 2>/dev/null 1>/dev/null
			@echo -e "[\e[1;32mOK\e[0m] Création documentation dans \e[1;35mdocs/\e[0m"


#########
# Clear #
#########

clear: clear_binary clear_doc
# Vider les binaires
ifeq ($(shell test -e binary && echo yes),yes)
clear_binary:
		@rm -Rf $(BIN) 
		@echo -e "[\e[1;32mOK\e[0m] Binaires supprimés"
else
clear_binary:
	@echo "Aucun binaire à supprimer"
endif

# Supprimer la doc
ifeq ($(shell test -e docs && echo yes),yes)
clear_doc:
		@rm -Rf $(DOC) 
		@echo -e "[\e[1;32mOK\e[0m] Documentation supprimée"
else
clear_doc:
	@echo "Aucune doc à supprimer"
endif

clean: clear
		@echo ''Suppression de la base de données''
		@echo -e "[\e[1;34mEn cours\e[0m] Supression base de données"
		@script/remove
		@echo -e "[\e[1;32mOK\e[0m] Base de données supprimée"


###########
# Version #
###########

# Affichage de la version
version:
			@echo "Jeu du Blokus    © Groupe Blokus 1 - S4 : Année 2018"
