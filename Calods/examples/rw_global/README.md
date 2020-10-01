# CALODS - Concise Algorithm Language for Observable Distributed Systems
## Projet long 2019 - 2020

This folder contains examples on read/write on global variables. It shows how we double prism instructions with a synchronizing label and the 'active' boolean.

To run these examples (from root):
```sh
    $ ./calods prism --double true examples/rw_global/x
```
with x: test name

If you don't want to double these instructions:
```sh
    $ ./calods prism --double false examples/rw_global/x
```
