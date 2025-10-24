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
	ld a, (_b)
	ld hl, (_a - 1)
	or a
	ld b, a
	ld a, h
	jr z, .LABEL.__LABEL1
.LABEL.__LABEL0:
	sra a
	djnz .LABEL.__LABEL0
.LABEL.__LABEL1:
	ld (_a), a
	sra a
	ld (_a), a
	ld (_a), a
	ld a, (_b)
	xor a
	ld (_a), a
	ld hl, (_a - 1)
	ld a, (_a)
	sub h
	sub 1
	sbc a, a
	neg
	push af
	ld a, (_b)
	pop hl
	or a
	ld b, a
	ld a, h
	jr z, .LABEL.__LABEL3
.LABEL.__LABEL2:
	srl a
	djnz .LABEL.__LABEL2
.LABEL.__LABEL3:
	ld (_a), a
	ld hl, (_a - 1)
	ld a, (_a)
	sub h
	sub 1
	sbc a, a
	neg
	ld hl, (_a - 1)
	or a
	ld b, a
	ld a, h
	jr z, .LABEL.__LABEL5
.LABEL.__LABEL4:
	sra a
	djnz .LABEL.__LABEL4
.LABEL.__LABEL5:
	ld (_a), a
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
