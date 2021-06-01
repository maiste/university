
// Primitive operations

// Note : for binary arithmetic operations, see BinOp instead
// Same for binary comparisons

package trac

object PrimOp extends Enumeration {
  type T = Value

  // Create a new array, with unspecified initial content.
  // Expects one argument : the integer size of the array to create 
  val New : T = Value("new")

  // Read one cell of an array.
  // Expects two arguments : the array, the integer index where to read
  val Get : T = Value("get")

  // Write one value in an array cell.
  // Expects three arguments : the array, the integer index, the value to write
  val Set : T = Value("set")

  // Create a new array intialized with the given arguments.
  // This is a shortcut for a New followed by several Set
  // The argument list could have an arbitrary length
  val Tuple : T = Value("tuple")

  // Print an integer. Expects one argument : the integer to print
  val Printint : T = Value("print_int")

  // Print a string. Expects one argument : the string to print
  val Printstr : T = Value("print_string")

  // Catenate two strings. Expects two string arguments
  val Cat : T = Value("cat")
}
