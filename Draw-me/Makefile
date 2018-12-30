#######################################
#     Création de module graphique    #
#    Makefile   -   DURAND | MARAIS   #
#######################################


# Variables
file = simple2 
image = braddock.jpg
# Show information
all:
	@echo -e "Il faut d'abord compiler avec =>  make compile\n"
	@echo -e "Pour dessiner votre fichier : \033[1;32mmake run file=\"<filename>\"\033[0m.\nCe fichier doit être dans le répertoire \033[1;33mtest/\033[0m.\nPar défaut le fichier testé est simple2.\n"
	@echo -e "Pour contruire le dessin de votre image : \033[1;32mmake creator image=\"<filename>\"\033[0m.\nCe fichier doit être dans le répertoire \033[1;33mimg/\033[0m.\nLe dessin produit est placé dans le dossier test.\nPar défaut le fichier testé est braddock.jpg"

# Create Lexer
create_lexer:
	@echo -e "[\033[1;34mEn cours\033[0m] Création du Lexer"
	@jflex src/lexer/draw.flex 1> /dev/null
	@echo -e "[\033[1;32mOK\033[0m] Lexer créé"


# Compilation java
compile: create_lexer
	@echo -e "[\033[1;34mEn cours\033[0m] Compilation des fichiers java"
	@mkdir -p bin
	@javac -sourcepath src -d bin src/*.java
	@ echo -e "[\033[1;32mOK\033[0m] Fichiers compilés" 

# Clear files
clear:
	@echo -e "[\033[1;34mEn cours\033[0m] Suppression du fichier contenant les .class"
	@rm -Rf bin
	@echo -e "[\033[1;32mOK\033[0m] Fichiers supprimés"

$(file):
	@echo -e "============================\n\n    Interpréteur     \n \033[1;33mFichier> test/"$(file)"\033[0m\n\n============================\n"

# Lancer la compilation
run: $(file)
	@java -cp bin Main test/$(file) 0

creator: 
	@echo -e "============================\n\n    Créateur     \n \033[1;33mFichier> img/"$(image)"\033[0m\n\n============================\n"
	@java -cp bin Main img/$(image) 1
