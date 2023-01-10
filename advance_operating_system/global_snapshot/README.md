# Consistent Global Snapshot Protocol

Implementation of Chandy/Lamport's snapshotting protocol. Starts nodes that randomly sends messages between each other. Uses the protocol to generate consistent snapshot logs and detect termination. Tests for consistent snapshot using Fidge/Mattern's vector clock.

## Running the program
- Run 'launcher.sh' to start multiple the program multiple times based on 'config.txt'