
 NAMControllerCompiler
=======================

[![Build Status](https://travis-ci.org/memo33/NAMControllerCompiler.svg?branch=master)](https://travis-ci.org/memo33/NAMControllerCompiler)

 Contents
----------

This application allows to compile a customized NAM controller file for the
_Network Addon Mod_ for _SimCity 4_. This helps minimizing the size of the
controller, and thus loading times of the game, by installing only those
override rules that are required for one's personal style of using the NAM.

By default, the NAM installer builds a controller file that contains those rules
that are necessary for the required components. Using this application, it is
possible to exclude rules, even if the corresponding networks exist in your
cities. For instance, you can deselect various RHW networks as long as you don't
plan to touch those networks during your game play. The game will start much
faster, then. Note that redragging these RHW networks would revert the network
to RHW-2 because the required override rules are absent. In this case, run the
compiler again to build a different compiler that includes the RHW networks you
want to use.

To start this application, double-click the file `NAMControllerCompiler.bat`.
The compiler cannot build a new controller while the game is running.


 Installation
--------------

This program is automatically installed by the NAM installer. The location
will be next to the Plugins folder, typically:

    Documents/SimCity 4/NAM Auxiliary Files/Controller Compiler/


 Contact and Support
---------------------

Support is provided at
[SC4Devotion.com](http://sc4devotion.com/forums/index.php?board=90.0).

The source files can be found at
[GitHub.com](https://github.com/memo33/NAMControllerCompiler).


 License
---------

This program is released under the MIT license (see included license file).

