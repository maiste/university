type int = { 0, 1, 2 }
var int r1 = 0
var int r2 = 1

proc f(){
     while(r1==r2){
        r1 = 0
     }
}

run f()