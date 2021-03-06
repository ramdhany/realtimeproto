# Real-Time Protocol for fetching number sequence



## Protocol
The protocol is implemented using Google Protocol Buffers and gRPC.

```protobuf

service NumberSequenceGenerator{
    rpc startNumberSequence(NumSeqRequest) returns (stream NumSeqResponse);
}

enum ServerMode{
    STATELESS_SERVER = 0;
    STATEFUL_SERVER = 1;
}

message NumSeqRequest{
    string clientId = 1;
    uint64 startNumber = 2;
    int32 intervalMs = 3;
    uint32 numTotalMessages = 4;
    ServerMode serviceMode = 5;
}

message NumSeqResponse{
    string clientId = 1;
    int32 SeqNo = 2; 
    uint64 number = 3;
    int64 checksum = 4;
}
```
The client calls `startNumberSequence()` on a gRPC service stub with a `NumSeqRequest` message. 
The request message (`NumSeqRequest`) has the following fields:
- `clientId`: a client identifier e.g. a UUID (Mandatory in server STATEFUL mode)
- `startNumber`: a starting value for sequence (set only in stateless mode for retries )
- `intervalMs` : specifies time interval at which ther server will send response messages
- `numTotalMessages`: used only in STATEFUL mode to specify count of numbers to the server
- `serviceMode`: `STATELESS_SERVER` | `STATEFUL_SERVER`, specifies which mode server will operate for this request 

It then receives a stream of `NumSeqResponse` messages from the server.
The response message has the following fields:
- `clientId` : client identifier value as set in corresponding request
- `SeqNo`: sequence number for this message starting from 1
- `number`: usigned integer generated and sent by the server
- `checksum`: Only set by the server when in STATEFUL mode. 


## Client 
- Written in node.js. 
- Client works with both the stateful and stateless servers (service modes).  It specifies the service mode as part of the request.
- Client supports retry on disconnections.

### Requirements

- Node.js > v14.x
- npm > 6.14.x

##  Building and running the client

    $ git clone https://github.com/ramdhany/realtimeproto
    $ cd realtimeproto/src
    $ cd client
    $ npm install


To See running options:
    

    $ node src/main.js -h          
    Number Sequence Node JS Client

    Gets and processes a stream of numbers from the NumberGeneratorSequence service 

    Options

    -h, --help                  Display usage guide.                           
    -s, ----server host:port    Number sequence generator service host:port          
    -m, --mode number           Service mode: 0 - STATELESS, 1 - STATEFUL      
    -c, --count number          Total count of numbers to receive. Must be > 0 
    -v, --verbose               Log level                                
    

To run the client with a **stateless** server (mode: 0) running on port 5000 and receive `10` numbers: 

    $ node src/main.js -v -s localhost:5000 -m 0 -c 10



To run the client with a **stateful** server (mode: 1) running on port 5000 and receive `10` numbers: 

    $ node src/main.js -v -s localhost:5000 -m 1 -c 10

Note: A checksum is returned

## Server: Number Sequence Generator Service 
Written in Java as a single service that supports both stateful and stateless modes.
Server runs on port 5000 (specifying a port number as a command-line parameter isn't supported yet)


In `STATELESS` mode, the server generates a random integer as the first number and generates an infinite sequence by doubling the number each time. The server stops servcing the request when the client ends the request or disconnects (the gRPC channel).


In `STATEFUL` mode, when the server receives a request from a new client, it uses a Pseudo Random Number Generator (`org.apache.commons.math3.random.RandomDataGenerator`) to generate `n` random integers (as requested by client). Random number generation is done on a seperate thread than the thread serviving the client request. 
Random numbers are stored in a `ClientState` object which includes (dummy) operations to persist the entire state to a data store. 

Note: 
- If a client issues a request to the stateful server after reconnection. the server needs to load all the client's state from the datastore. **This is not implemented.**
- The client state must ne deleted after a client has disconnected from the server for more than 30s.  **This is not implemented.**






### Requirements
- Maven 3.8.4
- Java 8 or higher (JDK 1.8)

### Running and builing the server

#### a. EITHER Import all dependencies and build from source code:

    $ cd <YOUR_WORKSPACE_FOLDER>/realtimeproto/src
    $ cd server
    $ mvn package


Run the server using the built JAR file 

    $ cd target
    $ java -jar numberseqgenerator-0.0.1-jar-with-dependencies.jar

Note: There is no command-line parameter for specifying a server port yet. Server runs on port 5000.

To change the port number, see the `main()` method in `NumSequenceServer.java`.

#### b. OR Build and run using docker

Note: the docker container crashes on start due to a JRE crash issue. This requires fixing.

    # build docker image
    $ docker build -t numsequenceservice:latest .   
    # run container 
    $ docker run -p 5000:5000 numsequenceservice:latest


## TODOs and  Unimplemented features
1. Fix number overflow issue in stateless server.
  a. When a long sequence of numbers is requested from stateless server, an integer flow happens. To check if overflow is due to number type used in server or in protocol buffer message.
2. Fix stateful server error: when server thread blocks, client retries. This causes a new (an empty) ClientState object to be created at the server (stateful impl.)
3. Fix docker container crash on start issue 
4. If a client issues a request to the stateful server after reconnection. the server needs to load all the client's state from the datastore. **This is not implemented.**
5. The client state must ne deleted after a client has disconnected from the server for more than 30s.  **This is not implemented.**