def fib(x) = if x <= 1 then 1
else fib(x-1) + fib(x-2) + 0
val _ = print_int(fib(10))
val _ = print_string("\n")
/* resultat 89 */
