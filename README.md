# baby-redis

![Build](https://github.com/mariusflores/baby-redis/actions/workflows/build.yml/badge.svg)
![Docker Pulls](https://img.shields.io/docker/pulls/mfloresdal/baby-redis)
![Java](https://img.shields.io/badge/Java-21-orange)
![License](https://img.shields.io/badge/License-MIT-blue)

A from-scratch implementation of a Redis-inspired in-memory key-value store,
built in Java as a deep dive into network protocols, data structures, and
systems programming.

## Recent updates
- **0.3.0**: Added FLUSHDB command with support for full and pattern-based (prefix*) key deletion. Enhanced KEYS command to support prefix-based pattern matching (KEYS prefix*).

- **0.2.0**: Implemented RESP-inspired wire protocol for typed client-server communication.

## Status

🚧 **In active development.** Core functionality and persistance works. Working on expanding method catalog

## Features

- TCP server accepting concurrent client connections
- RESP-inspired wire protocol for typed client-server communication
- Supported commands:
    - **Strings:** `GET`, `SET`, `DELETE`
    - **Sets:** `SADD`, `SREM`, `SISMEMBER`, `SMEMBERS`
    - **Expiry:** `EXPIRE`, `TTL`
    - **Key management:** `KEYS` (supports `*` and `prefix*` patterns), `FLUSHDB` (supports full and pattern-based flush)
    - **Test:** `PING`

## Project Structure

The baby-redis ecosystem consists of four independent repositories:

- [baby-redis](https://github.com/mariusflores/baby-redis) — the server
- [baby-redis-client](https://github.com/mariusflores/baby-redis-client) — the Java client library
- [baby-redis-cli](https://github.com/mariusflores/baby-redis-cli) — the command-line interface
- [baby-redis-protocol](https://github.com/mariusflores/baby-redis-protocol) — shared RESP protocol library

## Getting Started

### Prerequisites

- Java 21+
- Maven
- Docker (optional)
- [baby-redis-protocol](https://github.com/mariusflores/baby-redis-protocol) installed locally

### Building from source

```bash
git clone https://github.com/mariusflores/baby-redis.git
cd baby-redis
mvn clean package
```

### Running the Server

**With Java:**

```bash
java -jar target/baby-redis.jar
```

**With Docker (build locally):**

```bash
docker build -t baby-redis .
docker run -p 6379:6379 baby-redis
```

**With Docker (from Docker Hub):**

```bash
docker pull mfloresdal/baby-redis
docker run -p 6379:6379 mfloresdal/baby-redis
```

The server listens on port `6379` by default.

## Roadmap

- [x] Implement RESP-inspired wire protocol for typed
  responses [#2](https://github.com/Mariusflores/baby-redis/issues/2)
- [x] Add Logging framework [#3](https://github.com/Mariusflores/baby-redis/issues/3)
- [x] Add FLUSHDB command with pattern-based and full flush support
- [x] Enhance KEYS command to support prefix-based pattern matching
- [x] Build personal tools on top of the ecosystem (expense tracker, dashboard)
- [ ] Introduce KeyValueStore interface

## Related

- [baby-redis-client](https://github.com/Mariusflores/baby-redis-client) — Java Library handling socket connections to
  baby redis server
- [baby-redis-cli](https://github.com/Mariusflores/baby-redis-cli) — Devtool using this library to connect to server and
  perform command line operations
- [baby-redis-protocol](https://github.com/mariusflores/baby-redis-protocol) — shared RESP protocol library
- [expense-tracker](https://github.com/mariusflores/expense-tracker) — a personal tool to track economy and spending trends. Uses baby-redis for data storage.
- [energy-monitor](https://github.com/mariusflores/energy-monitor) — a personal tool to show energy-prices taken from public API. still early development. Uses baby-redis to cache energy-price data
  

