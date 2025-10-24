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
	call .core.COPY_ATTR
	ld a, 2
	call .core.INK_TMP
	ld hl, 255
	push hl
	ld hl, 175
	call .core.DRAW
	call .core.COPY_ATTR
	ld a, 7
	call .core.PAPER_TMP
	ld a, 127
	push af
	ld a, 128
	call .core.PLOT
	call .core.COPY_ATTR
	ld a, 1
	call .core.FLASH_TMP
	ld a, 1
	call .core.OVER_TMP
	ld a, 127
	push af
	ld a, 87
	push af
	ld a, 60
	call .core.CIRCLE
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
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/circle.asm"
	; Bresenham's like circle algorithm
	; best known as Middle Point Circle drawing algorithm
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/error.asm"
	; Simple error control routines
; vim:ts=4:et:
	    push namespace core
	ERR_NR    EQU    23610    ; Error code system variable
	; Error code definitions (as in ZX spectrum manual)
; Set error code with:
	;    ld a, ERROR_CODE
	;    ld (ERR_NR), a
	ERROR_Ok                EQU    -1
	ERROR_SubscriptWrong    EQU     2
	ERROR_OutOfMemory       EQU     3
	ERROR_OutOfScreen       EQU     4
	ERROR_NumberTooBig      EQU     5
	ERROR_InvalidArg        EQU     9
	ERROR_IntOutOfRange     EQU    10
	ERROR_NonsenseInBasic   EQU    11
	ERROR_InvalidFileName   EQU    14
	ERROR_InvalidColour     EQU    19
	ERROR_BreakIntoProgram  EQU    20
	ERROR_TapeLoadingErr    EQU    26
	; Raises error using RST #8
__ERROR:
	    ld (__ERROR_CODE), a
	    rst 8
__ERROR_CODE:
	    nop
	    ret
	; Sets the error system variable, but keeps running.
	; Usually this instruction if followed by the END intermediate instruction.
__STOP:
	    ld (ERR_NR), a
	    ret
	    pop namespace
#line 5 "/zxbasic/src/lib/arch/zx48k/runtime/circle.asm"
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/plot.asm"
	; MIXED __FASTCAL__ / __CALLE__ PLOT Function
	; Plots a point into the screen calling the ZX ROM PLOT routine
	; Y in A (accumulator)
	; X in top of the stack
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/in_screen.asm"
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/sposn.asm"
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/sysvars.asm"
	;; -----------------------------------------------------------------------
	;; ZX Basic System Vars
	;; Some of them will be mapped over Sinclair ROM ones for compatibility
	;; -----------------------------------------------------------------------
	push namespace core
SCREEN_ADDR:        DW 16384  ; Screen address (can be pointed to other place to use a screen buffer)
SCREEN_ATTR_ADDR:   DW 22528  ; Screen attribute address (ditto.)
	; These are mapped onto ZX Spectrum ROM VARS
	CHARS               EQU 23606  ; Pointer to ROM/RAM Charset
	TV_FLAG             EQU 23612  ; Flags for controlling output to screen
	UDG                 EQU 23675  ; Pointer to UDG Charset
	COORDS              EQU 23677  ; Last PLOT coordinates
	FLAGS2              EQU 23681  ;
	ECHO_E              EQU 23682  ;
	DFCC                EQU 23684  ; Next screen addr for PRINT
	DFCCL               EQU 23686  ; Next screen attr for PRINT
	S_POSN              EQU 23688
	ATTR_P              EQU 23693  ; Current Permanent ATTRS set with INK, PAPER, etc commands
	ATTR_T              EQU 23695  ; temporary ATTRIBUTES
	P_FLAG              EQU 23697  ;
	MEM0                EQU 23698  ; Temporary memory buffer used by ROM chars
	SCR_COLS            EQU 33     ; Screen with in columns + 1
	SCR_ROWS            EQU 24     ; Screen height in rows
	SCR_SIZE            EQU (SCR_ROWS << 8) + SCR_COLS
	pop namespace
#line 2 "/zxbasic/src/lib/arch/zx48k/runtime/sposn.asm"
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/attr.asm"
	; Attribute routines
; vim:ts=4:et:sw:
	    push namespace core
__ATTR_ADDR:
	    ; calc start address in DE (as (32 * d) + e)
    ; Contributed by Santiago Romero at http://www.speccy.org
	    ld h, 0                     ;  7 T-States
	    ld a, d                     ;  4 T-States
	    ld d, h
	    add a, a     ; a * 2        ;  4 T-States
	    add a, a     ; a * 4        ;  4 T-States
	    ld l, a      ; HL = A * 4   ;  4 T-States
	    add hl, hl   ; HL = A * 8   ; 15 T-States
	    add hl, hl   ; HL = A * 16  ; 15 T-States
	    add hl, hl   ; HL = A * 32  ; 15 T-States
	    add hl, de
	    ld de, (SCREEN_ATTR_ADDR)    ; Adds the screen address
	    add hl, de
	    ; Return current screen address in HL
	    ret
	; Sets the attribute at a given screen coordinate (D, E).
	; The attribute is taken from the ATTR_T memory variable
	; Used by PRINT routines
