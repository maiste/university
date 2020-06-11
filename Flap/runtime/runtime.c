#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>

int equal_string(const char* s1, const char* s2) {
  return (strcmp (s1, s2) == 0 ? 1 : 0);
}

int equal_char(char c1, char c2) {
  return (c1 == c2 ? 1 : 0);
}

void print_string(const char* s) {
  printf("%s", s);
}

void print_int(int64_t n) {
    fprintf(stdout, "%jd", n);
}

void observe_int(int64_t n) {
  print_int(n);
}

intptr_t* allocate_block (int64_t n) {
  return (intptr_t*)malloc (n * sizeof (int64_t));
}

intptr_t read_block (intptr_t* block, int64_t n) {
  return block[n];
}

int64_t write_block (intptr_t* block, int64_t n, intptr_t v) {
  block[n] = v;
  return 0;
}
