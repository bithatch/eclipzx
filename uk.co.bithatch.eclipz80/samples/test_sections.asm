; Z80 Assembly - Sections, Modules, and Linker Directives Test
;
; Demonstrates:
;   - MODULE directive (sets module namespace)
;   - SECTION directive (organises code into named sections)
;   - PUBLIC / XDEF (exports symbols for other modules)
;   - GLOBAL (marks symbols as globally visible)
;   - ALIGN directive (pads to alignment boundary)
;   - Forward-reference EQU (uses labels defined later)
;
; Note: EXTERN is supported but not tested here as it requires
; a linker phase (unresolved externs cause a hard error).

    ORG $8000

MODULE main

; ── Symbol visibility ──
PUBLIC start
GLOBAL msg_len

; ── Forward-reference EQU ──
end_addr EQU end_marker

; ── CODE section ──
SECTION CODE

start:
    ld hl, message
    ld b, msg_len
print_loop:
    ld a, (hl)
    or a
    jr z, print_done
    rst $10
    inc hl
    djnz print_loop
print_done:
    call cleanup
    ret

; A local routine to demonstrate CALL within the same module
cleanup:
    xor a
    ret

; ── Alignment test ──
    ALIGN 16            ; pad to next 16-byte boundary with 0x00
    db $AA              ; marker byte after alignment

    ALIGN 8, $FF        ; pad to next 8-byte boundary with 0xFF fill
    db $BB              ; marker byte after alignment

; ── DATA section ──
SECTION DATA

message:
    db "Hello", 0

msg_len EQU 5

end_marker:
    db 0
