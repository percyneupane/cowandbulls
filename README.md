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


