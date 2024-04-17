# HexDebug

Hex Casting addon that runs a local debug server using DAP.

## TODOs

* Step over/out don't work in certain cases.
* Switching to a different frame and back makes the line number wrong since frames don't keep track of evaluated patterns.
* The server sometimes refuses to die when closing the game???
* Ideally the debugger should keep a map of the original source location of each iota so it can more accurately display the program.
