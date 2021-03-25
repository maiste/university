def tag(tag, content, debug=False):
    """
    Function to display a tag
    :param tag: the tag to display
    :param content: the attribut
    :param debug: activate debug display
    """
    if debug:
        print("<{0}>{1}</{0}>".format(tag, content), end='\n')

def start_title(title, debug=False):
    """
    Function to display a title with a line
    :param title: The title to display
    :param debug: activate debug display
    """
    if debug:
        print("####### ({0}) #######".format(title), end='\n')

def end_title(title, debug=False):
    """
    Function to display the end of a title
    :param title: title to have the size
    :param debug: activate debug display
    """
    if debug:
        size = 18 + len(title)
        for i in range(0, size):
            print("#", end='')
        print("\n")

def msg(name, msg, debug=False):
    """
    Function to display a message
    :param name: the name of the msg
    :param msg: the message to display
    :param debug: activate debug display
    """
    if debug:
        print("[DEBUG] {0}: {1}".format(name, msg), end='\n')
