; test_z80n.asm - Z80N (ZX Spectrum Next) extended instruction test
; Tests all Z80N-specific instruction encodings
; Requires --z80n / --cpu=z80n

    ORG $8000

    ; ── Z80N zero-operand instructions ──
    SWAPNIB             ; ED 23
    MIRROR A            ; ED 24
    MUL D, E            ; ED 30
    PIXELDN             ; ED 93
    PIXELAD             ; ED 94
    SETAE               ; ED 95
    OUTINB              ; ED 90

    ; ── Z80N TEST nn ──
    TEST $55            ; ED 27 55

    ; ── Z80N NEXTREG ──
    NEXTREG $07, $03    ; ED 91 07 03   (NEXTREG reg, val)
    NEXTREG $07, A      ; ED 92 07      (NEXTREG reg, A)

    ; ── Z80N block operations ──
    LDIX                ; ED A4
    LDDX                ; ED AC
    LDIRX               ; ED B4
    LDDRX               ; ED BC
    LDPIRX              ; ED B7
    LDWS                ; ED A5

    ; ── Z80N barrel shifts (DE, B) ──
    BSLA DE, B          ; ED 28
    BSRA DE, B          ; ED 29
    BSRL DE, B          ; ED 2A
    BSRF DE, B          ; ED 2B
    BRLC DE, B          ; ED 2C

    ; ── Z80N PUSH nn (big-endian) ──
    PUSH $1234          ; ED 8A 12 34

    ; ── Z80N BRK (debugger breakpoint) ──
    ; NOTE: z88dk does not recognise BRK
    ; Uncomment to test with our assembler only:
    ; BRK                 ; DD 01

    ; Done
    RET                 ; C9
