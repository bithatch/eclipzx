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

    DEFP external_thing

    ; ── IN / OUT instructions ──
    IN A, ($FE)         ; DB FE
    IN B, (C)           ; ED 40
    IN A, (C)           ; ED 78
    OUT ($FE), A        ; D3 FE
    OUT (C), A          ; ED 79
    OUT (C), B          ; ED 41

    ; ── IX/IY indexed addressing ──
    LD IX, $1234        ; DD 21 34 12
    LD IY, $5678        ; FD 21 78 56
    LD A, (IX+5)        ; DD 7E 05
    LD B, (IY+0)        ; FD 46 00
    LD (IX+3), C        ; DD 71 03
    LD (IY+10), $42     ; FD 36 0A 42
    ADD A, (IX+1)       ; DD 86 01
    SUB (IY+2)          ; FD 96 02
    AND (IX+0)          ; DD A6 00
    CP (IY+4)           ; FD BE 04
    INC (IX+7)          ; DD 34 07
    DEC (IY+3)          ; FD 35 03
    INC IX              ; DD 23
    DEC IY              ; FD 2B
    PUSH IX             ; DD E5
    POP IY              ; FD E1
    ADD IX, BC          ; DD 09
    ADD IY, DE          ; FD 19
    LD SP, IX           ; DD F9
    RL (IX+2)           ; DD CB 02 16
    BIT 3, (IY+1)       ; FD CB 01 5E
    SET 0, (IX+0)       ; DD CB 00 C6
    RES 7, (IY+5)       ; FD CB 05 BE
    LD (IX+0), A        ; DD 77 00
    LD ($9000), IX      ; DD 22 00 90
    LD IX, ($9000)      ; DD 2A 00 90
    LD I, A             ; ED 47
    LD A, I             ; ED 57
    LD R, A             ; ED 4F
    LD A, R             ; ED 5F
