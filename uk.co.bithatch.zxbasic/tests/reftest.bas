' Tests all the types of references 

' Variable
LET A = 10
PRINT A

' Variable case insensitive
LET B = 20
PRINT b
  
' Function
FUNCTION C() 
	RETURN 99
END FUNCTION
PRINT C()
  
' Function case insensitive
FUNCTION D() AS UINTEGER
	RETURN 101 
END FUNCTION 
PRINT d()  
   
' Sub
SUB E() 
	PRINT "SUB" 
END SUB 
E() 

' Sub case insensitive
SUB F()
	PRINT "SUB2" 
END SUB 
f()  
   
' Built-in function
PRINT STR(1)
 
' Built-in function insensitive
PRINT str(1)
    
' Label goto
gotoTarget:
PRINT "A"
GOTO gotoTarget 
        
' Line goto 
10 PRINT "B"
20 GOTO 10
     
' Memory address
DIM udg(7) AS uByte => {0,1,3,7,15,31,63,127} 
POKE UINTEGER 23675,@udg(0)
    
' Memory address case
DIM udg2(7) AS uByte => {0,1,3,7,15,31,63,127} 
POKE UINTEGER 23675,@UDG2(0)  ' compiler fails this even with case flag , not much we can do'

' Address of label
datalabel:  
asm  
; asm comment  
	DB 0,0,0, 0,0,0, 0,0,0, 0,0,0
innerlabel: 
	DB 0,0,0, 0,0,0, 0,0,0, 0,0,0
end asm  ' comment'
print @datalabel  ; "stuff"      
 
  
 
 
