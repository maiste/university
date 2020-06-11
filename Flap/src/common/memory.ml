type location = int

type 'a block = 'a array

type 'a memory = {
  mutable bound : int;
  data          : 'a block option array;
}

type 'a t = 'a memory

let create size = {
  bound = 0;
  data  = Array.make size None
}

exception OutOfMemory

let allocate mem size init =
  let size = Mint.to_int size in
  if mem.bound >= Array.length mem.data then
    raise OutOfMemory
  else (
    let location = mem.bound in
    mem.data.(location) <- Some (Array.make size init);
    mem.bound <- mem.bound + 1;
    location
  )

exception InvalidDereference of location

let dereference mem location =
  match mem.data.(location) with
    | None -> raise (InvalidDereference location)
    | Some b -> b

let size block =
  Mint.of_int (Array.length block)

let read block i =
  block.(Mint.to_int i)

let write block i x =
  block.(Mint.to_int i) <- x

let array_of_block block =
  block

let print_location x = "#" ^ string_of_int x
