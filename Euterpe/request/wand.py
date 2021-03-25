from collecteur import utilities as UTIL
from graph.matrix import Matrice, Vecteur
from operator import itemgetter, attrgetter
from request import simple as SIMPLE
from request import tas as TAS
from collecteur import dico as DICO
from collecteur import precalcul as PC
import re
import math

idf_file = "ressources/idf.txt"
coeff_file = "ressources/coeff.txt"
freq_file = "ressources/freq.txt"
freq_ordered_file = "ressources/freq_ordered.txt"
freq2_file="ressources/out2.txt"
nd_file = "ressources/nd.txt"
pr_file = "ressources/pr.tx"
tf_file = "ressources/tf.txt"
p_file = "ressources/pr.txt"

pageranks = Matrice.get_pagerank(p_file)


beta = 0.5
alpha = 0.5

def ordered_page_line(output):
    """
    pour chaque mot , on ordonne les pages suivant pagerank
    :param output: le fichier de la relation mot page ordonnée
    :return:
    """
    print("Début de tri des pages fréquences")
    with open(freq_file, 'r') as fd:
        k = 0
        for line in fd:
            print("\rIteration {0}/{1}".format(k, 10000), end="")
            k += 1

            elems = UTIL.read_frequencies(line)
            ll = []
            for elem in (elems):
                pg=pageranks[int(elem[0])]
                elem=elem+(pg,)

                ll.append(elem)
            pg_list = sorted(ll, key=itemgetter(2), reverse=True)

            with open(output, 'a') as out:
                for i in range(len(pg_list)):
                    out.write("(" + str(pg_list[i][0]) + "," + str(pg_list[i][1]) + ")")
                out.write("\n")
    print("\nFin de tri des pages fréquences")
    return None

def ordered_tf(output):
    """
    pareil que pour les fréquences mais sur les tf
    """
    print("Début de tri des tf")
    with open(tf_file, 'r') as fd:
        k = 0
        for line in fd:
            print("\rIteration {0}/{1}".format(k, 10000), end="")
            k += 1

            elems = UTIL.read_pairs_file(line)
            ll = []
            for elem in (elems):
                pg=pageranks[int(elem[0])]
                elem=elem+(pg,)

                ll.append(elem)
            pg_list = sorted(ll, key=itemgetter(2), reverse=True)

            with open(output, 'a') as out:
                for i in range(len(pg_list)):
                    out.write("(" + str(pg_list[i][0]) + "," + str(pg_list[i][1]) + ")")
                out.write("\n")
    print("\nFin de tri des tf")
    return None

def pages_for_word(w):
    idf_w = SIMPLE.find_idf_for_r("ressources/idf.txt", [w])
    tfs = SIMPLE.find_tfs_for_w("ressources/ordered_tf.txt", w)
    nd = PC.get_ND("ressources/nd.txt")

    size_pages = len(tfs)
    pages = [(int(tf[0]), (idf_w * (float(tf[1]) / nd[int(tf[0])])), None) for tf in tfs]

    max = pages[-1][1]
    for i in range(size_pages-1, -1, -1):
        if pages[i][1] > max:
            max = pages[i][1]
        pages[i] = (pages[i][0], pages[i][1], max)

    return pages

def get_pages_scores_for_r(r):
    all_pages = []
    for x in r:
        pages = pages_for_word(x)
        all_pages += [pages]

    return all_pages

def init_pointeurs(r, all_pages):
    return [(w, pages[0][0], 0) for (w, pages) in zip(r, all_pages)]

def sort_pointeurs(pointeurs):
    def cmp_to_key(mycmp):
        'Convert a cmp= function into a key= function'
        class K:
            def __init__(self, obj, *args):
                self.obj = obj
            def __lt__(self, other):
                return mycmp(self.obj, other.obj) < 0
            def __gt__(self, other):
                return mycmp(self.obj, other.obj) > 0
            def __eq__(self, other):
                return mycmp(self.obj, other.obj) == 0
            def __le__(self, other):
                return mycmp(self.obj, other.obj) <= 0
            def __ge__(self, other):
                return mycmp(self.obj, other.obj) >= 0
            def __ne__(self, other):
                return mycmp(self.obj, other.obj) != 0
        return K

    def compare(x, y):
        return pageranks[x[1]] - pageranks[y[1]]

    return sorted(pointeurs, key=cmp_to_key(compare))

def remove_finished_pointeurs(pointeurs, dico, all_pages):
    for i in range(len(pointeurs)):
        (w, doc, idx) = pointeurs[i]
        if idx >= len(all_pages[dico[w]]):
            # Il faut supprimer, on a plus rien à regarder
            del pointeurs[i]

def find_pivot(n, pointeurs, all_pages, dico, nr, gamma):
    # On parcourt jusqu'a trouver le pivot
    acc = 0.0

    (_, d1, _) = pointeurs[0]
    pd1 = pageranks[d1]

    for i in range(n):
        (w, doc, index) = pointeurs[i]
        acc += all_pages[dico[w]][index][2]
        curr_score = (beta * pd1) + ((alpha / nr) * acc)
        if curr_score >= gamma:
            # Susceptible d'ajouter au tas
            return (i, doc)
    return (None, None)

def shift_to_j(pointeurs, j, dj, all_pages, dico):
    pdj = pageranks[dj]
    for i in range(j):
        (w, doc, index) = pointeurs[i]

        new_index = index
        borne = len(all_pages[dico[w]])
        while (new_index < borne and pageranks[doc] > pdj):
            new_index += 1

def add_to_tas(pointeurs, tas, dj, dico, n, all_pages, nr):
    # On regarde le nombre de pages qui contienent le pivot
    occs = 0
    for (w, doc, index) in pointeurs:
        if doc == dj:
            occs += 1

    if occs/n >= 0.5:
        acc = 0.0

        for i, (w, d, idx) in enumerate(pointeurs):
            if d == dj:
                try:
                    acc += all_pages[dico[w]][index][2]
                    new_idx = idx + 1
                    pointeurs[i] = (w, all_pages[dico[w]][new_idx][0], new_idx)
                except:
                    print("index: " + str(index))

        score = (beta * pageranks[dj]) + ((alpha * nr) * acc)
        if score >= tas.gamma():
            tas.put((dj, score))

def get_idfs(r):
    v = [find_idf_for_r("ressources/idf.txt", [w]) for w in r]
    nr = 0.0
    for x in v:
        nr += x ** 2
    nr = math.sqrt(nr)
    return [x/nr for x in v]

def wand(request, k=50):
    """
    Retourne les k meilleures pages
    """

    r = SIMPLE.transf_request_to_indexes(request)
    n = len(r)
    print("Requête:[" + request + "] => " + str(r))

    (Vr, Nr) = get_idfs(r)

    all_pages = get_pages_scores_for_r(r)

    tas = TAS.Tas(k)
    pointeurs = init_pointeurs(r, all_pages)

    dico = dict()
    index = 0
    for x in r:
        dico[x] = index
        index += 1

    while (True):
        pointeurs = sort_pointeurs(pointeurs)

        gamma = tas.gamma()
        (j, dj) = find_pivot(n, pointeurs, all_pages, dico, Nr, gamma)
        if j is None and dj is None:
            print("je ne sais pas quoi faire")
            import sys
            sys.exit(0)
        shift_to_j(pointeurs, j, dj, all_pages, dico)
        add_to_tas(pointeurs, tas, dj, dico, n, all_pages, Nr)
        remove_finished_pointeurs(pointeurs, dico, all_pages)

        print(pointeurs)
