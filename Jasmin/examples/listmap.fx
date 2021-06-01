def double (x) = 2*x

def nil () = [0]

def cons (x,l) = [1,x,l]

def map (f,l) =
  if l[0] == 0
  then nil()
  else cons(?(f)(l[1]),map(f,l[2]))

def sum(l) =
  if l[0] == 0
  then 0
  else l[1] + sum(l[2])

val example = cons(1,cons(2,cons(3,nil())))
val res = sum(map(&double,example))
val _ = print_int(res)
val _ = print_string("\n")
/* 12 */
