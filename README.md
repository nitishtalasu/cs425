# MP1: Running grep command on distributed system

This program allows a client to query distributed log files on multiple machines

## Architecture
This program is implemented using a simple client-server architecture. One of the VMs acts as a client while the remaining VMs act as servers. Both the client and server machines implement multithreading which makes the program execution faster as a result of concurrent programming.

The program starts when the user enters a grep query in the client machine. The client generates multiple threads, where each thread is connected to a unique server. Each thread is responsible for sending the grep query to the server it is connected to. This is achieved by using socket programming and input/output streams. Once the server receives the grep query, the query is executed in each server, on its local log files, and the matching lines are sent back to the client where the results from each server are stored in separate log files.

## To run the program
1. ssh into each machine `ssh <netid>@fa19-cs425-g59-XX.cs.illinois.edu`
2. clone the git repository containing the project
3. compile and run the `Server.java` file in each server by running `javac Server.java` to compile and `java Server` to run the program
4. compile and run `Client.java` in the client machine
5. Provide the grep command on the terminal when asked. Any [option] such as `-c or E` can be provided along with the pattern regex in the same input. There is no need to enter `grep`
6. The execution time for each thread is visible on the terminal output. The output of the grep command can be seen in the files labelled output_vmX.log where X is the VM number.
7. The server IP addresses and VM numbers can be modified in the `server_parameters.properties` file

## To run the test
1. ssh into each machine, to start the client and server machines
2. compile and run `LogGenerator.java`. This program runs on port 5500, and is responsible for generating log files
3. compile and run `Server.java`. This program runs on port 5000, and handles the client's grep request
4. compile and run `TestClient.java`. This program interacts with the above two programs and checks if the tests are running successfully or not.