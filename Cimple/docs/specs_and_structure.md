## Basic Version (v1.0)

The first release will include all the basic functionalities (i.e. the minimal part of our project).
It was decided to separate the model from the view and to make them communicate through the controller only.

After launching Cimple, the user can open one or several images. He will be able to edit them through the console commands.

**Different formats.** The user can open images of different formats as .jpg, .png and bitmap. He can also choose the format while saving the image.

**Selecting zone.** The user is able to pass a command *select* and then select a zone by clicking on the image and dragging it to modify the selection. Upon releasing the mouse the chosen zone will be selected and the user will be able to apply changes to it.

**Cut, copy and paste.** The user may also cut or copy the selected region and then paste wherever he wishes, even on another image. If no region is selected, Cimple pass in select mode.

**Image modifications.**

  * Transformations
    * Vertical and horizontal symetries
    * Image rotations mod90
  * Frame modifications
    * Resize working area (extending or reducing  the number of pixels the user is working on)
    * Modify the image size
  * Color modifications
    * Fill a selected region with a color
    * Replacing a color by another color in a selected region
    * Apply the negative effect on a selected region
    * Apply a Grey & White effect on a selected region
    * Apply a Black & White effect on a selected region


### Commands signatures

The arguments passed in <...> are mandatory and the ones in [...] are optionnal.

* Image transform
  - ```symmetry <v | h>``` : Command to apply a vertical or horizontal symmetry to current buffer image.
  - ```rotate [-r] n ``` : Command to rotate image by n degrees. If -r is present, rotate in counter-clockwise. In case n isn't a mulitple of 90, raise an error.
  - ```copy [-a]``` Copy an area into the buffer. If the flag -a is passed, applies to all the image, otherwise launches select mode in buffer image screen.
  - ```paste [-a]``` Paste the buffer onto the current image. If the flag -a is passed, applies to all the image, otherwise launches select mode in buffer image screen.
  - ```cut [-a]``` Copy an area into the buffer and fill the area with black. Put the selected area in black and white. If the flag -a is passed, applies to all the image, otherwise launches select mode in buffer image screen.
* Frame modifications
  - ```truncate [-s origin_x origin_y end_x end_y]``` : Command to specify a new square inside the current buffer image. Launch select mode and focus on image screen if command launches without any arguments. If -s is present , user have to specify the square dimensions in command line.
  - ```resize <workspace | image> width height``` : Command to resize an image or the workspace (paint-like method).
* Color modifications
  - ```fill [-a] red green blue alpha``` : Command to fill an area in image with a rgba color. If the flag -a is passed, fills all the image, otherwise launches select mode in buffer image screen.
  - ```replace  [-m margin] [-a] red green blue alpha red green blue alpha``` : Command to replace a color with another one.
  - ```contrast [-a] percent``` Increase or decrease the contrast of the current buffer. Put the selected area in black and white. If the flag -a is passed, applies to all the image, otherwise launches select mode in buffer image screen.
  - ```light [-a] percent``` Increase or decrease the light of the current buffer. Copy an area into the buffer and fill the area with black. If the flag -a is passed, applies to all the image, otherwise launches select mode in buffer image screen.
  - ```negative [-a]``` Put the selected area in negative. If the flag -a is passed, applies to all the image, otherwise launches select mode in buffer image screen.
  - ```greyscale [-a]``` Put the selected area in greyscale. If the flag -a is passed, applies to all the image, otherwise launches select mode in buffer image screen.
  - ```bnw [-a]``` Put the selected area in black and white. If the flag -a is passed, applies to all the image, otherwise launches select mode in buffer image screen.
