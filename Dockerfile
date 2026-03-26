FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY src ./src

RUN javac src/*.java

ENV PORT=42212

EXPOSE 42212

CMD ["sh", "-c", "java -cp src gameDaemon ${PORT}"]
