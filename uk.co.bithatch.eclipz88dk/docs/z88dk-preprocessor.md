## Preprocessor
[[Top](Tool---z80asm)]

**z80asm** runs a preprocessor on the input text and assembles the result 
output. The preprocessor understands the following statements:

### #define

`#define` works like in the C preprocessor and allows the definition of
assembly macros. The macro definition may include `\` which is translated 
to a newline. The macro definition can be continued on a subsequent line
if the previous line ends with `\`.

The macro name and the formal parameters names follow the rules of a C
identifier. 

Example:

    #define pushall \
        push bc \ \
        push de \ \
        push hl
    pushall                 ; pushes bc, de and hl

Note the double role of the backslash, as a line continuation, and as a line 
break.

The token pasting operator ## can be used to glue together two tokens into one:

    #define cat(a, b) a ## b
    
    cat(aa,bb)              ; expands to aabb

Alternative syntax (Future Feature):

    FOO DEFINE 42
    LD A,FOO ; expands to LD A,42

### #undef

`#undef` works like in the C preprocessor and allows a macro definition
to be deleted, if it exists.

### ASSERT expression, ["message"]

Evaluate the constant expression and abort assembly with an assertion failure error if the expression is false. The optional message is shown 
if the assertion fails.

### BINARY|INCBIN "filename"

Loads a binary file at the current location in the object file.

### DEFINE name[=const-expression]

Defines a symbol as logically true (1) or the value of the constant expression, if supplied.

### DEFL

Define a text macro that can be redefined and that can refer to it's previous 
value. A pure text-substitution is done, i.e. the text of the previous value 
is pasted in place in the new value.

    DEFL var = var+1    ; var is now "+1"
    DEFL var = var+1    ; var is now "+1+1"

Alternative syntax:

    var DEFL var+1

### EXITM

Exits the current macro expansion. It is normally used inside a conditional.

    MACRO m1 arg
    if arg==1
      EXITM
    endif
    ...
    ENDM

### IF expression ... [ELIF expression] ... [ELSE] ... ENDIF

Evaluate the constant *expression* and assembles the lines up to the `ELSE`
clause if it is true (i.e. not zero), or the lines from `ELSE` to `ENDIF`
otherwise. The `ELSE` clause is optional. This structure may be nested.

Optional `ELIF` clauses may be added to check for other conditions if the 
previous conditions failed.

Note: These directives can preceded by a hash to allow for compatibility with the C preprocessor.

### IFDEF name ... [ELIFDEF name] ... [ELSE] ... ENDIF

Check if the give symbol name is defined and assembles the lines up to the `ELSE`
clause if true (i.e. defined), or the lines from `ELSE` to `ENDIF` otherwise. 
The `ELSE` clause is optional. This structure may be nested.

Optional `ELIFDEF`|`ELINDEF` clauses may be added to check for other symbols 
if the previous conditions failed.

Note: These directives can preceded by a hash to allow for compatibility with the C preprocessor.

### IFNDEF name ... [ELIFNDEF name] ... [ELSE] ... ENDIF

Check if the give symbol name is defined and assembles the lines up to the `ELSE`
clause if false (i.e. not defined), or the lines from `ELSE` to `ENDIF` 
otherwise. The `ELSE` clause is optional. This structure may be nested.

Optional `ELIFDEF`|`ELINDEF` clauses may be added to check for other symbols 
if the previous conditions failed.

Note: These directives can preceded by a hash to allow for compatibility with the C preprocessor.

### INCLUDE "filename"

Include the given file in the current assembly position. The file is searched in
the path pointed by the `-IPATH` options. Detects and reports an attempt to
recursively include the same file.

Note: This directives can preceded by a hash to allow for compatibility with the C preprocessor.

### LINE|C_LINE line_num[, "filename"]

Tells the assembler that the next input line is from the given source file and
line number. This information is used for error messages.

`LINE` is used to synchronize the assembly source line number.

`C_LINE` is used by the C-compiler to point the error messages to the original
C source file.

### LOCAL

Used inside macros to declare the following symbols as local to the macro
invocation, i.e. they are replaced by a unique identifier on each invocation.

    MACRO abc
    LOCAL l1,l2
    l1:                 ; replaced by l1__<n> unique identifier
    l2:                 ; replaced by l2__<n> unique identifier
    ENDM

### MACRO

Define a new macro.

    MACRO name [arg1,...]
    text
    ENDM

Creates a macro that is expanded when referred to in the opcode field of an 
instruction. The formal parameters are assigned to the actual arguments before
the expansion.

Alternative syntax:

    name MACRO [arg1,...]
    text
    ENDM

### REPT

Repeat a block of code a predefined number of times. The count expression must
be constant.

    ; output 10 spaces on a ZX Spectrum
    REPT 10
    ld  a, ' '
    rst 10h
    ENDR
    
### REPTC

Repeat a block of code for each character of the given string, identifier
or number

    ; output "hello" on a ZX Spectrum
    REPTC var, "hello world"
    ld  a, var
    rst 10h
    ENDR
    
    ; store the digits of the version
    DEFL version=23
    REPTC var, version
    defb var
    ENDR

### REPTI

Repeat a block of code for each of the given expressions.

    ; push all registers
    REPTI reg, bc, de, hl, af
    push reg
    ENDR

### UNDEFINE name

Removes the definition of a symbol, if it exists.

