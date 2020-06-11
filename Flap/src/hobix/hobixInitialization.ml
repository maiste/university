let initialize () =
  Languages.register (module Hobix);
  Compilers.register (module HopixToHobix);
  Compilers.register (module Compilers.Identity (Hobix))
