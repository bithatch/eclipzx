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
.core.ZXBASIC_USER_DATA_END:
.core.__MAIN_PROGRAM__:
	call _test
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
_test:
	push ix
	ld ix, 0
	add ix, sp
	ld hl, 0
	push hl
	push hl
	inc sp
	push ix
	pop hl
	ld bc, -3
	add hl, bc
	ex de, hl
	ld hl, .LABEL.__LABEL5
	ld bc, 2
	ldir
	ld (ix-1), 0
	jp .LABEL.__LABEL0
.LABEL.__LABEL3:
	ld l, (ix-3)
	ld h, (ix-2)
	push hl
	ld hl, 65535
	ex de, hl
	pop hl
	ld (hl), e
	inc hl
	ld (hl), d
	ld l, (ix-3)
	ld h, (ix-2)
	inc hl
	ld (ix-3), l
	ld (ix-2), h
.LABEL.__LABEL4:
	inc (ix-1)
.LABEL.__LABEL0:
	ld a, (ix-1)
	push af
	ld a, 250
	pop hl
	cp h
	jp nc, .LABEL.__LABEL3
.LABEL.__LABEL2:
_test__leave:
	ld sp, ix
	pop ix
	ret
	;; --- end of user code ---
.LABEL.__LABEL5:
	DEFB 00h
	DEFB 40h
	END
