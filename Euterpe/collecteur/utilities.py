# Meant for normalize words
import re
import unidecode
import spacy
from nltk.corpus import stopwords
from nltk.stem.snowball import SnowballStemmer
import time
import datetime

def chrono(name, f):
  start = time.time()
  f()
  end = time.time()
  time_format = time.strftime("%d - %H:%M:%S", time.gmtime(end - start))
  print(name, "takes ", time_format)

# Globals
stemmer = SnowballStemmer(language='french')
nlp = spacy.load("fr_core_news_sm")
stopwords = set(stopwords.words('french'))
nlp.max_length = 2500000

def iter_page(xml_file, func):
    """
    Apply func to every page in xml_file
    :param xml_file: path to xml pages
    :param func: function called on every page
    :return: None
    """
    start_tag = "<page>"
    end_tag = "</page>"
    current_page = ""
    ongoing_page = False

    count = 0
    with open(xml_file) as f:
        for line in f:
            if start_tag in line:
                ongoing_page = True
                current_page += line
            elif end_tag in line:
                current_page += line

                func(current_page)
                count += 1
                current_page = ""
                ongoing_page = False
            elif ongoing_page:
                current_page += line

def set_corpus_size(nb):
    """
    Return the number of pages in the corpus
    """
    with open("ressources/size_corpus.txt", 'w') as fd:
        fd.write(str(nb))

def get_corpus_size():
    with open("ressources/size_corpus.txt", 'r') as fd:
        return int(fd.read().rstrip())

def normalize_word(word, removed_words = stopwords):
    """
    Normalize the word
    """
    word = unidecode.unidecode(word.lemma_.rstrip())
    test = re.findall(r"[!\"#$%&()*+’,-./:;<=>«»?@\[\]^_`{|}~]+|'{2,5}|http(s)?://\S+|www.\S+", word)
    if len(test)!=0 or word in removed_words or len(word)<3:
        return None
    return word

def normalize_text(text, removed_words = stopwords):
    """
    Normalize a text
    """
    tmp = []
    text = text.lower()
    for word in nlp(text):
        word = normalize_word(word)
        if word:
            tmp.append(word)
    return ' '.join(tmp)

def remove_html_tags(text):
    clean = re.compile('<.*?>')
    return re.sub(clean, '', text)

def read_frequencies(line):
    elems = re.findall(r"\(([0-9]+),([0-9]+)\)", line)
    return elems

def read_pairs_file(line):
    return re.findall(r"\(([0-9]+),([0-9]+\.[0-9]+)\)", line)
