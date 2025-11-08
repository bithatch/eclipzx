;EclipZX

;--------------------------------
;Include Modern UI

  !define MUI_WELCOMEFINISHPAGE_BITMAP "sidebar.bmp"
  !define MUI_UNWELCOMEFINISHPAGE_BITMAP "sidebar.bmp"
  !include "MUI2.nsh"
  !include LogicLib.nsh


;--------------------------------
;General

  ;Name and file
  Name "EclipZX"
  ;OutFile ..\..\target\release\EclipZX_Installer.exe

  ;Default installation folder
  InstallDir "$LOCALAPPDATA\EclipZX"
  
  ;Get installation folder from registry if available
  InstallDirRegKey HKCU "Software\EclipZX" ""

  ;Request application privileges for Windows Vista
  RequestExecutionLevel admin


;--------------------------------
;Variables

  Var StartMenuFolder
;--------------------------------
;Interface Settings

  !define MUI_ABORTWARNING
  !define MUI_HEADERIMAGE
  !define MUI_HEADERIMAGE_BITMAP "banner.bmp" ; optional

;--------------------------------
;Pages

  !insertmacro MUI_PAGE_WELCOME
  !define MUI_PAGE_CUSTOMFUNCTION_SHOW licpageshow
  !insertmacro MUI_PAGE_LICENSE "../../../../LICENSE"
  !insertmacro MUI_PAGE_COMPONENTS
  !insertmacro MUI_PAGE_DIRECTORY

;Start Menu Folder Page Configuration
  !define MUI_STARTMENUPAGE_REGISTRY_ROOT "HKCU" 
  !define MUI_STARTMENUPAGE_REGISTRY_KEY "Software\EclipZX" 
  !define MUI_STARTMENUPAGE_REGISTRY_VALUENAME "EclipZX"
  
  !insertmacro MUI_PAGE_STARTMENU Application $StartMenuFolder

  !insertmacro MUI_PAGE_INSTFILES
  !define MUI_FINISHPAGE_NOAUTOCLOSE
  !define MUI_FINISHPAGE_RUN
  !define MUI_FINISHPAGE_RUN_NOTCHECKED
  !define MUI_FINISHPAGE_RUN_TEXT "Start EclipZX"
  !define MUI_FINISHPAGE_RUN_FUNCTION "LaunchLink"
  !define MUI_FINISHPAGE_SHOWREADME_NOTCHECKED
  !define MUI_FINISHPAGE_SHOWREADME $INSTDIR\README.md
  
  
  !insertmacro MUI_PAGE_FINISH
  
  !insertmacro MUI_UNPAGE_CONFIRM
  !insertmacro MUI_UNPAGE_INSTFILES
  !insertmacro MUI_UNPAGE_FINISH
  
;--------------------------------
;Languages
 
  !insertmacro MUI_LANGUAGE "English"

;--------------------------------
;Functions

Function .onInit
UserInfo::GetAccountType
pop $0
${If} $0 != "admin" ;Require admin rights on NT4+
    MessageBox mb_iconstop "Administrator rights required!"
    SetErrorLevel 740 ;ERROR_ELEVATION_REQUIRED
    Quit
${EndIf}
FunctionEnd



;--------------------------------
;Installer Sections

Section "EclipZX" EclipZX

  SetOutPath "$INSTDIR"
  
  ;ADD YOUR OWN FILES HERE...
  File /r ..\..\..\..\uk.co.bithatch.eclipzx.repository\target\products\eclipzx\win32\win32\x86_64\*.*
  File eclipzx.ico
  File ..\..\..\..\README.md
  
  ;Store installation folder
  WriteRegStr HKCU "Software\EclipZX" "" $INSTDIR
  
  ;Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"
  
  ;Create shortcuts
  CreateDirectory "$SMPROGRAMS\$StartMenuFolder"
  
  CreateShortCut "$SMPROGRAMS\$StartMenuFolder\EclipZX.lnk" "$INSTDIR\eclipzx.exe" \
    "" \
    $INSTDIR\eclipzx.ico 0 SW_SHOWNORMAL ALT|CONTROL|SHIFT|P "Eclipzx"
    
  CreateShortCut "$SMPROGRAMS\$StartMenuFolder\Uninstall.lnk" "$INSTDIR\Uninstall.exe"
SectionEnd

;--------------------------------
;Descriptions

  ;Language strings
  LangString DESC_EclipZX ${LANG_ENGLISH} "Write games and apps for the ZX Spectrum family."

  ;Assign language strings to sections
  !insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
    !insertmacro MUI_DESCRIPTION_TEXT ${EclipZX} $(DESC_EclipZX)
  !insertmacro MUI_FUNCTION_DESCRIPTION_END

;--------------------------------
;Uninstaller Section

Section "Uninstall"

  ;ADD YOUR OWN FILES HERE...

  Delete "$INSTDIR\Uninstall.exe"

  RMDir "$INSTDIR"

  !insertmacro MUI_STARTMENU_GETFOLDER Application $StartMenuFolder
    
  Delete "$SMPROGRAMS\$StartMenuFolder\Uninstall.lnk"
  Delete "$SMPROGRAMS\$StartMenuFolder\EclipZX.lnk"
  RMDir "$SMPROGRAMS\$StartMenuFolder"

  DeleteRegKey /ifempty HKCR "eclipzx"
  DeleteRegKey /ifempty HKCU "Software\EclipZX"

SectionEnd

Function licpageshow
    FindWindow $0 "#32770" "" $HWNDPARENT
    CreateFont $1 "Courier New" "$(^FontSize)"
    GetDlgItem $0 $0 1000
    SendMessage $0 ${WM_SETFONT} $1 1
FunctionEnd

Function LaunchLink
   ExecShell "" "$SMPROGRAMS\$StartMenuFolder\EclipZX.lnk"
FunctionEnd