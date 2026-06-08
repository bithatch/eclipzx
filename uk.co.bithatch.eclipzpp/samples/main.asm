; Z80 Assembly - Hello World
; Prints "Hello" to the screen using RST 10h (ZX Spectrum ROM)

#define THE_ORG $8000
#define ROM_ROUTINE $10
#define SOMETHING_ELSE ; Pants

DEFINE ZZ = "NOOOO"

    assert THE_ORG , "NOOOO"
    org THE_ORG

    UNDEFINE ZZ

start:
    ld hl, message
.loop:
    ld a, (hl)
    or a
    ret z
    rst ROM_ROUTINE
    inc hl
    jr loop

message: 
#undef SOMETHING_ELSE
SOMETHING_ELSE
	include "message.inc" 
 
