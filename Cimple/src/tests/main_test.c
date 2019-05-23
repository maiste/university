#include "main_test.h"

int main(){
	int rc = 0;
	rc = run_io_test();
	if (rc == 0) rc = run_parser_test();
	if (rc == 0) rc = run_m_tests();
	return rc;
}