SET_ATTR:
	    ; Checks for valid coords
	    call __IN_SCREEN
	    ret nc
	    call __ATTR_ADDR
__SET_ATTR:
	    ; Internal __FASTCALL__ Entry used by printing routines
	    ; HL contains the address of the ATTR cell to set
	    PROC
__SET_ATTR2:  ; Sets attr from ATTR_T to (HL) which points to the scr address
	    ld de, (ATTR_T)    ; E = ATTR_T, D = MASK_T
	    ld a, d
	    and (hl)
	    ld c, a    ; C = current screen color, masked
	    ld a, d
	    cpl        ; Negate mask
	    and e    ; Mask current attributes
	    or c    ; Mix them
	    ld (hl), a ; Store result in screen
	    ret
	    ENDP
	    pop namespace
#line 3 "/zxbasic/src/lib/arch/zx48k/runtime/sposn.asm"
	; Printing positioning library.
	    push namespace core
	; Loads into DE current ROW, COL print position from S_POSN mem var.
__LOAD_S_POSN:
	    PROC
	    ld de, (S_POSN)
	    ld hl, SCR_SIZE
	    or a
	    sbc hl, de
	    ex de, hl
	    ret
	    ENDP
	; Saves ROW, COL from DE into S_POSN mem var.
__SAVE_S_POSN:
	    PROC
	    ld hl, SCR_SIZE
	    or a
	    sbc hl, de
	    ld (S_POSN), hl ; saves it again
__SET_SCR_PTR:  ;; Fast
	    push de
	    call __ATTR_ADDR
	    ld (DFCCL), hl
	    pop de
	    ld a, d
	    ld c, a     ; Saves it for later
	    and 0F8h    ; Masks 3 lower bit ; zy
	    ld d, a
	    ld a, c     ; Recovers it
	    and 07h     ; MOD 7 ; y1
	    rrca
	    rrca
	    rrca
	    or e
	    ld e, a
	    ld hl, (SCREEN_ADDR)
	    add hl, de    ; HL = Screen address + DE
	    ld (DFCC), hl
	    ret
	    ENDP
	    pop namespace
#line 2 "/zxbasic/src/lib/arch/zx48k/runtime/in_screen.asm"
	    push namespace core
__IN_SCREEN:
	    ; Returns NO carry if current coords (D, E)
	    ; are OUT of the screen limits
	    PROC
	    LOCAL __IN_SCREEN_ERR
	    ld hl, SCR_SIZE
	    ld a, e
	    cp l
	    jr nc, __IN_SCREEN_ERR	; Do nothing and return if out of range
	    ld a, d
	    cp h
	    ret c                       ; Return if carry (OK)
__IN_SCREEN_ERR:
__OUT_OF_SCREEN_ERR:
	    ; Jumps here if out of screen
	    ld a, ERROR_OutOfScreen
	    jp __STOP   ; Saves error code and exits
	    ENDP
	    pop namespace
#line 9 "/zxbasic/src/lib/arch/zx48k/runtime/plot.asm"
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/set_pixel_addr_attr.asm"
	push namespace core
	; Sets the attribute at a given screen pixel address in hl
	; HL contains the address in RAM for a given pixel (not a coordinate)
SET_PIXEL_ADDR_ATTR:
	    ;; gets ATTR position with offset given in SCREEN_ADDR
	    ld de, (SCREEN_ADDR)
	    or a
	    sbc hl, de
	    ld a, h
	    rrca
	    rrca
	    rrca
	    and 3
	    ld h, a
	    ld de, (SCREEN_ATTR_ADDR)
	    add hl, de  ;; Final screen addr
	    jp __SET_ATTR2
	pop namespace
#line 11 "/zxbasic/src/lib/arch/zx48k/runtime/plot.asm"
	    push namespace core
PLOT:
	    PROC
	    LOCAL PLOT_SUB
	    LOCAL PIXEL_ADDR
	    LOCAL COORDS
	    LOCAL __PLOT_ERR
	    LOCAL P_FLAG
	    LOCAL __PLOT_OVER1
	P_FLAG EQU 23697
	    pop hl
	    ex (sp), hl ; Callee
	    ld b, a
	    ld c, h
