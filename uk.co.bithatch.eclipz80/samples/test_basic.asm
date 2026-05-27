; test_basic.asm - Basic Z80 assembler test program
; Tests core instruction encoding without labels or expressions
; Expected to produce ~35 bytes of machine code 

    ORG $8000

    ; Zero-operand instructions
    NOP                 ; 00
    DI                  ; F3
    EI                  ; FB

    ; 8-bit register loads
    LD A, 42            ; 3E 2A
    LD B, 5             ; 06 05
    LD A, B             ; 78

    ; 16-bit register load
    LD HL, $C000        ; 21 00 C0

    ; 8-bit INC/DEC
    INC A               ; 3C
    DEC B               ; 05

    ; 16-bit INC/DEC
    INC HL              ; 23
    DEC DE              ; 1B

    ; Arithmetic
    ADD A, B            ; 80
    ADD A, 3            ; C6 03
    SUB 1               ; D6 01
    ADD HL, BC          ; 09

    ; Logic / Compare
    AND $FF             ; E6 FF
    OR $0F              ; F6 0F
    XOR A               ; AF
    CP 0                ; FE 00

    ; Indirect addressing
    LD A, (HL)          ; 7E
    LD (HL), B          ; 70
    LD (HL), 99         ; 36 63

    ; Stack
    PUSH HL             ; E5
    POP BC              ; C1

    ; Unconditional jump
    JP $8000            ; C3 00 80

    ; Halt and return
    HALT                ; 76
    RET                 ; C9
