from collecteur import utilities as UTIL
from collecteur import dico as DICO
from collecteur import precalcul as PC
from graph.matrix import Matrice, Vecteur
from graph.index import load_index, load_index_id
from operator import itemgetter, attrgetter
import math
import re
import random

idf_file = "ressources/idf.txt"
coeff_file = "ressources/coeff.txt"
freq_file = "ressources/freq.txt"
nd_file = "ressources/nd.txt"
pr_file = "ressources/pr.txt"
tf_file = "ressources/tf.txt"
score_file = "ressources/score_m.txt"

def find_nd_for_d(nd_file, d):
    index = 0
    with open(nd_file, 'r') as fd:
        for line in fd:
            if index == d:
                return float(line.rstrip())
            index += 1
    return None

def find_in_idf(idf_file, r, f):
    acc = 0.0

    number_of_words_done = 0
    max_number_of_words = len(r)

    with open(idf_file, 'r') as fd:
        index = 0
        for line in fd:
            if index in r: # if word is in requested words
                acc += f(line)

                # We may abort early
                number_of_words_done += 1
                if number_of_words_done == max_number_of_words:
                    return acc

            index += 1
    return acc

def find_idf_for_r(idf_file, r):
    def f(line):
        return float(line.rstrip())

    return find_in_idf(idf_file, r, f)

def find_nr_for_r(idf_file, r):
    def f(line):
        return float(line.rstrip()) ** 2

    return find_in_idf(idf_file, r, f)


def find_tfs_for_w(tf_path, w):
    with open(tf_path, 'r') as fd:
        index = 0
        for line in fd:
            if index == w:
                return UTIL.read_pairs_file(line)
            index += 1
    return []

dico = DICO.load_dico_as_dict_inverse("ressources/dico.txt")
def transf_request_to_indexes(request):
    # Request is now associated indexes
    r = []
    normalized = UTIL.normalize_text(request)
    for w in normalized.split(' '):
        if w in dico:
            r.append(dico[w])

    return r

def get_idfs(r):
    v = [find_idf_for_r("ressources/idf.txt", [w]) for w in r]
    acc= 0.0
    for x in v:
        acc += x ** 2
    nr = math.sqrt(acc)
    return (v, acc, nr)

def get_pages(r):
    res = []

    for w in r:
        res += [[(int(x), float(y)) for (x,y) in find_tfs_for_w("ressources/tf.txt", w)]]

    return res

def find_same_pages(all_pages, pointeurs):
    len_pages = [len(pages) for pages in all_pages]

    def index_still_in(pointeurs, len_pages):
        # Return true if the indexes are still in all_pages
        for ((_, _, idx), n) in zip(pointeurs, len_pages):
            if idx >= n:
                return False
        return True

    def find_max(pointeurs):
        max = 0
        for (_, idx_page, idx) in pointeurs:
            x = all_pages[idx_page][idx][0]
            if x > max:
                max = x
        return max

    res = []
    while (index_still_in(pointeurs, len_pages)):
        max = find_max(pointeurs)

        good = True
        for i in range(len(pointeurs)):
            [_, idx_page, idx] = pointeurs[i]
            if (all_pages[idx_page][idx][0] != max):
                pointeurs[i][2] = idx + 1
                good = False

        if (good):
            res.append(all_pages[0][pointeurs[0][2]])
            for i in range(len(pointeurs)):
                pointeurs[i][2] += 1

    return res


pageranks = Matrice.get_pagerank("ressources/pr.txt")

def page_rank_mid():
    x = float(0)
    for v in pageranks.values():
        x += v
    return float(x) / float(len(pageranks))


def get_tf_from(d, same_pages):
    for (id, tf) in same_pages:
        if id == d:
            return tf
    return -1

def fd(tf, nd, sum_idf, d, nr):
    return (sum_idf * tf) / (nd[d] * nr)

def false_request(dico):
    n = len(dico)
    m1 = random.randint(0,n)
    m2 = random.randint(0,n)
    return dico[m1] + " " + dico[m2]


def fd_per_request(req):
    x = 0
    nd = PC.get_ND("ressources/nd.txt")
    nb = 0

    r = transf_request_to_indexes(req)
    (_, sum_idf, nr) = get_idfs(r)
    all_pages = get_pages(r)
    if all_pages == []:
        return x
    pointeurs = [[w, i, 0] for i, w in enumerate(r)]
    same_pages = find_same_pages(all_pages, pointeurs)
    if same_pages == []:
        return x

    len_same_pages = len(same_pages)

    while nb < 1000:
        d = random.randint(0, len_same_pages-1)
        d = same_pages[d][0]

        tf = get_tf_from(d, same_pages)
        if tf == -1:
            continue
        nb += 1
        x += fd(tf, nd, sum_idf, d, nr)
    return x


def fd_mid():
    """
    Try to compute an average from a random computing
    """
    print("COMPUTE ALPHA")
    nb = float(0)
    x = float(0)
    dico = DICO.load_dico_as_dict("ressources/dico.txt")

    while nb < 1000000:
        r = false_request(dico)
        x += fd_per_request(r)
        nb += float(1000)
        print("\r{}".format(nb), end="")
    print("DONE\n")
    return float(x / nb)


# alpha = 0.9
# beta = 0.1

# alpha = page_rank_mid() / fd_mid()
# beta = 1 - alpha
# print("beta: ", alpha) # Value 5.8042711010239384e-05
# print("beta: ", beta) # Value 0.9999419572889897

alpha = 5.8042711010239384e-05
beta = 0.9999419572889897

def pages_scores(same_pages, sum_idf, nr, nd):
    def score(d, tf):
        pd = pageranks[d]
        bpd = beta * pd

        #fdr = (sum_idf * tf) / (nd[d] * nr)
        fdr = (sum_idf * tf)
        afdr = alpha * fdr
        # print("Compute score", d, "=>", sum_idf, tf, nd[d], nr, "// fdr", fdr, " //true fdr", fdr / (nd[d])* nr)
        return afdr + bpd

    return [(d, score(d, tf)) for (d, tf) in same_pages]

def simple(request):
    # Init
    r = transf_request_to_indexes(request)
    if len(r) == 1:
        return nocompute_request(r)
    else:
        return normal_request(r)

def normal_request(r):
    (_, sum_idf, nr) = get_idfs(r)
    nd = PC.get_ND("ressources/nd.txt")
    all_pages = get_pages(r)

    if  all_pages == []:
        return []

    pointeurs = [[w, i, 0] for i, w in enumerate(r)]

    # Get same pages
    same_pages = find_same_pages(all_pages, pointeurs)

    # Get scores for pages
    pages = pages_scores(same_pages, sum_idf, nr, nd)

    # Sort pages
    pages = sorted(pages, key=itemgetter(1), reverse=True)

    # Return pages
    score = [d for (d, _) in pages]
    return score

def nocompute_request(r):
    score_m = PC.get_score_m(score_file)
    return score_m.get(r[0], [])
