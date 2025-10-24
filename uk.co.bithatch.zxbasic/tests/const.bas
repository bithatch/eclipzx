DIM A1 As String   
DIM A2 As Byte
DIM A3(10) As Byte  
DIM A4(10,10) As Byte 
DIM A5(10,10 * 20) As Byte
DIM A6 As String = "Blergh" 
DIM A7 As Integer = 0
   
CONST G1 AS String = "Blergh"
CONST G2 AS Byte = 20
  
PRINT "HELLO"
  
DIM UDGaddr AS Uinteger AT 23675
PRINT "UDG memory address is "; AT 10, 2; UDGaddr  
     
REM udg will be an array of 8 UBytes
REM Remember, the default in ZX Basic is for arrays to begin at zero, so 0-7 is 8 bytes
DIM udg(7) AS uByte => {0,1,3,7,15,31,63,127} 
 
REM This sets the System UDG variable to point to the 1st array element:
POKE UINTEGER 23675,@udg(0): REM udg(0) is the 1st array element  

 
         

   