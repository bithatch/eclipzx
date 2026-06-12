    call    rand
    ld  bc,37
    add hl,bc
    call    srand_fastcall
;    } while(read_key() !=  ' ' );

.i_305
    call    _read_key
    ld  de,32
    and a
    sbc hl,de
    jp  nz,i_307    ;EOS

.i_307
rand
