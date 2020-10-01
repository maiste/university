type int = { 0, 1, 2 }
var int r = 0

proc loop(){
     var int x
     x = 1
     while(x != r){
         x = 2
         x = 1
     }
}

run loop()