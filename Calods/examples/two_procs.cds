type int = { 0, 1 }


proc p1(int x){
     if (x != 0) {
        decide x
     }
     decide x
}

proc p2(int x){
     if (x == 0){
        decide x
     }
}

run
forall x,y in int {
       p1(x) || p2(y)
}