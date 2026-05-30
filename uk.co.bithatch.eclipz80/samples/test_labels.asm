; Z80 Assembly - Hello World
; Prints "Hello" to the screen using RST 10h (ZX Spectrum ROM)

    org $8000

start:
    ld hl, message
loop:
    ld a, (hl)
    or a
    ret z
    rst $10
    inc hl
    jr loop 

message:
    db "Hello", 0