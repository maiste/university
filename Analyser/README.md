## AST 2021

The project was made by Valentin Chaboche and Étienne Marais

### Compilation

To use this project at its maximum, you need to have the following packages on your computer:
- ocaml >= 4.08.0
- graphviz

There is two things to compile to use the project:
- `./runanalysis`, it's the monotone framework analysis on different examples.
- `./ai`, it's the abstract evalution over signs.

**1. Runanalysis**

- Compilation:
```
$ cd tp/6/TP
$ make
```

- Execute the analysis on examples:
```
$ ./runanalysis
```

- The execution automatically generates flow graph files in a .dot.
To transform this .dot into a readable format you have to use graphviz as follow:
```sh
$ dot -T[png|pdf] <name>.dot > <name>.[png|pdf]

# Pdf
$ evince <name>.pdf

# Png
$ display <name>.png
```

**2. Abstint**

- Compilation:
```
$ cd tp/8/absint
$ make
```

- Execute the analysis on examples:
```
$ ./ai
```

### Examples

- MF:
  - Program 1:
  ```
  x = 5 ;
  y = 1 ;
  while x > 1 do
    y = x * y ;
    x = x - 1 ;
  done
  ```
  - Program 2:
  ```
  x = z + b ;
  y =  z * b ;
  while y > z + b do
    z = z + 1 ;
    x = Z + b ;
  done
  ```
  - Program 3:
  ```
  if a > b then
    x = b - a ;
    y = a - b ;
  else
    y = b - a ;
    x = a - b ;
  ```
  - Program 4:
  ```
  x = 2 ;
  y = 4 ;
  x = 1 ;
  if y > x then
    z = y ;
  else
    z = y * y ;
  x = z;
  ```
   - Program 5:
  ```
  x = -3 ;
  y = -4 ;
  if x > y then
    z = x * y;
  else
    z = x + y;
  ```
  - Program 6:
  ```
  x = 0 ;
  y = 1 ;
  z = -1 ;
  a = x * y ;
  b = x * z ;
  c = y * z ;
  d = x - y ;
  e = y - z ;
  f = x - z ;
  g = x + y ;
  h = y + z ;
  i = x + z ;
  ```


- Abstract Evaluation:
  - Program 1:
  ```
  a = -5
  b = 0
  c = 8
  d = 0
  d = a + b
  d = c + d
  ```
  - Program 2:
  ```
  a = -5
  b = 0
  c = 8
  d = 0
  d = b - c
  d = a - a
  ```
  - Program 3:
  ```
  a = -5
  b = 0
  c = 8
  d = 0
  d = a * b
  d = d * c
  ```
  - Program 4:
  ```
  x := Top
  y := Top

  if (y > 7) then
	  x = y - 7
  else
	  x = 7 - y
  ```
  - Program 5:
  ```
  y := Zero

  y = 1
  while (y > 0)
	  y = y + 1
  ```
  - Program 6:
  ```
  y := Zero

  while (y > 0){}
  ```

### Monotone Frameworks

We have completed theses tasks for the Monotone Framework solver part:
- Reaching definition analysis
- Available expression analysis
- Constant propagation analysis
- Sign analysis
- Very busy expression analysis
- Live variable analysis

File descriptions:
 - [whilennh](./tp/6/TP/whilennh.ml) contains the computation of labels and the interpreter.
 - [auxiliary](./tp/6/TP/auxiliary.ml) contains the flow, final and init functions.
 - [graph](./tp/6/TP/graph.ml) contains the material to be able to compute the dot files.
 - [mf](./tp/6/TP/mf.ml) computes the mf analysis.
 - [mfsolver](./tp/6/TP/mfsolver.ml) produces the result of the mf analysis.
 - [main](./tp/6/TP/main.ml) runs examples and produces dot files.

### Abstract evaluation
We have complete this task for the Abstract Evaluation part:
- Sign Domain

File descriptions:
 - [lattices](./tp/8/absint/lattices.ml) contains the definition of module sign
 - [signdomain](./tp/8/absint/signdomain.ml) contains the abstract domain for sign
 - [main](./tp/8/absint/main.ml) runs examples on sign
