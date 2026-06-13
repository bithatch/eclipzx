
.core.__START_PROGRAM:
    di
    push ix
    push iy
    exx
    push hl
    exx
    ld (.core.__CALL_BACK__), sp
    ei
    call .core.__MEM_INIT
    call .core.__PRINT_INIT
    jp .core.__MAIN_PROGRAM__
.core.__CALL_BACK__:
    DEFW 0
.core.ZXBASIC_USER_DATA:
    ; Defines HEAP SIZE
.core.ZXBASIC_HEAP_SIZE EQU 4768
.core.ZXBASIC_MEM_HEAP:
    DEFS 4768
    ; Defines USER DATA Length in bytes
.core.ZXBASIC_USER_DATA_LEN EQU .core.ZXBASIC_USER_DATA_END - .core.ZXBASIC_USER_DATA
    .core.__LABEL__.ZXBASIC_USER_DATA_LEN EQU .core.ZXBASIC_USER_DATA_LEN
    .core.__LABEL__.ZXBASIC_USER_DATA EQU .core.ZXBASIC_USER_DATA
_z:
    push ix

.core.__MEM_INIT
    db 4

.core.__PRINT_INIT
    db 4

.core.__MAIN_PROGRAM__
    db 4
