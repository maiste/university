type int = { no, yes }

proc p1(int x){
     decide x
}
proc p2(int y){
     decide y
}

run
forall x in int {
 p1(no) || p2(x)
}
