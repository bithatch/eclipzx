label: PRINT 
REM A comment on a label only line
5 PRINT "START WITH  A NORMAL LINE" 
10 REM A Comment with a line number
15 PRINT "SEP"
17 PRINT "SEP"
REM just a comment
19 PRINT "SEP"  
20 ' Another comment on a line number
' Just another comment  
21 PRINT "SEP" 
label0: PRINT
label1: ' Again  
22 PRINT "ON END OF LINE" ' I comment on things
PRINT "SO DO I" : REM And me 
PRINT "SO DO I" REM And me
      
29 PRINT "SEP"
30 /' And this is a multiline comment 
with more than one line 
'/  
31 PRINT "SEP"
40 PRINT "ANOTHER NORMAL LINE"
/' Multi
line '/
41 PRINT "SEP"
label2: /'Multi line 
again '/  
     
50 PRINT "THE NEXT EXPRESSION SHOULDNT BE A COMMENT" ; STR(1)
  
            