import xml.etree.ElementTree as ET
from collecteur import utilities as UTIL
import re
import unidecode as UNI

auto = re.compile(r"\[\[(.*)\]\]")

def write_links_on_disk(links, index, path):
    """
    Write a list on the disk
    """
    with open(path, 'a') as fd:
        for elt in links:
            number = index.get(elt, None)
            if number is not None:
                fd.write("{} ".format(number))
        fd.write("\n")
    return

def normalize(match):
    """
    Keep the right links
    :param match: the match line
    :return: a clear line
    """
    partition = match.split("|")
    normal_form = ""
    if len(partition) >= 1:
        normal_form = partition[0]
    return normal_form


def match_link(text):
    """
    Get the links in the text
    :param text: the text
    :return: the list of all th matching links
    """
    res = set ()
    for x in re.findall(r"\[\[([\w|\s]*)\]\]", text):
        res.add(normalize(x))
    return list(res)


def create_links_page(index, file, page):
    """
    Create a link from a page
    :file: where to write
    :page: the content to work on
    """
    links = dict()
    xml_page = ET.ElementTree(ET.fromstring(page))
    xml_text = xml_page.getroot().find('text').text
    links = []
    if xml_text is not None:
        links = match_link(xml_text)
    write_links_on_disk(links, index, file)
    return


def load_links(file):
    #print("Load links:", end="")
    links = dict()
    with open(file, 'r') as fd:
        id_page = 0
        for line in fd:
            links[id_page] = []
            for link in line.split(" "):
                link_norm = link.rstrip()
                if link_norm != '':
                    links[id_page].append(link_norm)
            links[id_page].sort(key=int)
            id_page += 1
    #print("DONE")
    return links

