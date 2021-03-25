#!/usr/bin/env python

from collecteur import parser
from collecteur import precalcul as PC
from graph import index
from graph import linker
from graph.matrix import Matrice, Vecteur
import graph.matrix as MA
from collecteur import utilities as UTIL

if __name__ == "__main__":
    parser.collect(False)

    links = linker.load_links("ressources/linkers.txt")
    m = Matrice.build_from(links)
    m.write_matrix("ressources/cli.txt")

    def pagerank():
        m.exec_and_store_pagerank(50, "ressources/pr.txt")
    UTIL.chrono("page_rank", pagerank)

    def create_score_m():
        PC.create_score_m(parser.dico, parser.score)
    UTIL.chrono("score", create_score_m)
