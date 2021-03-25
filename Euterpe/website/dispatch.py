from flask import Flask, Response, render_template, request # Basics
from request import simple
import graph.index as index

app = Flask(__name__)
link_index = index.load_index_id("ressources/idx_dirty.txt")


def ask_searcher(req):
    print("New request >", req)
    seaker = simple.simple(req)
    print(seaker[0:5])
    resp = []
    for page_id in seaker:
        true_name = link_index.get(page_id, None)
        if true_name is not None:
            link_name = "https://fr.wikipedia.org/wiki/" + true_name.replace(" ", "_")
            resp.append((true_name, link_name))
    if resp == []:
        return None
    return resp



def treat_request(req):
    if req == "":
        print("Empty request")
        return { ("Erreur, requête vide.", "euterpe.live")}
    else:
        resp = ask_searcher(req)
        if resp is None:
            return { ("Nous n'avons pas trouvé de resultat.", "euterpe.live") }
        else:
            return resp




@app.route('/', methods= ['POST', 'GET'])
def search():
    if request.method == 'POST':
        ask = True
        search_request = request.form['search']
        return render_template("home.html", ask = ask,  answer = treat_request(search_request))
    else:
        ask = False
        return render_template("home.html", ask = ask, name = "")
