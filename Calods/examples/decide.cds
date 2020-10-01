type int = { 1, 2 }
var int tmp = 1

proc process_decide(){
     if (tmp == 1){
        decide 1
     }else {
        decide 2
     }
}

run process_decide()