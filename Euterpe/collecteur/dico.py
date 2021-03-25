import xml.etree.ElementTree as ET
from collecteur import utilities as UTIL
import spacy
import re
import unidecode

dic = dict()
size_dico = 200000

def add_occurences(text):
    """
    :param text: string text
    :return: None
    """
    def f(dic, word):
        if len(word) > 1:
            if word in dic:
                dic[word] += 1
            else:
                dic[word] = 1

    words = text.split(' ')
    for word in words[1:-1]:
        f(dic, word)

    #First and last words may contains <page> or </page>
    f(dic, UTIL.remove_html_tags(words[0]).strip())
    f(dic, UTIL.remove_html_tags(words[-1]).strip())

    return None


def get_n_keys(dic, titles, curr_n, max_n):
    """
    :param max_n: number of most frequent word requested (default max_dict_length)
    :return: array of word
    """
    res = list(titles)

    acc = 0
    for w in dic.keys():
        w.strip()
        if w not in titles:
            res.append(w)
            acc += 1

        if acc >= max_n:
            break

    return res

def export_dico(dico_path, keys):
    keys_str = ""
    for k in keys:
        keys_str += k + "\n"

    with open(dico_path, "w") as fd:
        fd.write(keys_str)
    return None

def add_titles_to_keys(idx_file, n):
    """
    :param idx_file: path to idx file
    :param res_keys: current keys extracted from dictionnary
    :param n: number of titles to add
    """
    titles = set()

    with open(idx_file, 'r') as fd:
        for line in fd:
            for word in line.split(' '):
                word = word.strip()
                if len(word) > 0:
                    titles.add(word)

    len_titles = len(titles)
    if len_titles < n:
        return (titles, len_titles)
    else:
        tmp = list(titles)
        tmp = tmp[:n]

        s = set(tmp)

        return (s, n)

def create_dico(pages_file, dico_path, idx_file):
    """
    Entry point for dictionnary

    :param pages_file: path for xml pages
    :param dico_path: path for dictionnary
    :param removed_path: path for removed words
    :return: None
    """

    UTIL.iter_page(pages_file, add_occurences)

    # Get titles from corpus, maximum size_dico/
    (titles, n) = add_titles_to_keys(idx_file, size_dico//2)
    #print("n: " + str(n) + ", len(titles): " + str(len(titles)))

    # Get min(size_dico/2) best result, it fills the dictionnary to 200k
    # elements, based on number of titles added previously
    res_keys = get_n_keys(dic, titles, n, size_dico-n)
    res_keys = sorted(res_keys)

    #print("len(res_keys): " + str(len(res_keys)))
    #print("len(set(res_keys)): " + str(len(set(res_keys))))
    # Export to .txt
    export_dico(dico_path, res_keys)
    return None

def load_dico_as_dict(dico_path):
    """
    Load a dico as a dict
    :param: dico_path: path of the dict
    :return: a dict
    """
    id = 0
    dico = dict()
    with open(dico_path, 'r') as fd:
        for word in fd:
            dico[id] = word.strip()
            id += 1
    return dico

def load_dico_as_dict_inverse(dico_path):
    """
    Comme celle d'avant mais inverse index/value
    """
    id = 0
    dico = dict()
    with open(dico_path, 'r') as fd:
        for word in fd:
            dico[word.strip()] = id
            id += 1
    return dico

def remove_capital_letters(dico_path):
    with open("ressources/new_dico.txt", 'w') as out:
        with open(dico_path, 'r') as fd:
            for line in fd:
                out.write(line.lower())
