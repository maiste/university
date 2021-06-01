/* Fibonacci, in-place iterative version.
   t is an array of length 2 */

def fib_iter(n,t) =
 if n == 0 then t[0]
 else
   let ab = t[0]+t[1] in
   t[0] := t[1];
   t[1] := ab;
   fib_iter(n-1,t)

def fib(n) = fib_iter(n,[1,1])

val _ = print_int(fib(10))
val _ = print_string("\n")
/* resultat 89 */
  