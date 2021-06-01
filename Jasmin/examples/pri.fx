val _ = if 1; 2==3 then 3 ; 4 else 5 ; 6

val _ = 1 + 2 + 3

val _ = 1 + 1 * 2 * 3 + 4/5

val t = new[2]
val _ = t[0] := new[2]
val _ = t[1] := new[2]
val _ = t[0][0] := 0
val _ = t[0][1] := 1
val _ = t[1][0] := 2
val _ = t[1][1] := 3

val _ = t[1][1]

val _ = let y = 1;2 in 3;4

val _ = t[1] := 1;2

val _ = t[1] := 1 + 2

val _ = t[0+1] := 1 < 2

val _ = 1 + t[0][1] + 3
