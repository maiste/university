type int = { 0, 1, 2 }
var int r1 = 0
var int r2 = 1

proc f(){
     var int tmp
     switch(r1){
        (0):
                tmp = 0
                tmp = 1
        (_):
                tmp = 0
                tmp = 2
     }
}

run f()