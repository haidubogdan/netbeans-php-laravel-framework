# Netbeans Php laravel framework support

Netbeans Php laravel support

WIP

## Requirements
[![](https://img.shields.io/badge/Version-2.0+-green)]() [![](https://img.shields.io/badge/Netbeans-18+-green)]()

## Github actions

- releases and nbm artifact upload can be done with github action also

## Usage

### Features

Folder nodes for:
  - configuration files

**completion**

- blade template path completion on `render`, `make` and `view` methods
- config path completion for `config` method

**declaration finder**

- blade template declaration finder
- config declaration finder

**Commands**

You will able to execute php Script using `artisan` directly from the IDE.

![image](https://github.com/user-attachments/assets/3b284d8f-8a95-47c5-8984-e5e96800f2f3)


It has an inbuild customization to allow **remote connection** and **docker** usage.

> remote connection works only with docker

- wip console commands

!! IMPORTANT
!! **Windows**

For interactive docker execution commands, it would be best to install gihtub bash for windows.

Otherwise the terminal commands will try to use the default cgydrive

**Configuration**

Right click on the project.

![image](https://github.com/user-attachments/assets/922930fd-834b-4cb2-b98b-0080da78a3c2)

- automatic laravel version detection from **composer.json** file
- script commands configuration

![image](https://github.com/user-attachments/assets/ba8a3adc-6e74-443a-b458-01214d7e3eb1)
