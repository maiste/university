#! /bin/bash

function test_existence_mpsh {
    which mpsh >& /dev/null
    if [ "$?" != "0" ]
    then
        echo -e "\033[0;31m!!! mpsh n'est pas accessible de tout répertoire, le test ne peut être fait !!!\033[00m"
        exit 1
    fi
}

function mise_en_place {
    cd
    mkdir Test_schtroumpf
    cd Test_schtroumpf
    cat /etc/bash.bashrc > schtroumpf
    echo rien > titi
    mkdir rep
}

function test_shell {
    echo "mkdir Azrael
ls
ls gargamel
SCHTROUMPF=345
echo \$SCHTROUMPF
echo \$GARGAMEL
exit
" > /tmp/schtroumpf_$USER
    $1 < /tmp/schtroumpf_$USER > /tmp/rep_$USER.$1 2>/tmp/err_$USER.$1
    echo "rmdir Azrael" > /tmp/schtroumpf_$USER
    bash < /tmp/schtroumpf_$USER
}

function nettoyage {
    rm -fr $HOME/Test_schtroumpf
    rm -f /tmp/schtroumpf_$USER
    rm -f /tmp/rep_$USER.bash /tmp/rep_$USER.mpsh /tmp/err_$USER.bash /tmp/err_$USER.mpsh
}

echo -e "Le fonctionnement de ce test n'est garanti que sur les machines de l'ufr."
echo -e "Le terminal doit être suffisament large pour que le résultat soit lisible."
echo -e "La commande mpsh doit pouvoir s'exécuter quel que soit le répertoire courant.\n\n"

test_existence_mpsh

mise_en_place
test_shell bash
test_shell mpsh

sed -i -e 's/bash/mpsh/g' /tmp/rep_$USER.bash
sed -i -e 's/bash/mpsh/g' /tmp/err_$USER.bash
sed -i -e 's/\/bin\/ls/ls/g' /tmp/err_$USER.bash

echo -e "*** sorties obtenues (à gauche la sortie théorique, à droite la vôtre)\n"
diff -W 70 -y /tmp/rep_$USER.bash /tmp/rep_$USER.mpsh
if [ $? -eq 0 ]
then echo -e "\033[0;32m\n*** sortie correcte\n\n\033[00m"
else echo -e "\033[0;31m\n*** des erreurs dans la sortie\n\n\033[00m"
fi

echo -e "****************************************************************************\n"

echo -e "*** sorties erreurs obtenues (à gauche la sortie théorique, à droite la vôtre)\n"
diff -W 160 -y /tmp/err_$USER.bash /tmp/err_$USER.mpsh
if [ $? -eq 0 ]
then echo -e "\033[0;32m\n*** sortie erreur correcte\n\n\033[00m"
else echo -e "\033[0;31m\n*** des erreurs dans la sortie erreurs\n\n\033[00m"
fi

nettoyage

exit 0
