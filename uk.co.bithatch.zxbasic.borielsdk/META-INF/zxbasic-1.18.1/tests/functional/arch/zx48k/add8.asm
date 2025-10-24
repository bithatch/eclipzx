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
	DEFB 00
_b:
	DEFB 00
.core.ZXBASIC_USER_DATA_END:
.core.__MAIN_PROGRAM__:
	ld a, (_a)
	ld (_b), a
	ld a, (_a)
	inc a
	ld (_b), a
	ld a, (_a)
	ld (_b), a
	ld a, (_a)
	inc a
	ld (_b), a
	ld hl, (_a - 1)
	ld a, (_a)
	add a, h
	ld (_b), a
	ld hl, (_a - 1)
	ld a, (_a)
	sub h
	sub 1
	sbc a, a
	neg
	push af
	ld hl, (_a - 1)
	ld a, (_a)
	sub h
	sub 1
	sbc a, a
	neg
	ld h, a
	pop af
	add a, h
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
	END
