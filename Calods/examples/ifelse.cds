type int = { 0, 1, 2 }
var int r = 0

proc ifelse(){
     if (r == 0){
        r = 1
     }else {
        r = 2
     }
}

run ifelse()