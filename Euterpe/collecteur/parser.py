import xml.etree.ElementTree as ET
import re
from io import StringIO
from collecteur import debug as DBG
from collecteur import dico as DICO
from collecteur import utilities as UTIL
from collecteur import frequency as FREQ
from collecteur import precalcul as PC
from collecteur.automata import *
from xml.dom import minidom
from graph import index as IDX
from graph import linker as LINK


def append_xml_to_file(page, xml_output):
    # Might be heavy
    xmlstr = minidom.parseString(ET.tostring(page.getroot())).toprettyxml(indent="   ")
    with open(xml_output, "a") as fd:
        fd.write(xmlstr)

    return None


def filter_xml_pages(xml_file, xml_output, idx, idx_dirty, debug=False):
    """
    Parse pages ->
        if the page belongs to the them -> clean page

    :param xml_file: xml file where we extract pages
    :return: None
    """
    def add_page(page):
        xml_page = ET.ElementTree(ET.fromstring(page))
        clean_xml = clean_page(xml_page, idx, idx_dirty, debug)
        if clean_xml is not None:
            append_xml_to_file(clean_xml, xml_output)
    #print("Running filter")
    UTIL.iter_page(xml_file, add_page)
    return None

nb_pages = 0
def test_corpus(xml_file, xml_output, debug=False):
    def is_theme(page):
        try:
            theme = r"([mM]usique|piano|guitare|rock)"
            regex = re.compile(theme)
            resultat = regex.findall(page)
            if not resultat:
                return False
            else:
                return True
        except:
            return False
    def is_good_ns(page):
        try:
            theme = r"(<ns>0<\/ns>)"
            regex = re.compile(theme)
            resultat = regex.findall(page)
            if not resultat:
                return False
            else:
                return True
        except:
            return False
    #print("Running keeper")
    def add_page(page):
        global nb_pages
        if is_theme(page) and is_good_ns(page):
            with open(xml_output, "a") as fd:
                fd.write(page)
                nb_pages += 1

    UTIL.iter_page(xml_file, add_page)
    UTIL.set_corpus_size(nb_pages)
    return None


def is_theme(page, theme):
    """
    :param page: xml object page
    :param theme: regexp (or word?) we're looking for
    :return: list of pages that match the theme
    """
    try:
        page_root = page.getroot()
        revision = page_root.find('revision')
        if revision is None:
            return False
        text_tr = revision.find('text')
        if text_tr is None:
            return False
        text = text_tr.text
        regex = re.compile(theme)
        resultat = regex.findall(text)
        if not resultat:
            return False
        else:
            return True
        return True
    except:
        return False


def add_title(page, xml_tree):
    """
    Take a page and add the title to xml tree
    :param page: an xml object
    :param xml_tree: the xml tree representing the new file
    :return the title or None in case of error
    """
    page_title = page.find('title')
    if page_title is None:
        return None
    xml_title = ET.SubElement(xml_tree, "title")
    xml_title.text = page_title.text
    return xml_title.text


def add_id(page, xml_tree):
    """
    Take a page and add the id to xml tree
    :param page: an xml object that represents the page
    :param xml_tree the xml tree representing the new page object
    :return the id or None in case of error
    """
    page_id =page.find('id')
    if page_id is None:
        return None
    xml_id = ET.SubElement(xml_tree, "id")
    xml_id.text = page_id.text
    return xml_id.text


def clean_text_from_acc(text):
    """
    Return the text without the double bracket
    :param text: the text to clean
    """
    amc = Automata()
    clear_text = ""
    for c in text:
        amc.one_step(c)
        if amc.ignore():
            continue
        if amc.must_add_bracket():
            clear_text = clear_text + '{'
        clear_text = clear_text + c
    return clear_text


def clean_text_from_title(text, title=""):
    """
    Return the text without external link
    :param text: the text to clean
    """
    clear_text = ""
    auto = Remover(title)
    for s in StringIO(text):
        if auto.one_step(s):
            continue
        else:
            clear_text += s
    return clear_text


