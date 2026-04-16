# baby-redis

A from-scratch implementation of a Redis-inspired in-memory key-value store, 
built in Java as a deep dive into network protocols, data structures, and 
systems programming.

## Status

🚧 **In active development.** Core functionality works; ongoing work on persistence.

## Features

- TCP server accepting concurrent client connections
- Custom wire protocol for client-server communication
- Built-in CLI for development and testing
- Supported commands:
  - **Strings:** `GET`, `SET`, `DELETE`
  - **Sets:** `SADD`, `SREM`, `SISMEMBER`, `SMEMBERS`
  - **Expiry:** `EXPIRE`, `TTL`

## Project Structure

```
baby-redis/
├── server/    # The key-value store server
└── cli/       # Development CLI tool
```

See also: [baby-redis-client](https://github.com/mariusflores/baby-redis-client) 
— the standalone Java client library.

## Getting Started

### Prerequisites

- Java 21+
- Maven

### Building
```bash
git clone https://github.com/mariusflores/baby-redis.git
cd baby-redis
mvn clean package

```

### Running the Server

```bash
java -jar target/baby-redis.jar
```
The server listens on port `6379` by default.

### Running the CLI

```bash
mvn exec:java -Dexec.mainClass="io.babyredis.cli.BabyRedisCli"
```

Example session:

```
> SET greeting hello
OK
> GET greeting
"hello"
> SADD fruits apple banana cherry
(integer) 3
> SMEMBERS fruits
1) "apple"
2) "banana"
3) "cherry"
> EXPIRE greeting 60
(integer) 1
> TTL greeting
(integer) 58
> QUIT
```

