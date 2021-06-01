#!/usr/bin/env bash
# +----------------------------------+
# |   Test script for Transfo-prog   |
# +----------------------------------+
# |   Written by Marais / Chaboche   |
# |                                  |
# | WARNING: not really formal tests |
# | WARNING: should be launch from   |
# | the main directory               |
# +----------------------------------+
# | USE : JASMIN=path_exec ./test.sh |
# +----------------------------------+


set -e

printf "############## TEST SUITE #############\n"
printf "PURPOSE: Crash in case of error.\n\n"

for FILE in examples/*.fx
do
  printf "=== TEST: %s ===\n" $FILE
  file_javix_1=${FILE%.fx}.j
  file_javix=${FILE%.fx}.k
  file_java=${FILE%.fx}
  sbt "run $FILE" &&
  printf "Build jasmin->j\n" ; $JASMIN $file_javix_1 && java -noverify $file_java || true
  printf "\nBuild jasmin->k\n" ; $JASMIN $file_javix && java -noverify $file_java || true
  printf "\n\n\n\n"
done
printf "#######################################\n"
