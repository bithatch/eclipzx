# EclipZX

![EclipZX](src/web/title.png)

EclipZX is a Eclipse based IDE targeting development of games and applications on the PC and deploy to all models of the ZX Spectrum, including modern reboots such as the ZX Spectrum Next.

Building on the shoulders of giants, such as Boriels ZX Basic, Z88DK, and JSpeccy, EclipZX aims to bring it all together with other new purpose built tools.

It consists of a suite of Eclipse plugins, all packaged up together in an an easy to use but powerful (nearly) all-in-one development kit.


## Features

 * Boriel ZX Basic support. Write your games and applications in a modern ZX Basic that compiles to machine code.
 * Z88DK C support. Write your games and applications in C.
 * Define multiple SDKs for both ZX Basic and Z88DK and select the one to use with your project. E.g. A recent version of Boriels SDK will be bundled, but you can always download your own.
 * Adds concept of User Libraries to ZX Basic that you can share with others to use in their EclipZX projects. Comes with one example implementation, the great NextLib.
 * Deploy to any number of common formats such as NEX, TGZ, TAP, SNA and more.
 * Fully featured built in emulator based on JSpeccy.
 * Click+Run your source file, it will be built and deployed to your chosen emulator.
 * Comes with emulator launch templates for CSpect and Zesarux. 
 * Create, Format and Manage FAT16/FAT32 disk images, for deploying your games on SD 
   cards for the next. The same system is used for launching emulators that support SD card images.
 * ZX Next Sprite editor, and UDG / Character set Editors for original Spectrums.
 * ZX Next palette editors.
 * A screen editor supporting .SCR for original video modes, and all new ZX Next modes¹ .
 * Compress and decompress files using ZX0. 
 * AYFX Effects Editor.
 * Various project creation wizards, imports and exports.
 * Debugging support for internal emulator and external emulators that support DeZOG¹ .
 * Experimental built in ZX Basic interpreter where you can test short pieces of code.
 * Highly configurable globally and at the project level.
 * Infinitely expandable with compatible plugins from the Eclipse Marketplace.
 
*¹ Under development now*

## Status as of 15/08/2025

EclipZX has already undergone heavy private development, and is coming close to be in a state
ready for public consumption. 

The plan is to release the first public beta version along with the uploading all the source 
to this repository and opening the issue tracker at point early September 2025.

### Remaining Tasks

Just a rough list, there is a lot more to do that this, but these are the ones.

 * Undo support in all editors (half done).
 * The screen editor is relatively immature, but should progress rapidly.
 * Debug support using (e.g. CSpect+DeZOG and others) is immature.
 * A full tracker to supplement the AYFX effects editor (if I get time).
 * Various bugs and problems with product builds.
 
## The Future

It depends on interest of course. But I will be using these tools myself, so it will likely be kept to date. 

But if there is interest, more and better Emulator supports, and more platforms and graphics formats are the obvious areas where EclipZX could be expanded, even beyond the Sinclair range.


## License

EclipZX plugins will be licensed under the usual Eclipse Public License version 2, and any original support libraries will be under a liberal license as possible (likely MIT). 

Binary builds will be available as pay-what-you-want with no minimum. EclipZX is and will always bee free and open source.

## Project status
If you have run out of energy or time for your project, put a note at the top of the README saying that development has slowed down or stopped completely. Someone may choose to fork your project or volunteer to step in as a maintainer or owner, allowing your project to keep going. You can also make an explicit request for maintainers.
