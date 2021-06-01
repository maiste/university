def fact(x,r) = if x == 0 then r else fact(x-1,r*x)
val _ = print_int(fact(10,1))
val _ = print_string("\n")
/* answer: 3628800 */
