find . -type f \( -iname 'temp.tap' -o -iname 'temp.bas*' -o -iname 'Compile.txt' \
    -o -iname 'Memory.txt' -o -iname '*.cfg' -o -iname '*.sna' -o -iname '*.bmi' \
    -o -iname '*.bor' -o -iname '*.nex' -o -iname '*.asm' \) -exec rm -f {} \; -print 
