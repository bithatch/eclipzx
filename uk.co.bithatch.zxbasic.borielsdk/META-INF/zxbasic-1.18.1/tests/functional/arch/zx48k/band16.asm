	org 32768
.core.__START_PROGRAM:
	di
	push ix
	push iy
	exx
	push hl
	exx
	ld (.core.__CALL_BACK__), sp
	ei
	jp .core.__MAIN_PROGRAM__
.core.__CALL_BACK__:
	DEFW 0
.core.ZXBASIC_USER_DATA:
	; Defines USER DATA Length in bytes
.core.ZXBASIC_USER_DATA_LEN EQU .core.ZXBASIC_USER_DATA_END - .core.ZXBASIC_USER_DATA
	.core.__LABEL__.ZXBASIC_USER_DATA_LEN EQU .core.ZXBASIC_USER_DATA_LEN
	.core.__LABEL__.ZXBASIC_USER_DATA EQU .core.ZXBASIC_USER_DATA
_a:
	DEFB 00, 00
_b:
	DEFB 00
.core.ZXBASIC_USER_DATA_END:
.core.__MAIN_PROGRAM__:
	ld hl, (_a)
	ld hl, 0
	ld a, l
	ld (_b), a
	ld de, 1
	ld hl, (_a)
	call .core.__BAND16
	ld a, l
	ld (_b), a
	pop hl
	ld a, l
	ld (_b), a
	ld hl, (_a)
	ld hl, 0
	ld a, l
	ld (_b), a
	ld de, 1
	ld hl, (_a)
	call .core.__BAND16
	ld a, l
	ld (_b), a
	pop hl
	ld a, l
	ld (_b), a
	ld de, (_a)
	ld hl, (_a)
	call .core.__BAND16
	ld a, l
	ld (_b), a
	ld hl, 0
	ld b, h
	ld c, l
.core.__END_PROGRAM:
	di
	ld hl, (.core.__CALL_BACK__)
	ld sp, hl
	exx
	pop hl
	exx
	pop iy
	pop ix
	ei
	ret
	;; --- end of user code ---
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/bitwise/band16.asm"
; vim:ts=4:et:
	; FASTCALL bitwise and16 version.
	; result in hl
; __FASTCALL__ version (operands: A, H)
	; Performs 16bit or 16bit and returns the boolean
; Input: HL, DE
; Output: HL <- HL AND DE
	    push namespace core
__BAND16:
	    ld a, h
	    and d
	    ld h, a
	    ld a, l
	    and e
	    ld l, a
	    ret
	    pop namespace
#line 46 "arch/zx48k/band16.bas"
	END
