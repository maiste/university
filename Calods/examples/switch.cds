type int = { 0, 1, 2}

proc switch_int(){
     var int x
     x = 0
     switch x{
         (0):
              x = 1
         (1):
              x = 2
         (_):
              x = 0
     }
}

run switch_int()