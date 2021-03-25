import math
from collecteur import utilities as UTIL
from collecteur import dico as DICO
import re

def idf(freq_path, output):
    """
    IDF(m) = log10 (|D| / |d ∈ D : m ∈ d|)
    """
    numer = UTIL.get_corpus_size() # |d|
    with open(output, 'w') as out:
        with open(freq_path, 'r') as fd:
            for freq in fd:
                # Freq line is (id0, occ0)(id1,occ1)....(idN, occN)
                # We just need to count each '('

                denom = freq.count('(') # |d ∈ D : m ∈ d|
                #if denom == 0:
                    #print(freq)
                arg = numer / denom
                x = math.log10(arg)

                out.write(str(x) + "\n")
    #print("IDF Done")

def tf(dico_path, freq_path, output):
    """
    In output each line will be the tf of a certain word
    line0: (d0, tf(m, d0))...(dN, tf(m, dN)\n
    """
    def tf_aux(d, occ):
        """
        TF(m, d) =
          0 si m ∈ d
          1 + log10(#occ(m, d)) sinon

        The function is called only if m ∈ d by construction
        """
        return 1 + math.log10(occ)

    with open(freq_path, 'r') as fd:
        with open(output, 'w') as out:
            for line in fd:
                elems = UTIL.read_frequencies(line)

                for elem in elems:
                    d = elem[0]
                    occs = elem[1]

                    tf = tf_aux(d, int(occs))

                    out.write("(" + d + "," + str(tf) + ")")
                out.write("\n")

    #print("TF Done")

def create_ND(tf_path, nd_path):
    ND = [0.0] * UTIL.get_corpus_size()
    with open(tf_path, 'r') as fd:
        for line in fd:
            tfs = UTIL.read_pairs_file(line)
            for tf in tfs:
                d = int(tf[0])
                tf_value = float(tf[1])
                ND[d] += tf_value**2

    ND = [math.sqrt(x) for x in ND]

    with open(nd_path, 'w') as out:
        out.write(str(ND[0]))
        for x in ND[1:]:
            out.write(";" + str(x))
    #print("Done ND")

def get_ND(nd_path):
    with open(nd_path, 'r') as fd:
        return [float(x) for x in fd.readline().split(";")]

def create_score_m(dico_path, score_path):
    from request import simple as SPL # Dummy python, avoid execution befor call
    dico = DICO.load_dico_as_dict(dico_path)
    id = 0
    res = []
    for _ in dico.values():
        r = [id]
        id += 1
        score = SPL.normal_request(r)
        res.append(score)
    with open(score_path, 'w') as out:
        for pages in res:
            for page in pages:
                out.write(str(page) + " ")
            out.write("\n")
    return None

def get_score_m(score_path):
    score = dict()
    id = 0
    with open(score_path) as fd:
        for line in fd:
            score[id] = [int(x) for x in line.rstrip().split(" ") if x != '']
            id += 1
        return score
