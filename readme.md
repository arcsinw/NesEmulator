# NesEmulator

[![Java CI with Maven](https://github.com/arcsinw/NesEmulator/actions/workflows/maven.yml/badge.svg?branch=master&event=push)](https://github.com/arcsinw/NesEmulator/actions/workflows/maven.yml)

使用Java编写的NES模拟器

## 进度
- [x] CPU (已通过nestest.nes测试，非官方指令未支持)
- [x] PPU
- [x] 双人输入支持 (P1 : 键盘:keyboard:, P2 : 手柄:video_game:)
- [ ] Mapper
    - [x] NROM
    - [x] UxROM
    - [x] MMC1
    - [ ] MMC3
    - [ ] AxROM
- [ ] APU
- [ ] wiki

## 运行方法

执行Emulator的main函数

## 使用的库
1. https://github.com/StrikerX3/JXInput (用于支持Xbox手柄)

## References
1. [NES 模拟器开发教程](https://www.jianshu.com/nb/44676155)
2. https://www.youtube.com/javidx9
3. https://github.com/OneLoneCoder/olcNES
4. https://github.com/dustpg/BlogFM/issues
5. https://github.com/tanakh/bjne-java
6. [6502 Instruction Reference](http://obelisk.me.uk/6502/reference.html)
7. [6502 Addressing Modes](http://obelisk.me.uk/6502/addressing.html)
8. [NES reference guide](http://wiki.nesdev.com/w/index.php/NES_reference_guide)
9. http://www.qmtpro.com/~nes/misc/
10. [6502 instruction set 图版](https://pastraiser.com/cpu/6502/6502_opcodes.html)
11. [6502 Instruction Set 文字版](https://www.masswerk.at/6502/6502_instruction_set.html)
12. [github.com/fogleman/nes](https://github.com/fogleman/nes)
13. [ykmr1224/NesEmulator](https://github.com/ykmr1224/NesEmulator)
14. [GunshipPenguin/nescafe](https://github.com/GunshipPenguin/nescafe)
