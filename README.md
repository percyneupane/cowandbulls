# Bulls and Cows Online Hosting

This project is a Java socket game with two separate programs:

- `gameDaemon`: the server
- `BullsandCows`: the terminal client

## What changed for hosting

The server now reads its port from:

1. the first command-line argument
2. the `PORT` environment variable
3. fallback: `42212`

The client can connect to a custom host and port:

```bash
javac src/*.java
java -cp src BullsandCows <host> <port>
```

Example:

```bash
java -cp src BullsandCows your-server-hostname 42212
```

If no values are provided, the client still uses `localhost 42212`.

## Run locally

Start the server:

```bash
javac src/*.java
java -cp src gameDaemon
```

Start the client in another terminal:

```bash
java -cp src BullsandCows
```

## Deploy online

This is not a web app. It exposes a raw TCP socket, so the hosting platform must support public TCP services.

One practical option is a container-based host that supports TCP services. This repository now includes a `Dockerfile`, so the server can be deployed as a container.

After deployment, connect with:

```bash
java -cp src BullsandCows <public-hostname> <public-port>
```