def clean_text(page, xml_tree, debug=False):
    """
    Clean the text of the page
    :param page: the original page
    :param xml_tree: the new page where to add content
    :param debug: activate debug display
    """
    revision = page.find('revision')
    if revision is None:
        return None
    text_tree = revision.find('text')
    if text_tree is None:
        return None
    text = text_tree.text
    text = clean_text_from_acc(text)
    text = clean_text_from_title(text, "Liens externes")
    text = clean_text_from_title(text, "Bibliographie")
    text = clean_text_from_title(text, "Notes")
    xml_text = ET.SubElement(xml_tree, "text")
    xml_text.text = text
    return text


def clean_page(page, idx, idx_dirty, debug=False):
    """
    :param page: xml page
    :return: xml cleaned page
    """
    tree_page = ET.ElementTree()
    page_root = page.getroot()
    new_page = ET.Element("page")

    DBG.start_title("NEW PAGE -> INSERT", debug)
    title = add_title(page_root, new_page)
    if title is None:
        return None
    # Title can be added to indexes
    ntitle = UTIL.normalize_text(title)
    IDX.create_index_title(title, ntitle, idx, idx_dirty)

    DBG.tag("title", title, debug)
    id = add_id(page_root, new_page)
    if id is None:
        return None
    DBG.tag('id', id, debug)
    text = clean_text(page_root, new_page, debug)

    if text is None:
        return None
    DBG.tag('text', text, debug)
    DBG.end_title("NEW PAGE -> INSERT", debug)
    tree_page._setroot(new_page)
    return tree_page

def lemmatize_corpus(clean_corpus, output_corpus, idx_dirty, links_file):
    """
    Lemmatize and create links for the whole corpus

    :param clean_corpus: xml pages
    :param links_file: output file for links
    """

    output = open(output_corpus, 'w')
    output.write("")
    output.close()

    output = open(output_corpus, 'a')

    indexes = IDX.load_index(idx_dirty)

    def lemmatize(page):
        LINK.create_links_page(indexes, links_file, page)

        page = UTIL.remove_html_tags(page)
        page = UTIL.normalize_text(page)

        page = "<page>\n" + page + "\n</page>\n"
        output.write(page)

    UTIL.iter_page(clean_corpus, lemmatize)
    #UTIL.set_corpus_size()
    return None


# File paths
frwiki = "ressources/frwiki10000.xml"
corpus = "ressources/corpus.xml"
clean_corpus = "ressources/clean_corpus.xml"
lemm_corpus = "ressources/lemm_corpus.xml"

idx = "ressources/idx.txt"
idx_dirty = "ressources/idx_dirty.txt"
linkers = "ressources/linkers.txt"

dico = "ressources/dico.txt"
freq = "ressources/freq.txt"
freq_dir = "ressources/freqs/"

tf = "ressources/tf.txt"
idf = "ressources/idf.txt"
nd = "ressources/nd.txt"
score = "ressources/score_m.txt"

def collect(debug=False):
    # Create corpus
    def test():
        test_corpus(frwiki, corpus)
    UTIL.chrono("test_corpus", test)

    def filter():
        filter_xml_pages(corpus, clean_corpus, idx, idx_dirty, debug)
    UTIL.chrono("filter_xml_pages", filter)

    def lemm():
        lemmatize_corpus(clean_corpus, lemm_corpus, idx_dirty, linkers)
    UTIL.chrono("lemmatize_corpus", lemm)

    # Create dictionnary
    def create_dico():
        DICO.create_dico(lemm_corpus, dico, idx)
    UTIL.chrono("create_dico", create_dico)

    # Creation frequency (relation mot pages)
    def create_freq():
        FREQ.create_frequency(lemm_corpus, dico, freq_dir, freq, idx)
    UTIL.chrono("create_freq", create_freq)

    # Creation of IDF/TF/ND
    def create_idf():
        PC.idf(freq, idf)
    UTIL.chrono("idf", create_idf)

    def create_tf():
        PC.tf(dico, freq, tf)
    UTIL.chrono("tf", create_tf)

    def create_nd():
        PC.create_ND(tf, nd)
    UTIL.chrono("nd", create_nd)


    return None
