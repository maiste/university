# Cimple image editor

![Release](https://img.shields.io/badge/Release-v1-b.svg?style=flat-square)


```sh

                                  ²`:-
                                `ommms
                               `ymmmms         `.
                             `hmmmd+        :sdmd`
                            `ymmmh.      .odmmmms
                            :dmmmd.     :smmmmmy+
                -+.       `smmmmd:  `:odmmmmh+.
               /mmm/     :dmmmmm+:ohmmmmmmy-
               smmmo    +mmmmmmmdmmmmmmdo-
               smmmo   .dmmmmmmmmmmmmy/`
               ymmmo   +mmmmmmmmmmmms
               hmmmo   hmmmmmmmmmmmd..+hdho:`
               dmmms   dmmmmmmmmmmmysdmmmmmmdy/.
              `mmmmd.  dmmmmmmmmmmmmmmmmmmmmmmmmho.
              `dmmmmd:`mmmmmmmmmmmmmmmmhsyhdmmmmmmh`
               +mmmmmmhmmmmmmmmmmmmmms.    `-ohmmmmo
                /dmmmmmmmmmmmmmmmmmm/         
                 .ymmmmmmmmmmmmmmmmm`         
                   +mmmmmmmmmmmmmmmm`           :dmmmmm:
                    :dmmmmmmmmmmmmmm/         `ommmmmmh`
                     smmmmmmmmmmmmmmmy:     -+hmmmmmmm/
                     ymmmmmmmmmmmmmmmmmdssydmmmmmmmmd+
                     dmmmmmmmmmmmmmmmmmmmmmmmmmmmmh+.
                    .mmmmmmmmmmmmmmmmmmmmmmmmmmy/.
                    +mmmmmmmmmmmmmmmmmmmmmmmd+.
                    hmmmmmmmmmmmmmmmmmmmmmmh.
                   -mmmmmmmmmmmmmmmmmmmmmmh.
                   smmmmmmmmmmmmmmmmmmmmms`

```

## Purpose 
C project for university subject CP6.

## Dependencies

To compile the project you need to have :
```sh
 $ gcc make SDL2 SDL2_Image libjpeg cmocka readline
```
To see code coverage you need to have :
```sh
 $ lcov
```

## Compilation

To compile the project :
```sh
 $ make
```

To cleanall the repository : 
```sh
 $ make cleanall
```

To edit and show the coverage report : 
```sh
 $ make coverage
 $ firefox coverage/index.html
```

## Launch

To launch the project :
```sh
  $ ./cimple
```


To display the manual where all the commands are listed :
```sh
  $ man man/cimple.man
```



