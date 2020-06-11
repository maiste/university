set -e

# Compile examples in x86_64
echo -e "[Build] retrolix -> x86_64"
./flap -t x86-64 src/retrolix/examples/$1.retrolix

# Compile in binary code
echo -e "[Build] x86_64 -> binary $1"
gcc -no-pie -g -o $1 src/retrolix/examples/$1.s src/retrolix/examples/runtime.c

# Delete binary
# echo -e "[Clean] $1.s"
# rm -rf src/retrolix/examples/$1.s
