import xml.etree.ElementTree as ET
from collecteur import utilities as UTIL
import unidecode as UNI


def write_idx_on_disk(index_dirty, index_clean, path, path_dirty):
    """
    Write a list on the disk
    """
    with open(path, 'a') as fd:
        for name in index_clean:
            fd.write("{}\n".format(name))
    with open(path_dirty, 'a') as fd:
        for name in index_dirty:
            fd.write("{}\n".format(name))
    return []


def create_index_title(title, ntitle, path, path_dirty):
    """
    Write the index on the disk
    :title: the title to add.
    :ntitle: the title normalized
    :path: path to the normalize version
    :path_dirty: path from the common version
    """
    with open(path_dirty, 'a') as fd:
        fd.write("{}\n".format(title))
    with open(path, 'a') as fd:
        fd.write("{}\n".format(ntitle))
    return None

def load_index(file):
    #print("Load index:", end="")
    index = dict()
    with open(file, 'r') as fd:
        id_page = 0
        for name in fd:
            index[name.strip()] = id_page
            id_page += 1
    #print("DONE")
    return index

def load_index_id(file):
    #print("Load index id:", end="")
    index = dict()
    with open(file, 'r') as fd:
        id_page = 0
        for name in fd:
            index[id_page] = name.strip()
            id_page += 1
    #print("DONE")
    return index

