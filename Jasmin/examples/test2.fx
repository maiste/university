def f(x) =
  if x+1 > 0 then print_int(1)
  else print_int(0)

def g(x) =
  if 0 < x+1 then print_int(1)
  else print_int(0)

def h(x) =
  if x*1 < x+1 then print_int(1)
  else print_int(0)



val _ = f(0)
val _ = g(0)
