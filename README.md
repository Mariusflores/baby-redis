# baby-redis

![Build](https://github.com/mariusflores/baby-redis/actions/workflows/build.yml/badge.svg)
![Docker Pulls](https://img.shields.io/docker/pulls/mfloresdal/baby-redis)
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

The baby-redis ecosystem consists of three independent repositories:

- [baby-redis](https://github.com/mariusflores/baby-redis) — the server
- [baby-redis-client](https://github.com/mariusflores/baby-redis-client) — the Java client library
- [baby-redis-cli](https://github.com/mariusflores/baby-redis-cli) — the command-line interface

## Getting Started

### Prerequisites

- Java 21+
- Maven

## Building & Running the server locally

### Building

```bash
git clone https://github.com/mariusflores/baby-redis.git
cd baby-redis
mvn clean package

```

### Running the Server with Java

```bash
java -jar target/baby-redis.jar
```

### Running the Server with docker

```bash
docker build -t baby-redis .
docker run -p 6379:6379 baby-redis
```

### pull directly from Docker Hub:

```bash
docker pull mfloresdal/baby-redis
docker run -p 6379:6379 mfloresdal/baby-redis
```

The server listens on port `6379` by default.

## Roadmap

- [ ] Implement RESP-inspired wire protocol for typed
  responses [#2](https://github.com/Mariusflores/baby-redis/issues/2)
- [x] Add Logging framework [#3](https://github.com/Mariusflores/baby-redis/issues/3)
- [ ] Build personal tools on top of the ecosystem (expense tracker, dashboard)

## Related

[baby-redis-client](https://github.com/Mariusflores/baby-redis-client) — Java Library handling socket connections to
baby redis server  
[baby-redis-cli](https://github.com/Mariusflores/baby-redis-cli) — Devtool using this library to connect to server and
perform command line operations  
