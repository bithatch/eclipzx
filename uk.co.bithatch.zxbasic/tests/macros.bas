#ifndef OUTER
#define OUTER
#define ATHING
#define ATHING2
 
#ifdef ATHING
PRINT "A  THING" 
REM ANOTHER THING
LET A = 1 
#endif
#ifndef ATHING2
PRINT "B  THING"
#endif  
   
 
#endif   
