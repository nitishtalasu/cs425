# MP2: Distributed Group Membership

This program implements a heartbeat based group membership protocol

## Design
This program uses a multi-threaded architecture where each Main class spawns four threads running simultaneously and continuously - client, server, failure detector and heartbeat handler threads.
One chosen server acts as the introducer node which is responsible for allowing new processes to join the membership system. No other process is allowed to join the system when the introducer is down. When the introducer is back up, all existing processes automatically rejoin the system.
Failure detector detects a process as failed if it does not receive heartbeat in the last 1 second, and removes the process from the membership list if heartbeat is not received for 2 seconds.
Each heartbeat handler sends heartbeats to 1 predecessor and 2 successor nodes every 500 milliseconds. The neighbors are chosen based on a sorting algorithm, representing a pseudo-ring topology. Whenever a new process joins the system, the membership list is automatically sorted.
Since it is guaranteed that there cannot be more than 3 failures simultaneously, this system guarantees
The client thread expects various inputs from the user to join, leave, exit membership groups and print useful information such as nodeId, membershipList, etc.

## Dependencies
1. Maven - To install maven, run `sudo yum install maven` in the VM

## To run the program
1. ssh into each machine `ssh <netid>@fa19-cs425-g59-XX.cs.illinois.edu`
2. clone the git repository containing the project and navigate to `Mp2` folder
3. Run `mvn clean install` to install dependencies
4. Run `mvn exec:java -Dexec.mainClass="Main"` to start the Main thread 
5. When the terminal shows `Waiting for user input..`, you can enter commands: `join`, `leave`, `printlist`, `printId`, `exit`. 
6. Use the above mentioned print options to see the updated lists.

### Notes
1. There is no separate command to run the introducer. Any one VM can be chosen as the introducer and its IPAddress has to be updated in the `Introducer.java` enum
2. To update the time duration for failure detection, the corresponding value can be updated in the `FailureDuration.java` enum
3. Every time any of these files are changed, run `mvn clean install` and `mvn exec:java -Dexec.mainClass="Main"`

