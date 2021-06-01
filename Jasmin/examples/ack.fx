def ack (n,m) =
 if n == 0 then m+1
 else if m == 0 then ack (n-1,1)
 else ack (n-1, ack(n,m-1))
val _ = print_int(ack (3,3))
val _ = print_string("\n")
/* answer: 61 */
