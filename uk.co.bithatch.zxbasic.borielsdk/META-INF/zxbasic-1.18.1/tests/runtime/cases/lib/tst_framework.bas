' Simple TEST framework

#define FINISH \
  ASM \
    RST 8 \
    DB 8 ;; "STOP" \
  END ASM


#define SHOW_OK \
  PRINT PAPER 4; INK 0; " OK "; PAPER 8; TAB 31;

#define SHOW_ERROR \
  PRINT PAPER 2; INK 0; FLASH 1; " ERROR "; PAPER 8; FLASH 0; TAB 31;

#define REPORT_OK \
  SHOW_OK: FINISH

#define REPORT_FAIL \
  SHOW_ERROR: FINISH

#define ASSERT(val) \
  IF NOT VAL THEN REPORT_FAIL

#define SHOW_RESULT(IS_OK) \
  IF IS_OK THEN \
    SHOW_OK \
  ELSE \
    SHOW_ERROR \
  END IF

#define SHOW_RESULTLN(IS_OK) \
  SHOW_RESULT(IS_OK): PRINT

#define INIT(msg) \
  BORDER 0: PAPER 0: INK 7: CLS \
  PRINT msg
