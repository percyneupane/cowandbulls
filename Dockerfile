FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY src ./src
COPY web ./web

RUN javac src/*.java

ENV PORT=8080

EXPOSE 8080

CMD ["sh", "-c", "java -cp src WebAppServer ${PORT}"]
