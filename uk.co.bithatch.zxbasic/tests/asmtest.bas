function fastcall checkints() as ubyte 
asm 
    start:    
        ; Detect if interrupts were enabled 
        ; The value of IFF2 is copied to the P/V flag by LD A,I and LD A,R.
        ex af,af'
        ld a,i 
        ld a,r 
        jp po,intsdisable 
        ld a,1              ; ints on 
        ld (itbuff),a
        ex af,af'
        ret 
    intsdisable:
        xor a               ; ints off 
        ld (itbuff),a    
        ex af,af'
        ret 
    itbuff:
        db 0 
end asm 
end function