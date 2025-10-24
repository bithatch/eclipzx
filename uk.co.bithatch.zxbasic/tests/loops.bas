 
' With an empty line at start

'DO
'    PRINT "HELLO"
'LOOP : 
 REM This loop repeats forever.
    
10 DO
20    PRINT "HELLO"
30 LOOP :
40  REM This loop repeats forever.    
  
LET A = 0
DO 
    PRINT "HELLO"
LOOP UNTIL A = 1 
    
50 DO
60    PRINT "HELLO"
70 LOOP UNTIL A = 1  
   
IF A > 1 THEN 
	PRINT "Test"
END IF
 
80 IF A > 1 THEN
90	PRINT "Test"
100 END IF
   
 
REM Multiple lines
DO
    PRINT "HELLO"
    PRINT "HELLO 2"
    PRINT "HELLO 3"
    CONTINUE DO
LOOP UNTIL A = 1 
    
REM Nested
DO
	DO
		PRINT "HELLO"
	LOOP UNTIL A = 1 
LOOP :
   
REM in a single line!  
DO LOOP UNTIL INKEY$ <> ""

DO 
    PRINT "HELLO"
LOOP WHILE A = 1
   
 
DO WHILE A = 1 
	PRINT "HELLO"
LOOP
  
REM in a single line!
DO LOOP WHILE INKEY$ = ""
  
WHILE A = 1
	PRINT "HELLO"
END WHILE
  
WHILE A = 2
	PRINT "HELLO"
	CONTINUE WHILE
WEND
 
1000 WHILE A = 2
1010	PRINT "HELLO"
1020	CONTINUE WHILE
1030 WEND 
    
  
