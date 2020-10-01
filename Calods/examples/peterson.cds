type bool = { faux, vrai }
type int = { 1, 2 }
var bool D1 = faux
var bool D2 = faux
var int tour = 1

proc p0(){
  D1 = vrai
  tour = 2
  while(D2==vrai and tour==2){
  }
  D1 = faux
}
proc p1(){
  D2 = vrai
  tour = 1
  while(D1==vrai and tour==1){
  }
  D2 = faux
}
run p0() || p1()
