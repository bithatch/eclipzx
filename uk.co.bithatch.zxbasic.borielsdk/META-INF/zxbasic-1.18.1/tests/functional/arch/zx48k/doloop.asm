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
.LABEL._10:
.LABEL.__LABEL0:
.LABEL._20:
	jp .LABEL.__LABEL0
.LABEL.__LABEL1:
.LABEL.__LABEL2:
	jp .LABEL.__LABEL2
.LABEL.__LABEL3:
.LABEL._30:
.LABEL.__LABEL4:
.LABEL._40:
	ld hl, _a
	inc (hl)
.LABEL._50:
	jp .LABEL.__LABEL4
.LABEL.__LABEL5:
.LABEL.__LABEL6:
	ld hl, _a
	inc (hl)
	jp .LABEL.__LABEL6
.LABEL.__LABEL7:
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
