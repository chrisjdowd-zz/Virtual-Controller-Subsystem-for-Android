eMPIO
=====

Enhanced Multi Peripheral Input Output subsystem for Android

EMPIO was originally designed for the Ouya, but it has been adapted to work with Android phones and tablets as well.
Currently, the application may not run on either without a few slight tweaks. These will be added as I have time, as will
continued development. 

Primary control is currently expected to be done with an Ouya controller or PS3 sixaxis as of the initial release.

=====

EMPIO is an application that allows you to fully remap your controller. At the moment, it supports Ouya and wired/wireless PS3 controllers (tested with sixaxis). 
The biggest feature of this app is that it allows you to map your controller to TOUCH SCREEN presses - even though there's no touch screen on the console!

Upon launch, you're greeted with a setup activity that gives you some information about the app. The main screen is a 2x3 grid of the applications you have installed. Pressing U brings up the options menu. Pressing Y brings up a basic file browser to install an apk.

Upon selecting an app to run, it checks if your console is rooted. If it's not rooted, it asks you to return after rooting it. 
Then it runs the background service.

The service gets exclusive control over your controller and loads the map file on the internal storage. Once the map is loaded, it interprets whatever your controller is doing and maps it to whatever you specified.


* Current supported controllers:

1. Ouya
2. PS3 Sixaxis wired/wireless


* Future planned controller support:

1. 360
2. wiimote
3. custom input (keyboard, mouse, 3rd party controllers)

* Features:

1. Map controller to various things
2. Add cycles - a grid of points you can cycle through
3. Multiple sets - set of mappings, includes cycles and button maps
4. Assign a map to be called when pressing a cycle point
5. Assign a map to be called when selecting/cycling to a point
6. Map analog stick to on screen analog - choose center position, radius, and deadzone. Analog stick moves around the center position, up to the radius and stops when the deadzone is reached
7. Set a variable and then retrieve it later

* Current map types:

1. Button/key
2. Touch
3. Callback

* Future planned map types:

1. drag
2. gesture
3. Macro (touch & button)

Documentation will be updated as I have time.

This app is **BETA** as it is not anywhere near "completion".