#line 37 "/zxbasic/src/lib/arch/zx48k/runtime/plot.asm"
#line 43 "/zxbasic/src/lib/arch/zx48k/runtime/plot.asm"
	    ld a, 191
	    cp b
	    jr c, __PLOT_ERR ; jr is faster here (#1)
__PLOT:			; __FASTCALL__ entry (b, c) = pixel coords (y, x)
	    ld (COORDS), bc	; Saves current point
	    ld a, 191 ; Max y coord
	    call PIXEL_ADDR
	    res 6, h    ; Starts from 0
	    ld bc, (SCREEN_ADDR)
	    add hl, bc  ; Now current offset
	    ld b, a
	    inc b
	    ld a, 0FEh
	LOCAL __PLOT_LOOP
__PLOT_LOOP:
	    rrca
	    djnz __PLOT_LOOP
	    ld b, a
	    ld a, (P_FLAG)
	    ld c, a
	    ld a, (hl)
	    bit 0, c        ; is it OVER 1
	    jr nz, __PLOT_OVER1
	    and b
__PLOT_OVER1:
	    bit 2, c        ; is it inverse 1
	    jr nz, __PLOT_END
	    xor b
	    cpl
	LOCAL __PLOT_END
__PLOT_END:
	    ld (hl), a
	    jp SET_PIXEL_ADDR_ATTR
__PLOT_ERR:
	    jp __OUT_OF_SCREEN_ERR ; Spent 3 bytes, but saves 3 T-States at (#1)
	PLOT_SUB EQU 22ECh
	PIXEL_ADDR EQU 22ACh
	COORDS EQU 5C7Dh
	    ENDP
	    pop namespace
#line 6 "/zxbasic/src/lib/arch/zx48k/runtime/circle.asm"
	; Draws a circle at X, Y of radius R
	; X, Y on the Stack, R in accumulator (Byte)
	    push namespace core
	    PROC
	    LOCAL __CIRCLE_ERROR
	    LOCAL __CIRCLE_LOOP
	    LOCAL __CIRCLE_NEXT
__CIRCLE_ERROR:
	    jp __OUT_OF_SCREEN_ERR
CIRCLE:
	    ;; Entry point
	    pop hl    ; Return Address
	    pop de    ; D = Y
	    ex (sp), hl ; __CALLEE__ convention
	    ld e, h ; E = X
	    ld h, a ; H = R
#line 33 "/zxbasic/src/lib/arch/zx48k/runtime/circle.asm"
#line 39 "/zxbasic/src/lib/arch/zx48k/runtime/circle.asm"
	    ld a, h
	    add a, d
	    sub 192
	    jr nc, __CIRCLE_ERROR
	    ld a, d
	    sub h
	    jr c, __CIRCLE_ERROR
	    ld a, e
	    sub h
	    jr c, __CIRCLE_ERROR
	    ld a, h
	    add a, e
	    jr c, __CIRCLE_ERROR
; __FASTCALL__ Entry: D, E = Y, X point of the center
	; A = Radious
__CIRCLE:
	    push de
	    ld a, h
	    exx
	    pop de        ; D'E' = x0, y0
	    ld h, a       ; H' = r
	    ld c, e
	    ld a, h
	    add a, d
	    ld b, a
	    call __CIRCLE_PLOT    ; PLOT (x0, y0 + r)
	    ld b, d
	    ld a, h
	    add a, e
	    ld c, a
	    call __CIRCLE_PLOT    ; PLOT (x0 + r, y0)
	    ld c, e
	    ld a, d
	    sub h
	    ld b, a
	    call __CIRCLE_PLOT ; PLOT (x0, y0 - r)
	    ld b, d
	    ld a, e
	    sub h
	    ld c, a
	    call __CIRCLE_PLOT ; PLOT (x0 - r, y0)
	    exx
	    ld b, 0        ; B = x = 0
	    ld c, h        ; C = y = Radius
	    ld hl, 1
	    or a
	    sbc hl, bc    ; HL = f = 1 - radius
	    ex de, hl
	    ld hl, 0
	    or a
	    sbc hl, bc  ; HL = -radius
	    add hl, hl    ; HL = -2 * radius
	    ex de, hl    ; DE = -2 * radius = ddF_y, HL = f
	    xor a        ; A = ddF_x = 0
	    ex af, af'    ; Saves it
__CIRCLE_LOOP:
	    ld a, b
	    inc a
	    cp c
	    ret nc        ; Returns when x >= y
    bit 7, h    ; HL >= 0? : if (f >= 0)...
	    jp nz, __CIRCLE_NEXT
	    dec c        ; y--
	    inc de
	    inc de        ; ddF_y += 2
	    add hl, de    ; f += ddF_y
__CIRCLE_NEXT:
	    inc b        ; x++
	    ex af, af'
	    add a, 2    ; 1 Cycle faster than inc a, inc a
	    inc hl        ; f++
	    push af
	    add a, l
	    ld l, a
	    ld a, h
	    adc a, 0    ; f = f + ddF_x
	    ld h, a
	    pop af
	    ex af, af'
	    push bc
	    exx
	    pop hl        ; H'L' = Y, X
	    ld a, d
	    add a, h
	    ld b, a        ; B = y0 + y
	    ld a, e
	    add a, l
	    ld c, a        ; C = x0 + x
	    call __CIRCLE_PLOT ; plot(x0 + x, y0 + y)
	    ld a, d
	    add a, h
	    ld b, a        ; B = y0 + y
	    ld a, e
	    sub l
	    ld c, a        ; C = x0 - x
	    call __CIRCLE_PLOT ; plot(x0 - x, y0 + y)
	    ld a, d
	    sub h
	    ld b, a        ; B = y0 - y
	    ld a, e
	    add a, l
	    ld c, a        ; C = x0 + x
	    call __CIRCLE_PLOT ; plot(x0 + x, y0 - y)
	    ld a, d
	    sub h
	    ld b, a        ; B = y0 - y
	    ld a, e
	    sub l
	    ld c, a        ; C = x0 - x
	    call __CIRCLE_PLOT ; plot(x0 - x, y0 - y)
	    ld a, l
	    cp h
	    jr z, 1f
	    ld a, d
	    add a, l
	    ld b, a        ; B = y0 + x
	    ld a, e
	    add a, h
	    ld c, a        ; C = x0 + y
	    call __CIRCLE_PLOT ; plot(x0 + y, y0 + x)
	    ld a, d
	    add a, l
	    ld b, a        ; B = y0 + x
	    ld a, e
	    sub h
	    ld c, a        ; C = x0 - y
	    call __CIRCLE_PLOT ; plot(x0 - y, y0 + x)
	    ld a, d
	    sub l
	    ld b, a        ; B = y0 - x
	    ld a, e
	    add a, h
	    ld c, a        ; C = x0 + y
	    call __CIRCLE_PLOT ; plot(x0 + y, y0 - x)
	    ld a, d
	    sub l
	    ld b, a        ; B = y0 - x
	    ld a, e
	    sub h
	    ld c, a        ; C = x0 + y
	    call __CIRCLE_PLOT ; plot(x0 - y, y0 - x)
1:
	    exx
	    jp __CIRCLE_LOOP
__CIRCLE_PLOT:
	    ; Plots a point of the circle, preserving HL and DE
	    push hl
	    push de
	    call __PLOT
	    pop de
	    pop hl
	    ret
	    ENDP
	    pop namespace
#line 42 "arch/zx48k/inktemp.bas"
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/copy_attr.asm"
#line 4 "/zxbasic/src/lib/arch/zx48k/runtime/copy_attr.asm"
	    push namespace core
COPY_ATTR:
	    ; Just copies current permanent attribs into temporal attribs
	    ; and sets print mode
	    PROC
	    LOCAL INVERSE1
	    LOCAL __REFRESH_TMP
	INVERSE1 EQU 02Fh
	    ld hl, (ATTR_P)
	    ld (ATTR_T), hl
	    ld hl, FLAGS2
	    call __REFRESH_TMP
	    ld hl, P_FLAG
	    call __REFRESH_TMP
__SET_ATTR_MODE:		; Another entry to set print modes. A contains (P_FLAG)
#line 65 "/zxbasic/src/lib/arch/zx48k/runtime/copy_attr.asm"
	    ret
#line 67 "/zxbasic/src/lib/arch/zx48k/runtime/copy_attr.asm"
__REFRESH_TMP:
	    ld a, (hl)
	    and 0b10101010
	    ld c, a
	    rra
	    or c
	    ld (hl), a
	    ret
	    ENDP
	    pop namespace
#line 43 "arch/zx48k/inktemp.bas"
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/draw.asm"
	; DRAW using bresenhams algorithm and screen positioning
; Copyleft (k) 2010 by J. Rodriguez (a.k.a. Boriel) http://www.boriel.com
; vim:ts=4:et:sw=4:
	; Y parameter in A
	; X parameter in high byte on top of the stack
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/SP/PixelDown.asm"
	;
	; PixelDown
	; Alvin Albrecht 2002
	;
	; Pixel Down
	;
	; Adjusts screen address HL to move one pixel down in the display.
	; (0,0) is located at the top left corner of the screen.
	;
; enter: HL = valid screen address
; exit : Carry = moved off screen
	;        Carry'= moved off current cell (needs ATTR update)
	;        HL = moves one pixel down
; used : AF, HL
	    push namespace core
SP.PixelDown:
	    PROC
	    LOCAL leave
	    push de
	    ld de, (SCREEN_ADDR)
	    or a
	    sbc hl, de
	    inc h
	    ld a,h
	    and $07
	    jr nz, leave
	    scf         ;  Sets carry on F', which flags ATTR must be updated
	    ex af, af'
	    ld a,h
	    sub $08
	    ld h,a
	    ld a,l
	    add a,$20
	    ld l,a
	    jr nc, leave
	    ld a,h
	    add a,$08
	    ld h,a
	    cp $19     ; carry = 0 => Out of screen
	    jr c, leave ; returns if out of screen
	    ccf
	    pop de
	    ret
leave:
	    add hl, de ; This always sets Carry = 0
	    pop de
	    ret
	    ENDP
	    pop namespace
#line 15 "/zxbasic/src/lib/arch/zx48k/runtime/draw.asm"
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/SP/PixelUp.asm"
	;
	; PixelUp
	; Alvin Albrecht 2002
	;
	; Pixel Up
	;
	; Adjusts screen address HL to move one pixel up in the display.
	; (0,0) is located at the top left corner of the screen.
	;
; enter: HL = valid screen address
; exit : Carry = moved off screen
	;        HL = moves one pixel up
; used : AF, HL
	    push namespace core
SP.PixelUp:
	    PROC
	    LOCAL leave
	    push de
	    ld de, (SCREEN_ADDR)
	    or a
	    sbc hl, de
	    ld a,h
	    dec h
	    and $07
	    jr nz, leave
	    scf         ; sets C' to 1 (ATTR update needed)
	    ex af, af'
	    ld a,$08
	    add a,h
	    ld h,a
	    ld a,l
	    sub $20
	    ld l,a
	    jr nc, leave
	    ld a,h
	    sub $08
	    ld h,a
leave:
	    push af
	    add hl, de
	    pop af
	    pop de
	    ret
	    ENDP
	    pop namespace
#line 16 "/zxbasic/src/lib/arch/zx48k/runtime/draw.asm"
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/SP/PixelLeft.asm"
	;
	; PixelLeft
	; Jose Rodriguez 2012
	;
	; PixelLeft
	;
	; Adjusts screen address HL and Pixel bit A to move one pixel to the left
	; on the display.  Start of line set Carry (Out of Screen)
	;
; enter: HL = valid screen address
	;        A = Bit Set
; exit : Carry = moved off screen
	;        Carry' Set if moved off current ATTR CELL
	;        HL = moves one character left, if needed
	;        A = Bit Set with new pixel pos.
; used : AF, HL
	    push namespace core
SP.PixelLeft:
	    PROC
	    LOCAL leave
	    push de
	    ld de, (SCREEN_ADDR)
	    or a
	    sbc hl, de  ; This always sets Carry = 0
	    rlca    ; Sets new pixel bit 1 to the right
	    jr nc, leave
	    ex af, af' ; Signal in C' we've moved off current ATTR cell
	    ld a,l
	    dec a
	    ld l,a
	    cp 32      ; Carry if in screen
	    ccf
	    ld a, 1
leave:  ; Sets screen offset back again
	    push af
	    add hl, de
	    pop af
	    pop de
	    ret
	    ENDP
	    pop namespace
#line 17 "/zxbasic/src/lib/arch/zx48k/runtime/draw.asm"
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/SP/PixelRight.asm"
	;
	; PixelRight
	; Jose Rodriguez 2012
	;
	; PixelRight
	;
	; Adjusts screen address HL and Pixel bit A to move one pixel to the left
	; on the display.  Start of line set Carry (Out of Screen)
	;
; enter: HL = valid screen address
	;        A = Bit Set
; exit : Carry = moved off screen
	;        Carry' Set if moved off current ATTR CELL
	;        HL = moves one character left, if needed
	;        A = Bit Set with new pixel pos.
; used : AF, HL
	    push namespace core
SP.PixelRight:
	    PROC
	    LOCAL leave
	    push de
	    ld de, (SCREEN_ADDR)
	    or a
	    sbc hl, de  ; This always sets Carry = 0
	    rrca    ; Sets new pixel bit 1 to the right
	    jr nc, leave
	    ex af, af' ; Signal in C' we've moved off current ATTR cell
	    ld a, l
	    inc a
	    ld l, a
	    cp 32      ; Carry if IN screen
	    ccf
	    ld a, 80h
leave:  ; Sets screen offset back again
	    push af
	    add hl, de
	    pop af
	    pop de
	    ret
	    ENDP
	    pop namespace
#line 18 "/zxbasic/src/lib/arch/zx48k/runtime/draw.asm"
	;; DRAW PROCEDURE
	    push namespace core
	    PROC
	    LOCAL __DRAW1
	    LOCAL __DRAW2
	    LOCAL __DRAW3
	    LOCAL __DRAW4, __DRAW4_LOOP
	    LOCAL __DRAW5
	    LOCAL __DRAW6, __DRAW6_LOOP
	    LOCAL __DRAW_ERROR
	    LOCAL DX1, DX2, DY1, DY2
	    LOCAL __INCX, __INCY, __DECX, __DECY
	    LOCAL P_FLAG
	P_FLAG EQU 23697
__DRAW_ERROR:
	    jp __OUT_OF_SCREEN_ERR
DRAW:
	    ;; ENTRY POINT
	    LOCAL COORDS
	    LOCAL __DRAW_SETUP1, __DRAW_START, __PLOTOVER, __PLOTINVERSE
	    ex de, hl ; DE = Y OFFSET
	    pop hl	; return addr
	    ex (sp), hl ; CALLEE => HL = X OFFSET
	    ld bc, (COORDS)
	    ld a, c
	    add a, l
	    ld l, a
	    ld a, h
	    adc a, 0 ; HL = HL + C
	    ld h, a
	    jr nz, __DRAW_ERROR	; if a <> 0 => Out of Screen
	    ld a, b
	    add a, e
	    ld e, a
	    ld a, d
	    adc a, 0 ; DE = DE + B
	    ld d, a
	    jr nz, __DRAW_ERROR	; if a <> 0 => Out of Screen
	    ld a, 191
	    sub e
	    jr c, __DRAW_ERROR	; Out of screen
	    ld h, e			; now H,L = y2, x2
__DRAW:
	    ; __FASTCALL__ Entry. Plots from (COORDS) to coord H, L
	    push hl
	    ex de, hl		; D,E = y2, x2;
	    ld a, (P_FLAG)
	    ld c, a
	    bit 2, a        ; Test for INVERSE1
	    jr z, __DRAW_SETUP1
	    ld a, 2Fh       ; CPL
	    ld (__PLOTINVERSE), a
	    ld a, 0A6h      ; and (hl)
	    jp __DRAW_START
__DRAW_SETUP1:
	    xor a           ; nop
	    ld (__PLOTINVERSE), a
	    ld a, 0B6h      ; or (hl)
	    bit 0, c        ; Test for OVER
	    jr z, __DRAW_START
	    ld a, 0AEh      ; xor (hl)
__DRAW_START:
	    ld (__PLOTOVER), a ; "Pokes" last operation
	    exx
	    ld bc, (COORDS) ; B'C' = y1, x1
	    ld d, b         ; Saves B' in D'
	    ld a, 191
	    LOCAL __PIXEL_ADDR
	__PIXEL_ADDR EQU 22ACh
	    call __PIXEL_ADDR
	    res 6, h    ; Starts from 0 offset
	    ;; Now gets pixel mask in A register
	    ld b, a
	    inc b
	    xor a
	    scf
	    LOCAL __PIXEL_MASK
__PIXEL_MASK:
	    rra
	    djnz __PIXEL_MASK
	    ld b, d         ; Restores B' from D'
	    ld de, (SCREEN_ADDR)
	    add hl, de
	    pop de			; D'E' = y2, x2
    exx             ; At this point: D'E' = y2,x2 coords
	    ; B'C' = y1, y1  coords
	    ; H'L' = Screen Address of pixel
	    ex af, af'      ; Saves A reg for later
	    ; A' = Pixel mask
	    ld bc, (COORDS) ; B,C = y1, x1
	    ld a, e
	    sub c			; dx = X2 - X1
	    ld c, a			; Saves dx in c
	    ld a, 0Ch       ; INC C opcode
	    ld hl, __INCX   ; xi = 1
	    jr nc, __DRAW1
	    ld a, c
	    neg		 		; dx = X1 - X2
	    ld c, a
	    ld a, 0Dh       ; DEC C opcode
	    ld hl, __DECX   ; xi = -1
__DRAW1:
	    ld (DX1), a
	    ld (DX1 + 2), hl ; Updates DX1 call address
	    ld (DX2), a
	    ld (DX2 + 2), hl ; Updates DX2 call address
	    ld a, d
	    sub b			; dy = Y2 - Y1
	    ld b, a			; Saves dy in b
	    ld a, 4         ; INC B opcode
	    ld hl, __INCY   ; y1 = 1
	    jr nc, __DRAW2
	    ld a, b
	    neg
	    ld b, a         ; dy = Y2 - Y1
	    ld a, 5         ; DEC B opcode
	    ld hl, __DECY   ; y1 = -1
__DRAW2:
	    ld (DY1), a
	    ld (DY1 + 2), hl ; Updates DX1 call address
	    ld (DY2), a
	    ld (DY2 + 2), hl ; Updates DX2 call address
	    ld a, b
	    sub c			; dy - dx
	    jr c, __DRAW_DX_GT_DY	; DX > DY
	    ; At this point DY >= DX
	    ; --------------------------
	    ; HL = error = dY / 2
	    ld h, 0
	    ld l, b
	    srl l
	    ; DE = -dX
	    xor a
	    sub c
	    ld e, a
	    sbc a, a
	    ld d, a
	    ; BC = DY
	    ld c, b
	    ld b, h
	    exx
	    scf             ; Sets Carry to signal update ATTR
	    ex af, af'      ; Brings back pixel mask
	    ld e, a         ; Saves it in free E register
	    jp __DRAW4_LOOP
__DRAW3:			; While c != e => while y != y2
	    exx
	    add hl, de		; error -= dX
	    bit 7, h		;
	    exx				; recover coordinates
	    jr z, __DRAW4	; if error < 0
	    exx
	    add hl, bc		; error += dY
	    exx
	    ld a, e
DX1:                ; x += xi
	    inc c
	    call __INCX     ; This address will be dynamically updated
	    ld e, a
__DRAW4:
DY1:                ; y += yi
	    inc b
	    call __INCY     ; This address will be dynamically updated
	    ld a, e         ; Restores A reg.
	    call __FASTPLOT
__DRAW4_LOOP:
	    ld a, b
	    cp d
	    jp nz, __DRAW3
	    ld (COORDS), bc
	    ret
__DRAW_DX_GT_DY:	; DX > DY
	    ; --------------------------
	    ; HL = error = dX / 2
	    ld h, 0
	    ld l, c
	    srl l			; HL = error = DX / 2
	    ; DE = -dY
	    xor a
	    sub b
	    ld e, a
	    sbc a, a
	    ld d, a
	    ; BC = dX
	    ld b, h
	    exx
	    ld d, e
	    scf             ; Sets Carry to signal update ATTR
	    ex af, af'      ; Brings back pixel mask
	    ld e, a         ; Saves it in free E register
	    jp __DRAW6_LOOP
__DRAW5:			; While loop
	    exx
	    add hl, de		; error -= dY
	    bit 7, h		; if (error < 0)
	    exx				; Restore coords
	    jr z, __DRAW6	;
	    exx
	    add hl, bc		; error += dX
	    exx
DY2:                ; y += yi
	    inc b
	    call __INCY     ; This address will be dynamically updated
__DRAW6:
	    ld a, e
DX2:                ; x += xi
	    inc c
	    call __INCX     ; This address will be dynamically updated
	    ld e, a
	    call __FASTPLOT
__DRAW6_LOOP:
	    ld a, c			; Current X coord
	    cp d
	    jp nz, __DRAW5
	    ld (COORDS), bc
	    ret
	COORDS   EQU 5C7Dh
__DRAW_END:
	    exx
	    ret
	    ;; Given a A mask and an HL screen position
	    ;; return the next left position
	    ;; Also updates BC coords
	__DECX EQU SP.PixelLeft
	    ;; Like the above, but to the RIGHT
	    ;; Also updates BC coords
	__INCX EQU SP.PixelRight
	    ;; Given an HL screen position, calculates
	    ;; the above position
	    ;; Also updates BC coords
	__INCY EQU SP.PixelUp
	    ;; Given an HL screen position, calculates
	    ;; the above position
	    ;; Also updates BC coords
	__DECY EQU SP.PixelDown
	    ;; Puts the A register MASK in (HL)
__FASTPLOT:
__PLOTINVERSE:
	    nop         ; Replace with CPL if INVERSE 1
__PLOTOVER:
	    or (hl)     ; Replace with XOR (hl) if OVER 1 AND INVERSE 0
	    ; Replace with AND (hl) if INVERSE 1
	    ld (hl), a
	    ex af, af'  ; Recovers flag. If Carry set => update ATTR
	    ld a, e     ; Recovers A reg
	    ret nc
	    push hl
	    push de
	    push bc
	    call SET_PIXEL_ADDR_ATTR
	    pop bc
	    pop de
	    pop hl
	    LOCAL __FASTPLOTEND
__FASTPLOTEND:
	    or a        ; Resets carry flag
	    ex af, af'  ; Recovers A reg
	    ld a, e
	    ret
	    ENDP
	    pop namespace
#line 44 "arch/zx48k/inktemp.bas"
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/flash.asm"
	; Sets flash flag in ATTR_P permanently
; Parameter: Paper color in A register
	    push namespace core
FLASH:
	    ld hl, ATTR_P
	    PROC
	    LOCAL IS_TR
	    LOCAL IS_ZERO
__SET_FLASH:
	    ; Another entry. This will set the flash flag at location pointer by DE
	    cp 8
	    jr z, IS_TR
	    ; # Convert to 0/1
	    or a
	    jr z, IS_ZERO
	    ld a, 0x80
IS_ZERO:
	    ld b, a	; Saves the color
	    ld a, (hl)
	    and 07Fh ; Clears previous value
	    or b
	    ld (hl), a
	    inc hl
	    res 7, (hl)  ;Reset bit 7 to disable transparency
	    ret
IS_TR:  ; transparent
	    inc hl ; Points DE to MASK_T or MASK_P
	    set 7, (hl)  ;Set bit 7 to enable transparency
	    ret
	; Sets the FLASH flag passed in A register in the ATTR_T variable
FLASH_TMP:
	    ld hl, ATTR_T
	    jr __SET_FLASH
	    ENDP
	    pop namespace
#line 45 "arch/zx48k/inktemp.bas"
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/ink.asm"
	; Sets ink color in ATTR_P permanently
; Parameter: Paper color in A register
	    push namespace core
INK:
	    PROC
	    LOCAL __SET_INK
	    LOCAL __SET_INK2
	    ld de, ATTR_P
__SET_INK:
	    cp 8
	    jr nz, __SET_INK2
	    inc de ; Points DE to MASK_T or MASK_P
	    ld a, (de)
	    or 7 ; Set bits 0,1,2 to enable transparency
	    ld (de), a
	    ret
__SET_INK2:
	    ; Another entry. This will set the ink color at location pointer by DE
	    and 7	; # Gets color mod 8
	    ld b, a	; Saves the color
	    ld a, (de)
	    and 0F8h ; Clears previous value
	    or b
	    ld (de), a
	    inc de ; Points DE to MASK_T or MASK_P
	    ld a, (de)
	    and 0F8h ; Reset bits 0,1,2 sign to disable transparency
	    ld (de), a ; Store new attr
	    ret
	; Sets the INK color passed in A register in the ATTR_T variable
INK_TMP:
	    ld de, ATTR_T
	    jp __SET_INK
	    ENDP
	    pop namespace
#line 46 "arch/zx48k/inktemp.bas"
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/over.asm"
	; Sets OVER flag in P_FLAG permanently
; Parameter: OVER flag in bit 0 of A register
	    push namespace core
OVER:
	    PROC
	    ld c, a ; saves it for later
	    and 2
	    ld hl, FLAGS2
	    res 1, (HL)
	    or (hl)
	    ld (hl), a
	    ld a, c	; Recovers previous value
	    and 1	; # Convert to 0/1
	    add a, a; # Shift left 1 bit for permanent
	    ld hl, P_FLAG
	    res 1, (hl)
	    or (hl)
	    ld (hl), a
	    ret
	; Sets OVER flag in P_FLAG temporarily
OVER_TMP:
	    ld c, a ; saves it for later
	    and 2	; gets bit 1; clears carry
	    rra
	    ld hl, FLAGS2
	    res 0, (hl)
	    or (hl)
	    ld (hl), a
	    ld a, c	; Recovers previous value
	    and 1
	    ld hl, P_FLAG
	    res 0, (hl)
	    or (hl)
	    ld (hl), a
	    jp __SET_ATTR_MODE
	    ENDP
	    pop namespace
#line 47 "arch/zx48k/inktemp.bas"
#line 1 "/zxbasic/src/lib/arch/zx48k/runtime/paper.asm"
	; Sets paper color in ATTR_P permanently
; Parameter: Paper color in A register
	    push namespace core
PAPER:
	    PROC
	    LOCAL __SET_PAPER
	    LOCAL __SET_PAPER2
	    ld de, ATTR_P
__SET_PAPER:
	    cp 8
	    jr nz, __SET_PAPER2
	    inc de
	    ld a, (de)
	    or 038h
	    ld (de), a
	    ret
	    ; Another entry. This will set the paper color at location pointer by DE
__SET_PAPER2:
	    and 7	; # Remove
	    rlca
	    rlca
	    rlca		; a *= 8
	    ld b, a	; Saves the color
	    ld a, (de)
	    and 0C7h ; Clears previous value
	    or b
	    ld (de), a
	    inc de ; Points to MASK_T or MASK_P accordingly
	    ld a, (de)
	    and 0C7h  ; Resets bits 3,4,5
	    ld (de), a
	    ret
	; Sets the PAPER color passed in A register in the ATTR_T variable
PAPER_TMP:
	    ld de, ATTR_T
	    jp __SET_PAPER
	    ENDP
	    pop namespace
#line 48 "arch/zx48k/inktemp.bas"
	END
