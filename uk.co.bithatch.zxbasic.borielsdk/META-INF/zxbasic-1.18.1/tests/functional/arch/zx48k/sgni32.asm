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
_y:
	DEFB 01h
	DEFB 00h
	DEFB 00h
	DEFB 00h
.core.ZXBASIC_USER_DATA_END:
.core.__MAIN_PROGRAM__:
	ld hl, (_y)
	ld de, (_y + 2)
	call .core.__SGNI32
	ld (0), a
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
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/sgni32.asm"
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/sgn.asm"
	; Returns SGN (SIGN) for 32, 16 and 8 bits signed integers, Fixed and FLOAT
	    push namespace core
	    PROC
	    LOCAL __ENDSGN
__SGNF:
	    or b
	    or c
	    or d
	    or e
	    ret z
	    ld a, e
	    jr __ENDSGN
__SGNF16:
__SGNI32:
	    ld a, h
	    or l
	    or e
	    or d
	    ret z
	    ld a, d
	    jr __ENDSGN
__SGNI16:
	    ld a, h
	    or l
	    ret z
	    ld a, h
__ENDSGN:
	    or a
	    ld a, 1
	    ret p
	    neg
	    ret
	    ENDP
	    pop namespace
#line 2 "/zxbasic/src/lib/arch/zx48k/runtime/sgni32.asm"
#line 21 "arch/zx48k/sgni32.bas"
	END
