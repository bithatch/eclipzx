REM test for local array with labels

DIM a(1 TO 3) as UInteger => {@label1, @label2, @label3}
POKE a(1), 5
label1:
label2:
label3:
