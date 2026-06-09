## Expressions
[[Top](Tool---z80asm)]

*z80asm* accepts expressions in both directives and assembly instructions. The
expressions my be required to be constant, i.e. evaluated at assembly time, 
or non-constant, i.e. evaluated by the linker.

### Operators

The expressions follow closely the C-standard with a few additions.

The operators in descending precedence order are:

| Operators    | Description                                            | Assoc
| ---          | ---                                                    | ---
| + - <br> ! ~ <br> (...) [...] | Unary-plus, unary-minus<br>Logical NOT, binary NOT <br>Parenthesized expression | right
| **           | Power                                                  | right
| * / %        | Multiply, divide, reminder                             | left
| + -          | Add, subtract                                          | left
| << >>        | Binary shift left, shift right                         | left
| = == <br> != <> <br> < <= <br> > >= | Equal<br>Different<br>Less than, less or equal<br>Greater than, greater or equal | left
| &amp;        | Binary AND                                             | left
| &#124; ^     | Binary OR, binary XOR                                  | left
| &amp;&amp;   | Logical AND                                            | left
| &#124;&#124; | Logical OR                                             | left
| ? :          | Ternary conditional                                    | right


### Numbers

The assembler accepts numbers in decimal, hexadecimal and binary. 
Different syntax is allowed to simplify porting of code written for other 
assemblers. Some of the prefix characters are also used as operators; in this 
case a space may be needed after the prefix to define it as an operator:

```
   ld a, %10     ; A = 2 (10 binary)
   ld a, 12 % 10 ; A = 2 (remainder of 12 divided by 10)
```

All expressions are computed as signed integers with the host platform's integer size (32-bit or 64-bit in the most common platforms).

**Decimal** numbers are a sequence of decimal digits (```0..9```). Leading 
zeros are insignificant - note the difference from C, where a leading zero 
means octal.

```
  ld a, 99
```

**Hexadecimal** numbers are a sequence of hexadecimal digits (```0..9``` and 
```A..F```, case-insensitive), either prefixed or suffixed with an hexadecimal 
marker. If the marker is a suffix, and the number starts with a letter, then a 
leading zero has to be added.

```
  ld a, $FF
  ld a, 0xFF
  ld a, 0FFh
```

**Binary** numbers are a sequence of binary digits (```0..1```), either 
prefixed or suffixed with a binary marker. 

```
  ld a, %11
  ld a, @11
  ld a, 0b11
  ld a, 11b
```

Binary numbers can be specified as **bitmaps**, with ```#``` as ```1``` and 
```-``` as ```0```, using the binary prefix (```@``` or ```%```) immediately 
followed by a double-quoted string of hashes and dashes.

```
  defb @"---##---"
  defb @"-##--##-"
  defb %"-##-----"
  defb %"-##-----"
  defb @"-##--##-"
  defb @"---##---"
```

### Character constants

A character enclosed in single-quotes can be used to represent the character code.

```
  defb 'A'
```

### ASMPC

`ASMPC` can be used in an expression to represent the address of the current 
instruction. The value will be resolved during linking.