* I/O and window management
  - ```load [-w windowId] imagepath``` : Command to load image in a window. To load image in a specific window, already opened, you need to add -w flag and the window id . If the id is unspecified, it opens in the new image.
  - ```save [-p imagepath]``` : Command to save an image used in the current window. In order to change image format , we need to use -p and write a valid image path with the new extension.
  - ```list_buffer``` : List all opened buffers.
  - ```switch_buffer dest``` : Switch to the destination buffer.
  - ```quit [-w buffer]``` Quit a buffer specified by `buffer`. If none is specified, applies to all buffers opened.
  - ```move_buffer dest_id``` Move current image to another opened window. If the destination window is empty, it changes the current window id. Otherwise, it moves the current window content into the destination one.
  - ```help``` Display command format.
* Scripts
  - ```apply_script path``` apply the script located in given pathto the current buffer
  - ```edit_script path``` opens the script located at path if it exists, otherwise creates a new one
* Multiple file treatment
 - ```bundle regex cmd``` Apply a command (bnw, negative, greyscale) to a set of images, specified by a regex, in the current directory.
  ## Extended Version (v2.0)

For now, the extensions that we consider adding are :

**Scripts.** The user will be able to write commands in a separate file and then apply all of them to a chosen image.

**Scripts editor.** In order to improve the working flow and give the user a possibility to edit his scripts without closing the program, we will add a *edit_script* command. Once this command called, the main program will launch a process with a text editor ($EDITOR and nano if not defined). The user will be able to modify his script and on closing his editor, the main Cimple process will continue.

**Undo/redo.** The program will allow to revert the last applied change, and recursively the one before (etc...) or redo changes that were done (undo the undos).

**Crash resistance.**
  * On every launch the program checks for temporary files in /tmp/cimpletmp/ directory. In case it finds any files, it lists them and the user can choose which ones to load.
  * In case the program exits normally, all the files at /tmp/cimpletmp/ are deleted.
  * In case the program crashes, the program will load images from the /tmp/cimpletmp directory when it's opened again.


**Group editing.** Apply an action to a set of images.

## Project structure

The project main part consists of two separate parts, the model and the view, linked by a controller.
Here is what the directory tree would look like :

```sh
cimple
├── const.mk
├── docs
├── include
├── LICENSE
├── Makefile
├── Dockerfile
├── README.md
├── scripts
│   ├── config_uncrustify.cfg
│   └── uncrustify.sh
├── src
│   ├── controller
│   │   └── cmd_line.c
│   ├── main.c
│   ├── model
│   │   ├── in.c
│   │   ├── m_color.c
│   │   ├── m_frame.c
│   │   ├── m_transform.c
│   │   ├── m_image.c
│   │   ├── out.c
│   │   └── parse.c
│   └── view
│       ├── cmd_view.c
│       ├── event.c
│       └── window.c
└── tests
```

Short description of these directories :
  * build :
  temporary files / files created by compiling the code (i.e. .o etc).
  * docs :
  documentation on how to use the program.
  * include :
  contains all the headers
  * lib :
  all library files (SDL etc)
  * tests :
  all the tests needed, including the ones for Travis CI.
  * src :
  main source files, see details below.
  * scripts :
  scripts needed for developping

### Source files

Here are some brief explanations on the source files and what they will do.

* view :
  * **cmd_view.c :**
     File responsible for the displays (such as coordinates while selecting region) and the input in the console.
  * **event.c :**
     Event handler for mouse clicks on displayed images.
  * **window.c :**
    Displaying images on the screen.
* model :
  * **in.c :**
    Opening/importing existing image.
  * **out.c :**
    Saving image on the machine.
  * **m_color.c :**
    Contains all the color modification functions.
  * **m_frame.c :**
    Contains all the window and frame changing functions.
  * **m_transform.c :**
    Contains symmetry and rotation functions.
  * **m_image.c :**
     Contains image management functions.
  * **parse.c :**
    Gets an input line from the controller and parses it returning command structure.
* controller :
  * **cmd_line.c :**
    manage view and model calls.
* **main.c :**
    Main program.


## Programs to use

* Make
* Git
* Uncrustify
* Lcov

## Technical specs

Command line structure for the parser :

```c
struct cmd {
  char * name;
  char ** args;
  int size;
};
```
