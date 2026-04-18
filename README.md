# baby-redis

![Build](https://github.com/mariusflores/baby-redis/actions/workflows/build.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-orange)
![License](https://img.shields.io/badge/License-MIT-blue)

A from-scratch implementation of a Redis-inspired in-memory key-value store,
built in Java as a deep dive into network protocols, data structures, and
systems programming.

## Status

🚧 **In active development.** Core functionality works; ongoing work on persistence.

## Features

- TCP server accepting concurrent client connections
- Custom wire protocol for client-server communication
- Supported commands:
    - **Strings:** `GET`, `SET`, `DELETE`
    - **Sets:** `SADD`, `SREM`, `SISMEMBER`, `SMEMBERS`
    - **Expiry:** `EXPIRE`, `TTL`

## Project Structure

```
baby-redis/
├── server/    # The key-value store server
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

## Roadmap

- [ ] Implement RESP-inspired wire protocol for typed responses
- [ ] Support additional data types (hashes, sorted sets)
- [ ] Connection pooling in the client
- [ ] Build personal tools on top of the ecosystem (expense tracker, dashboard)
