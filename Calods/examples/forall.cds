type do_we = { no, yes, maybe }

proc args(do_we x, do_we y, do_we z){
     decide x
}

run
forall x,y in do_we {
    args(x, y, maybe)
}