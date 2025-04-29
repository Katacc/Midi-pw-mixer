<div align="center">
    <h1>Midi pw-mixer</h1>
    A Midi mixer software to target Pipewire on linux and Korg nanoKONTROL2
    
    ![Maven build](https://github.com/Katacc/Midi-pw-mixer/actions/workflows/maven.yml/badge.svg)
    
</div>

## Getting started

### Prequisites
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
$ java -jar midi-mixer-1.0-SNAPSHOT.jar # Run the program
$ java -jar midi-mixer-1.0-SNAPSHOT.jar & disown # To send the program to background
```
Or just grab the latest build from release,
I will include a bash script for easier launching of the application,
then just put them into your PATH and autostart on login if you want to

### Usage

This software relies on the user configuration file.
Please create a folder and a file in your `/home/user/.config/`
The folder and configuration files should be
`/home/user/.config/midi-mixer/config.ini`

and heres a configuration file example
```sh
[fader 5]
application = spotify

[fader 6]
application = Zen

[fader 7]
application = Last Epoch.exe:Warframe.x64.exe

```

Follow the same syntax to add the rest of the faders and applications to map to the faders
(Add an empty line in to the file, might fix at some point but it requires one empty line after configurations.)

The faders id's are from left to right 0 - 7 so to spotify to the leftmost fader you would type
```sh
[fader 0]
application = spotify

```

The program automatically refreshes the configuration every 200 Midi messages, wich is full fader movement almost twice,
so if your volumes don't work or doesnt pick up immediately
after changing configuration or opening an application, do a full fader
movement twice to reconfigure.

(This way we avoid any time based configuration parsing that would lead to
constant CPU usage for the time calculations)

### Future and additions

This project was meant to target my specific setup, but if enough interest rises, I might add support for other configurations.
If you wan't a feature added, be sure to make an issue or pull request with the added feature, or make a fork for yourself :)
