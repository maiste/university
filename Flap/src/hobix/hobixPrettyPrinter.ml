open PPrint
open PPrintCombinators
open PPrintEngine
open ExtPPrint
open HobixAST
open Position

let int i = string (Int64.to_string i)

let rec program p =
  separate_map hardline (definition) p

and definition = function
  | DefineValue vd ->
     value_definition "val" vd
  | DeclareExtern (x, n) ->
    group (string "extern" ++ identifier x
           ++ string ":" ++ string (string_of_int n))

and value_definition ?(parens=false) what = function
  | SimpleValue (x, e) ->
     let pe =
       if parens then may_paren_expression e else expression e
     in
     nest 2 (group (group (string what ++ identifier x ++ string "=")
                    ++ group pe))
  | RecFunctions rv ->
     group (
         string "fun"
         ++ separate_map
              (hardline ^^ string "and" ^^ break 1)
              function_definition
              rv
       )

and function_definition (f, e) =
  match e with
    | Fun (xs, e) ->
       group (
           identifier f
           ++ group (string "(" ++ separate_map (string "," ^^ break 1) identifier xs ++ string ")")
           ++ string "="
       ) ++ group (expression e)
    | _ ->
       assert false

and identifier (Id x) =
  string x

and expression = function
  | Literal l ->
    literal l

  | While (c, b) ->
    nest 2 (group (string "while" ++ may_paren_expression c
                   ++ string "{" ^^ break 1
                   ++ expression b
                   ++ break 1 ^^ string "}"))

  | Variable x ->
    identifier x

  | Define (vd, e2) ->
    nest 2 (
      group (value_definition ~parens:true "val" vd ^^ string ";"
    ))
    ++ group (expression e2)

  | Fun (p, e) ->
    nest 2 (group (
      group (string "\\" ^^ function_parameters p ++ string "=>") ++
        group (expression e)
    ))

  | Apply (a, bs) ->
    group (
      parens_at_left_of_application a (expression a)
      ++ parens (separate_map (string "," ^^ break 1) expression bs)
    )

  | IfThenElse (c, t, f) ->
    nest 2 (group (
      group (string "if"
             ++ group (may_paren_expression c)
             ++ string "then"
      )
      ++ group (may_paren_expression t)
    ))
    ++ nest 2 (group (
      string "else"
      ++ group (may_paren_expression f)
      ++ string "fi"
    ))

  | WriteBlock (e1, e2, e3) ->
    parens (expression e1) ^^ string "[" ^^ expression e2
    ^^ string "] := " ^^ may_paren_expression e3

  | ReadBlock (e1, e2) ->
    parens (expression e1) ^^ string "[" ^^ expression e2 ^^ string "]"

  | AllocateBlock e1 ->
    expression (Apply (Variable (Id "newblock"), [e1]))

  | Switch (i, bs, default) ->
    group (string "switch" ++ expression i ++ string "in")
    ++ group (
      branches bs
    ) ++ string "or else" ++ begin match default with
      | None -> string "nothing"
      | Some t -> expression t
    end
and branches bs =
  let bs = List.mapi (fun i x -> (i, x)) (Array.to_list bs) in
  separate_map (string "|" ^^ break 1) (fun (i, t) ->
      nest 2 (group (
                  string (string_of_int i)
                  ++ string "=>"
                  ++ match t with
                     | None -> string "!"
                     | Some t -> expression t)
    )) bs

and function_parameters xs =
  parens (separate_map (string "," ^^ break 1) identifier xs)

and may_paren_expression e = match e with
  | Fun _ | Define _ -> parens (expression e)
  | _ -> expression e

and literal = function
  | LInt x ->
    int x
  | LChar c ->
    char c
  | LString s ->
    string_literal s

and char c =
  group (string "'" ^^ string (Char.escaped c) ^^ string "'")

and string_literal s =
  group (string "\"" ^^ string (String.escaped s) ^^ string "\"")

and parens_at_left_of_application e =
  match e with
  | Apply _ | Variable _ | Literal _ -> fun x -> x
  | _ -> parens

and parens_at_right_of_application e =
  match e with
  | Variable _ | Literal _ -> fun x -> x
  | _ -> parens

let to_string f x =
  let b = Buffer.create 13 in
  ToBuffer.pretty 0.8 80 b (f x);
  Buffer.contents b
