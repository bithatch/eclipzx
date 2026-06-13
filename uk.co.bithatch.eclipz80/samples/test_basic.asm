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

    ; ── Data directives ──

    ; DEFB / DB — single bytes and expressions
    DEFB 1, 2, $80+1   ; 01 02 81
    DB $FF              ; FF

    ; DEFB / DB — string data (each char emitted as a byte)
    DB "Hello"          ; 48 65 6C 6C 6F

    ; DEFW / DW — 16-bit words, little-endian
    DEFW $1234          ; 34 12
    DW 256*2            ; 00 02

    ; DEFW_BE / DW_BE — 16-bit words, big-endian
    ; NOTE: z88dk z80asm does not recognise DW_BE or DEFW_BE despite wiki docs
    ; Uncomment to test with our assembler only:
    ; DW_BE $AABB         ; AA BB

    ; DEFP — 24-bit pointer, little-endian
    DEFP $123456        ; 56 34 12

    ; DEFQ — 32-bit dword, little-endian
    DEFQ $DEADBEEF      ; EF BE AD DE

    ; DEFS / DS — reserve space (filled bytes)
    DEFS 4, $FF         ; FF FF FF FF
    DEFS 3              ; 00 00 00

    ; DC — terminated string (bit 7 set on last char)
    ; NOTE: z88dk z80asm does not recognise DC despite wiki docs
    ; Uncomment to test with our assembler only:
    ; DC "abc"            ; 61 62 E3

    ; Halt and return
    HALT                ; 76
    RET                 ; C9
    
    ; Some trailing data
    INCBIN "testbin.dat"
    INCLUDE "inc.asm"

    DEFP external_thing ; works
    DEFP core.other_thing ; fails to resolve. but it should
    DEFP third_thing ; works
    DEFP local_thing
    DEFP .inner.local_inner_thing

    push namespace inner
    local_inner_thing:
    DEFB 20
    defp inner.local_inner_thing ; works - should not
    defp local_inner_thing ; does not work - should (relative to current namespace)
    pop namespace

    DEFP .inner.local_inner_thing

local_thing:
    DEFB 10
    
