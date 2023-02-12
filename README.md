 NAMControllerCompiler
=======================

 Contents
----------

This application allows you to compile a customized NAM controller file for the
[Network Addon Mod](https://www.moddb.com/mods/network-addon-mod) for SimCity 4.

By default, the NAM installer comes with a full-sized controller file.
Using this compiler application, you can build a smaller controller file that
only contains what you need, which helps reduce the start-up time of the game.


 Installation
--------------

Download the [latest version of the compiler](https://github.com/memo33/NAMControllerCompiler/releases)
and launch it by double-clicking the file `NAMControllerCompiler.bat` while the
game is not running. Select a copy of the
[Controller directory](https://github.com/NAMTeam/Network-Addon-Mod/tree/master/Controller)
as `input`.


 Source code
-------------

The source files can be found at
[GitHub.com](https://github.com/memo33/NAMControllerCompiler).


 Build Instructions
--------------------

Run

    ant init-ivy
    ant dist

Optionally, run `ant test` (currently fails due to https://github.com/stefanbirkner/system-rules/issues/85).


 License
---------

This program is released under the MIT license (see included license file).
