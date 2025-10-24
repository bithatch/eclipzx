REM functions and stuff
  
DIM G1 AS String
DIM G2 AS Byte 
 
CONST C1 AS String = "Hello"
CONST C2 AS Byte = 0
 
LET G1 = "Hello"
   
SUB SUB1()
	DIM L1 AS String
	PRINT "SUB1" 
	RETURN  
	PRINT 
END SUB  
     
SUB SUB2()
	DIM L1 AS String
	PRINT "SUB1" 
END SUB 

SUB SUB2A(s AS String)
	PRINT "SUB1" 
END SUB
 
 
FUNCTION FUNC1(s AS String) AS String
	LET A = 1
	LET B = 2 
	BORDER 7
	LET C = 2 * 1 
	RETURN s 
END FUNCTION 
         
FUNCTION FUNCA1() AS String 
	LET A = 1 
	RETURN FUNC1("X")
END FUNCTION
   
10 FUNCTION FUNC2()  
11 DIM B AS String
20	LET A = 1
'label: LET B = STR(1)
30 	RETURN 1  
40 END FUNCTION
  
50 SUB FUNC3()  
60	LET A = 1
label2: LET B = 1
80 END SUB
 
   

 
FUNCTION FUNC1A(s AS String)
	LET A = 1
	RETURN 1 
END FUNCTION

FUNCTION FUNC1B(s AS String, p as UInteger) AS String
	LET A = 1
	PRINT G1
'	PRINT G2
'	PRINT C1
	RETURN S 
END FUNCTION

SUB1()
FUNC3() 
FUNC1("A") 

FUNC1B("ABV", 3)

PRINT FUNC1("X")

PRINT STR(1)    
       
LET C =1



 

   
 