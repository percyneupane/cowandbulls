# Bulls and Cows Web App

This project now includes a browser version of Bulls and Cows.

## Run locally

Compile the Java sources:

```bash
javac src/*.java
```

Start the web server:

```bash
java -cp src WebAppServer
```

Then open:

```text
http://localhost:8080
```

The server also supports a custom port:

```bash
java -cp src WebAppServer 9000
```

Or via environment variable:

```bash
PORT=9000 java -cp src WebAppServer
```

## Deploy online

This repository includes a `Dockerfile` for container-based deployment.

After deployment, the app serves the game UI from the browser and keeps one game session per browser using cookies.

## Legacy terminal version

The original socket-based server and terminal client are still present:

- `gameDaemon`
- `BullsandCows`
