def fact(x) = if x == 0 then 1 else x * fact(x-1)
val _ = print_int(fact(10))
val _ = print_string("\n")
/* answer: 3628800 */
