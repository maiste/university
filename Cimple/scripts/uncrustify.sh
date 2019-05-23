#!/bin/sh

G_PATH_PATH_UNCRUSTIFY="/usr/bin/uncrustify"
G_PATH_CONFIG="./scripts/config_uncrustify.cfg"

if [ ! -f ${G_PATH_PATH_UNCRUSTIFY} ]; then
        echo "The file ${G_PATH_PATH_UNCRUSTIFY} may not exist."
        echo "Verify that you have uncrustify installed..."
else
        if [ ! -f ${G_PATH_CONFIG} ]; then
            echo "Config file not found!"
            exit 0
        fi
        ret=$(find ./ -name "*.c")
        for elem in $ret; do
                uncrustify -c ${G_PATH_CONFIG} --replace --no-backup --mtime "${elem}"
        done
        exit 0
fi

exit 1
