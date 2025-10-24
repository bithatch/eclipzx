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
	DEFB 00, 00, 00, 00
_b:
	DEFB 00
.core.ZXBASIC_USER_DATA_END:
.core.__MAIN_PROGRAM__:
	ld a, (_b)
	ld b, a
	ld hl, (_a)
	ld de, (_a + 2)
	or a
	jr z, .LABEL.__LABEL1
.LABEL.__LABEL0:
	call .core.__SHL32
	djnz .LABEL.__LABEL0
.LABEL.__LABEL1:
	ld (_a), hl
	ld (_a + 2), de
	ld hl, (_a)
	ld de, (_a + 2)
	call .core.__SHL32
	ld (_a), hl
	ld (_a + 2), de
	ld hl, (_a)
	ld de, (_a + 2)
	ld (_a), hl
	ld (_a + 2), de
	ld a, (_b)
	xor a
	ld l, a
	ld h, 0
	ld e, h
	ld d, h
	ld (_a), hl
	ld (_a + 2), de
	ld hl, (_a + 2)
	push hl
	ld hl, (_a)
	push hl
	ld hl, (_a)
	ld de, (_a + 2)
	call .core.__EQ32
	sub 1
	sbc a, a
	inc a
	push af
	ld a, (_b)
	pop hl
	or a
	ld b, a
	ld a, h
	jr z, .LABEL.__LABEL3
.LABEL.__LABEL2:
	add a, a
	djnz .LABEL.__LABEL2
.LABEL.__LABEL3:
	ld l, a
	ld h, 0
	ld e, h
	ld d, h
	ld (_a), hl
	ld (_a + 2), de
	ld hl, (_b - 1)
	ld a, (_b)
	sub h
	sub 1
	sbc a, a
	neg
	ld b, a
	ld hl, (_a)
	ld de, (_a + 2)
	or a
	jr z, .LABEL.__LABEL5
.LABEL.__LABEL4:
	call .core.__SHL32
	djnz .LABEL.__LABEL4
.LABEL.__LABEL5:
	ld (_a), hl
	ld (_a + 2), de
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
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/bitwise/shl32.asm"
	    push namespace core
__SHL32: ; Left Logical Shift 32 bits
	    sla l
	    rl h
	    rl e
	    rl d
	    ret
	    pop namespace
#line 90 "arch/zx48k/shlu32.bas"
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/cmp/eq32.asm"
	    push namespace core
__EQ32:	; Test if 32bit value HLDE equals top of the stack
    ; Returns result in A: 0 = False, FF = True
	    exx
	    pop bc ; Return address
	    exx
	    xor a	; Reset carry flag
	    pop bc
	    sbc hl, bc ; Low part
	    ex de, hl
	    pop bc
	    sbc hl, bc ; High part
	    exx
	    push bc ; CALLEE
	    exx
	    ld a, h
	    or l
	    or d
	    or e   ; a = 0 and Z flag set only if HLDE = 0
	    ld a, 1
	    ret z
	    xor a
	    ret
	    pop namespace
#line 91 "arch/zx48k/shlu32.bas"
	END
