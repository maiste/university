def digit (n) =
  if n == 0 then "0"
  else if n == 1 then "1"
  else if n == 2 then "2"
  else if n == 3 then "3"
  else if n == 4 then "4"
  else if n == 5 then "5"
  else if n == 6 then "6"
  else if n == 7 then "7"
  else if n == 8 then "8"
  else "9"

def nat_to_string (n) =
  if n < 10 then digit(n)
  else nat_to_string(n/10) ^ digit(n%10)

def int_to_string (n) =
  if n < 0 then "-" ^ nat_to_string(0 - n)
  else nat_to_string(n)

val _ = print_string (int_to_string(123456))
val _ = print_string("\n")