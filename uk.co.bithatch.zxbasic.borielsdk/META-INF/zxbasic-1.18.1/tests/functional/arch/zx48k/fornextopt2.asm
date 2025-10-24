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
.core.ZXBASIC_USER_DATA_END:
.core.__MAIN_PROGRAM__:
	ld a, 10
	ld (_a), a
	jp .LABEL.__LABEL0
.LABEL.__LABEL3:
.LABEL._lbl:
	ld hl, _a
	inc (hl)
.LABEL.__LABEL4:
	ld hl, _a
	dec (hl)
.LABEL.__LABEL0:
	ld a, (_a)
	cp 11
	jp nc, .LABEL.__LABEL3
.LABEL.__LABEL2:
	jp .LABEL._lbl
	;; --- end of user code ---
	END
