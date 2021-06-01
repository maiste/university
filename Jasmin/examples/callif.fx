
/* Un appel de fonction sur autre chose qu'un Fun ou un Var */

def double (x) = x*2
def plusdeux (x) = x+2
def choice(b,x) = ?(if b then &double else &plusdeux)(x)

val tt = (0==0)
val ff = (0==1)
val _ = print_int(choice(tt,4)); print_int(choice(ff,4)); print_string("\n")
/* resultat : 86 */
