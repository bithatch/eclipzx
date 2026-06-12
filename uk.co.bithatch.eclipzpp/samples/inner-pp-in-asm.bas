asm  
    ld iy,$5c3a 
    jp nbtempstackstart
end asm 

#ifdef IM2 
    Imtable:
    ASM
        ALIGN 256
        IMvect:
        defs 257,0
    #ifndef NOAYFX
    afxBankAd:
        dw 0,0,0,0 
        
    afxChDesc:
        DS 6*3
        db 0
    #endif
    end asm 

#endif 
filename:

asm         
filename:
    DEFS 255,0
endfilename:    
end asm 