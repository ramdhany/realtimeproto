## Overview

The problem is to implement a client and server that implement a protocol of your own design that meets the requirements given below.

The high-level requirement is that a client establishes a connection to the server and receives a stream of messages whose primary payload is a number; the sequence of messages delivered to the client conforms to the description below. The aim of the protocol is to ensure that the client/server dialogue is correctly achieved, even when the client connection can be dropped from time to time.

Two variants of the functionality are specified below; the aim is to implement both.

### Stateless connections

In this case, upon receiving a new client connection, the server generates a random integer `a` in the range `1..0xff` and streams an indefinite sequence of numbers in discrete messages, each 1s apart. Each number in the sequence is 2x the previous number; so if for example `a = 23`, the sequence will be `23, 46, 92, ...`.

The client is started with an argument that specifies the total number `n` (maximum `0xffff`) of messages to receive. Its job is to connect and receive the complete sequence of `n` numbers, close the connection, and return the sum of the received numbers. 

The client must be capable of reconnecting and continuing to receive the sequence in the case that its connection drops; when reconnecting, the client must supply connection parameters that allow the server to continue the sequence starting with the first undelivered number. On receipt of the final message, the client must add the received numbers and log the result to stdout.

This variant can be implemented without requiring any per-client state to be maintained by the server; any state is on a per-connection basis only.

### Stateful connections

In this case the client, before connection, generates a random uuid client identifier string and generates a random integer `n` (max `0xffff`), and sends these as part of its connection request. For this client, the server randomly initialises a PRNG which generates a sequence of uint32 numbers. The server sends this sequence in a stream of discrete messages, 1s apart. The final messgae also contains a checksum that may be used by the client to verify that it has correctly received the entire sequence. The server closes the connection after sending the final message.

The client's job is to receive the sequence and, once all messages have been received, close the connection, calculate the checksum, and compare with the value supplied by the server. The client must be capable of reconnecting and continuing to receive the sequence in the case that its connection drops; when reconnecting, the client must supply connection parameters that allow the server to continue the sequence starting with the first undelivered number. The final checksum(s), and an indication of success or failure, is output to stdout.

The complete sequence of numbers delvered by the server must be the same irrespective of disconnections by the client. The server must maintain session state for each client (ie state for each client id) in order to implement this functionality.

The server must maintain session state for up to 30s during periods of disconnection. If a client fails to reconnect within 30s of a disconnection then the server discards the state, and any subsequent reconnection attempt for that client id must be rejected with a suitable error. After sending the final message for a given client id the session state must be discarded immediately.

An abstraction must be defined representing the interface betweem the server and a session state store, and an implementation of an in-memory store (ie for use by a single server instance) must be provided. However, the interface must be capable of implementation as a shared session store (eg using Redis) which could be shared by multiple server instances (so reconnections can succeed if made to a different server from the original connection).

## Protocol

You are free to design and implement the protocol in any way you choose; the requirement is simply that:

- the communication occurs over a single TCP connection unless the connection drops;
- the timing of successive messages is under control of the server; ie information may be sent by the client as part of initiating a connection, but there is no per-message client request.

Clients must reconnect automatically in the case that the connection drops, and implement a strategy to ensure that it does not busy-loop if connection attempts immediately fail.

Acceptable solutions would be to implement something using gRPC (https://grpc.io/), SSE (https://www.w3.org/TR/eventsource/) or something implemented directly using TCP.

You must write a description/specification of the protocol which contains an explanation of:

- how the client knows when the stream is complete;
- how the client resumes correctly from a dropped connection;
- how the client can validate the result (in the case of the stateful variant);

In the case of the stateful connection variant, you must write a specification for the session state and associated API which:
- describes the PRNG used, and reconnection parameters;
- ensures that the stored session state is the minimum necessary to achieve the stated functionality; your description must include an explanation of why this is the minimum required state;
- defines the checksum used.

Both specifications must be clear enough that a client could be written, without looking at your code, that interoperates with your server. In the case of the stateful connection variant, the specification must be sufficient that independent interoperable implementations could be created; that is, a client reconnecting with the same parameters to independent implementations sharing a common session store would receive the same sequence of numbers.

Any assumptions made about the underlying network/transport must be documented. If there are limits arising from the protocol design (eg max number of concurrent connections, max number of messages) then these should be stated.

## Server requirements

The server must listen on the localhost interface using a port specified as a commandline argument.

The server must be capable of handling multiple clients concurrently.

Stateless and stateful variants may have a common implementation or be separate implementations. In the case of a common implementation, the server may operate in stateless or stateful mode based either on a init-time argument or the mode may be inferred from the client-supplied connection parameters.

## Client requirements

The client must connect to the server via the port specified as a commandline argument. The result (ie the sum/checksum of the received numbers) must be written to  stdout.

Stateless and stateful variants may have a common implementation or be separate implementations. In the case of a common implementation, the client should operate in stateless or stateful mode based on a init-time argument.

## Test requirements

Some test mechanism must be provided with a clear pass/fail outcome; it is allowed for this to use both the client and server (ie it is not necessary to write any independent mock client or server). In order to support the testing it is allowable to add support for additional commandline arguments or a programmatic interface, for client and/or server.

## Implementation requirements

The client and server must be implemented in different languages; nodejs and go are preferred.

You may use libraries and frameworks as you see fit, including libraries for the frameworks named above, but you may not use an framework for a higher-level protocol that provides a solution for the problem at hand.

#### Documentation

A README must be included that states how to build, run, and test.

#### Delivery

The result should be made available as a repo on github or similar.