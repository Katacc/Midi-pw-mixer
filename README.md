<div align="center">
    <h1>Midi pw-mixer</h1>
    A Midi mixer software to target Pipewire on linux and Korg nanoKONTROL2

![Maven build](https://github.com/Katacc/Midi-pw-mixer/actions/workflows/maven.yml/badge.svg)

</div>

## Getting started

### Prequisites

- Java 21 JDK or JRE
- Pipewire
- pw-dump
- wpctl
- User configuration file
- Korg nanoKONTROL2

### Installation

You can either grab the repo and compile the software with maven using

```sh
$ mvn package
$ cd target
$ java -jar midi-mixer-<VERSION>.jar # Run the program
$ java -jar midi-mixer-<VERSION>.jar & disown # To send the program to background
```

Or just grab the latest build from release, I will include a bash script for
easier launching of the application, then just put them into your PATH and
autostart on login if you want to

### Usage

This software relies on the user configuration file. Please create a folder and
a file in your `/home/user/.config/` The folder and configuration files should
be `/home/user/.config/midi-mixer/config.ini`

and heres a configuration file example

```sh
[fader 5]
application = spotify

[fader 6]
application = Zen

[fader 7]
application = Last Epoch.exe:Warframe.x64.exe

[knob 16]
application = Firefox
```

Follow the same syntax to add the rest of the faders, knobs and applications to
map to the faders or knobs.

The faders IDs are from left to right 0 - 7 and the knob IDs are 16 - 23. To map
spotify to the leftmost fader you would type

```sh
[fader 0]
application = spotify
```

To map Firefox to the leftmost knob you would type

```sh
[knob 16]
application = Firefox
```

You can map any pipewire node to any fader or knob, i.e. not only applications,
but also for example output and devices:

```sh
[fader 6]
application = alsa_output.usb-Beyerdynamic_FOX_5.00-00.analog-stereo

[fader 22]
application = alsa_input.usb-Beyerdynamic_FOX_5.00-00.mono-fallback.3
```

You can force reconfiguration with `cycle` button from midiKONTROL2. This is
required if you open up a application after starting midi-mixer

(This way we avoid any time based configuration parsing that would lead to
constant CPU usage for the time calculations)

### Debugging

You can dump raw MIDI messages received and Pipewire commands sent by enabling
the debug mode using the `-d` parameter:

```sh
$ java -jar midi-mixer-1.0-SNAPSHOT.jar -d # Run the program in debug mode
```

### Future and additions

This project was meant to target my specific setup, but if enough interest
rises, I might add support for other configurations. If you want a feature
added, be sure to make an issue or pull request with the added feature, or make
a fork for yourself :)
