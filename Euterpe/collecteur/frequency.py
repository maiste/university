from collecteur import utilities as UTIL
from graph import index as IDX
import json
import os
from random import randrange

freq = dict()
id_page = 0
id_file = 0

def frequency_for_page(page, dico, titles):
    aux = dict()
    weight = 8

    for word_page in page.split(' '):
        # On détermine si le mot appartient au titre de la page, lui affectant
        # alors un poids plus fort
        to_add = 1
        if word_page in titles[id_page].split(' '):
            to_add += weight

        if word_page in aux:
            aux[word_page] += to_add
        else:
            aux[word_page] = to_add

    for word in dico:
        if word in aux:
            count = aux[word]
            if word in freq:
                if count > 0:
                    freq[word].append((id_page, count))

def create_frequency(pages_file, dico_path, freqs_dir_path, freq_path, indexes_path):
    """
    Entry point of frequency module
    """
    documents_size = UTIL.get_corpus_size()
    titles = IDX.load_index_id(indexes_path)

    dico = []
    with open(dico_path, 'r') as fd:
        acc = 0
        for word in fd:
            acc += 1
            word = word.rstrip()
            dico.append(word)
            freq[word] = []

    def frequency_in_page(page):
        global id_page
        frequency_for_page(page, dico, titles)
        id_page += 1

    # Create temp files
    UTIL.iter_page(pages_file, frequency_in_page)

    # Merge temp files
    with open(freq_path, 'w') as out:
        for _, words_list in freq.items():
            to_add = ""
            if len(words_list) == 0:
                choosen_d = randrange(documents_size)
                to_add += "(" + str(choosen_d) + ",1)"
            for elem in words_list:
                id = str(elem[0])
                occ = str(elem[1])
                to_add += "(" + id + "," + occ + ")"

            to_add += "\n"
            out.write(to_add)
    return None
