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
	DEFB 00, 00, 00, 00, 00
.core.ZXBASIC_USER_DATA_END:
.core.__MAIN_PROGRAM__:
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
	push hl
	inc sp
	ld a, 000h
	ld de, 00000h
	ld bc, 00000h
	ld hl, 4
	call .core.__PISTOREF
	push ix
	pop hl
	ld de, -5
	add hl, de
	call .core.__PLOADF
	ld hl, 4
	call .core.__PISTOREF
	ld a, (_y)
	ld de, (_y + 1)
	ld bc, (_y + 3)
	ld hl, 4
	call .core.__PISTOREF
	push ix
	pop hl
	ld de, 4
	add hl, de
	ld h, (ix+1)
	ld l, (ix+0)
	call .core.__ILOADF
	ld hl, _y
	call .core.__STOREF
_test__leave:
	ld sp, ix
	pop ix
	exx
	pop hl
	ex (sp), hl
	exx
	ret
	;; --- end of user code ---
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/iloadf.asm"
	; __FASTCALL__ routine which
	; loads a 40 bits floating point into A ED CB
	; stored at position pointed by POINTER HL
	;A DE, BC <-- ((HL))
	    push namespace core
__ILOADF:
	    ld a, (hl)
	    inc hl
	    ld h, (hl)
	    ld l, a
	; __FASTCALL__ routine which
	; loads a 40 bits floating point into A ED CB
	; stored at position pointed by POINTER HL
	;A DE, BC <-- (HL)
__LOADF:    ; Loads a 40 bits FP number from address pointed by HL
	    ld a, (hl)
	    inc hl
	    ld e, (hl)
	    inc hl
	    ld d, (hl)
	    inc hl
	    ld c, (hl)
	    inc hl
	    ld b, (hl)
	    ret
	    pop namespace
#line 60 "arch/zx48k/byreff.bas"
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/ploadf.asm"
	; Parameter / Local var load
	; A => Offset
	; IX = Stack Frame
; RESULT: HL => IX + DE
	    push namespace core
__PLOADF:
	    push ix
	    pop hl
	    add hl, de
	    jp __LOADF
	    pop namespace
#line 61 "arch/zx48k/byreff.bas"
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/storef.asm"
	    push namespace core
__PISTOREF:	; Indect Stores a float (A, E, D, C, B) at location stored in memory, pointed by (IX + HL)
	    push de
	    ex de, hl	; DE <- HL
	    push ix
	    pop hl		; HL <- IX
	    add hl, de  ; HL <- IX + HL
	    pop de
__ISTOREF:  ; Load address at hl, and stores A,E,D,C,B registers at that address. Modifies A' register
	    ex af, af'
	    ld a, (hl)
	    inc hl
	    ld h, (hl)
	    ld l, a     ; HL = (HL)
	    ex af, af'
__STOREF:	; Stores the given FP number in A EDCB at address HL
	    ld (hl), a
	    inc hl
	    ld (hl), e
	    inc hl
	    ld (hl), d
	    inc hl
	    ld (hl), c
	    inc hl
	    ld (hl), b
	    ret
	    pop namespace
#line 62 "arch/zx48k/byreff.bas"
	END
