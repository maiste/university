
def compose (f,g,x) = ?(f) (?(g) (x))

def square (x) = x*x

def pow4(x) = compose (&square,&square,x)

val _ = print_int(pow4(3))
val _ = print_string("\n")
