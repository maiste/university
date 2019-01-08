#ifndef ARITH_H
#define ARITH_H

enum op_e {op_PLUS,op_MINUS,op_MULT};

struct arith;

struct op {
  enum op_e what;
  struct arith * left;
  struct arith * right;
};

typedef struct op op;

union arith_c{
  int nat;
  char * var;
  op * opa;
};

enum arith_e {arith_NAT, arith_VAR, arith_OP};

struct arith {
  enum arith_e what;
  union arith_c val;
};

typedef struct arith arith;

arith * mk_nat(int n);
arith * mk_var(char * v);
arith * mk_op(enum op_e o,arith * l, arith * r);

int eval(arith * a);
char * eval_to_char(arith * a);

void free_arith(arith * a);
#endif
