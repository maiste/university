type int = { 0, 1, 2, 3, 4 }


proc one_arg(int x){
     var int tmp
     tmp = x
     decide tmp
}

run one_arg(0)