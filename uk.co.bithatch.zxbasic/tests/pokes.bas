
dim tlen as uinteger
filen = "myfilename" + chr(0)
tlen=len(filen)+1
for nbx=0 to tlen
    poke @filename+nbx,code (filen(nbx))
next  
poke @filename+nbx+1,0


filename:

asm         
filename:
    DEFS 255,0
endfilename:    
end asm 
