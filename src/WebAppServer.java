import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class WebAppServer {
    private static final int DEFAULT_PORT = 8080;
    private static final int MAX_GUESSES = 20;
    private static final Path WEB_ROOT = Path.of("web");
    private static final Map<String, GameSession> SESSIONS = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        int port = resolvePort(args);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new StaticFileHandler("index.html", "text/html; charset=utf-8"));
        server.createContext("/styles.css", new StaticFileHandler("styles.css", "text/css; charset=utf-8"));
        server.createContext("/app.js", new StaticFileHandler("app.js", "application/javascript; charset=utf-8"));
        server.createContext("/api/state", exchange -> handleState(exchange, false));
        server.createContext("/api/start", exchange -> handleState(exchange, true));
        server.createContext("/api/guess", new GuessHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("Web app listening on http://localhost:" + port);
    }

    private static int resolvePort(String[] args) {
        if (args.length > 0) {
            return Integer.parseInt(args[0]);
        }

        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isBlank()) {
            return Integer.parseInt(envPort);
        }

        return DEFAULT_PORT;
    }

    private static void handleState(HttpExchange exchange, boolean restart) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) && !"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, "GET, POST");
            return;
        }

        GameSession session = getOrCreateSession(exchange);
        if (restart) {
            session.reset();
        }
        sendJson(exchange, 200, session.toJson());
    }

    private static GameSession getOrCreateSession(HttpExchange exchange) {
        String sessionId = readCookie(exchange.getRequestHeaders(), "SESSION_ID");
        GameSession session = null;

        if (sessionId != null) {
            session = SESSIONS.get(sessionId);
        }

        if (session == null) {
            sessionId = UUID.randomUUID().toString();
            session = new GameSession();
            SESSIONS.put(sessionId, session);
            exchange.getResponseHeaders().add("Set-Cookie", "SESSION_ID=" + sessionId + "; Path=/; HttpOnly; SameSite=Lax");
        }

        return session;
    }

    private static String readCookie(Headers headers, String name) {
        List<String> cookieHeaders = headers.get("Cookie");
        if (cookieHeaders == null) {
            return null;
        }

        for (String cookieHeader : cookieHeaders) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                String trimmed = cookie.trim();
                if (trimmed.startsWith(name + "=")) {
                    return trimmed.substring(name.length() + 1);
                }
            }
        }

        return null;
    }

    private static void sendMethodNotAllowed(HttpExchange exchange, String allowedMethods) throws IOException {
        exchange.getResponseHeaders().set("Allow", allowedMethods);
        sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }

    private static void sendText(HttpExchange exchange, int statusCode, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String extractGuess(String requestBody) {
        String marker = "\"guess\"";
        int keyIndex = requestBody.indexOf(marker);
        if (keyIndex < 0) {
            return "";
        }

        int colonIndex = requestBody.indexOf(':', keyIndex);
        int firstQuoteIndex = requestBody.indexOf('"', colonIndex + 1);
        int secondQuoteIndex = requestBody.indexOf('"', firstQuoteIndex + 1);
        if (colonIndex < 0 || firstQuoteIndex < 0 || secondQuoteIndex < 0) {
            return "";
        }

        return requestBody.substring(firstQuoteIndex + 1, secondQuoteIndex).trim();
    }

    private static String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static class StaticFileHandler implements HttpHandler {
        private final String fileName;
        private final String contentType;

        private StaticFileHandler(String fileName, String contentType) {
            this.fileName = fileName;
            this.contentType = contentType;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange, "GET");
                return;
            }

            Path path = WEB_ROOT.resolve(fileName);
            if (!Files.exists(path)) {
                sendText(exchange, 404, "text/plain; charset=utf-8", "Not found");
                return;
            }

            sendText(exchange, 200, contentType, Files.readString(path, StandardCharsets.UTF_8));
        }
    }

    private static class GuessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange, "POST");
                return;
            }

            GameSession session = getOrCreateSession(exchange);
            String guess = extractGuess(readRequestBody(exchange));
            GuessResponse response = session.submitGuess(guess);

            if (response.errorMessage != null) {
                sendJson(exchange, 400, "{\"error\":\"" + escapeJson(response.errorMessage) + "\"}");
                return;
            }

            sendJson(exchange, 200, response.sessionJson);
        }
    }

    private static class GuessResponse {
        private final String errorMessage;
        private final String sessionJson;

        private GuessResponse(String errorMessage, String sessionJson) {
            this.errorMessage = errorMessage;
            this.sessionJson = sessionJson;
        }
    }

    private static class GameSession {
        private final List<GuessEntry> history = new ArrayList<>();
        private String secretCode;
        private boolean won;
        private boolean finished;

        private GameSession() {
            reset();
        }

        private synchronized void reset() {
            history.clear();
            won = false;
            finished = false;
            secretCode = generateCode();
        }

        private synchronized GuessResponse submitGuess(String guess) {
            if (finished) {
                return new GuessResponse(null, toJson());
            }

            String validationError = validateGuess(guess);
            if (validationError != null) {
                return new GuessResponse(validationError, null);
            }

            String result = scoreGuess(guess, secretCode);
            history.add(new GuessEntry(guess, result));

            if ("BBBBB".equals(result)) {
                won = true;
                finished = true;
            } else if (history.size() >= MAX_GUESSES) {
                finished = true;
            }

            return new GuessResponse(null, toJson());
        }

        private synchronized String toJson() {
            StringBuilder historyJson = new StringBuilder("[");
            for (int i = 0; i < history.size(); i++) {
                GuessEntry entry = history.get(i);
                if (i > 0) {
                    historyJson.append(",");
                }
                historyJson.append("{\"guess\":\"")
                        .append(escapeJson(entry.guess))
                        .append("\",\"result\":\"")
                        .append(escapeJson(entry.result))
                        .append("\"}");
            }
            historyJson.append("]");

            int guessesMade = history.size();
            int guessesRemaining = Math.max(0, MAX_GUESSES - guessesMade);
            String revealCode = finished && !won ? secretCode : "";

            return "{"
                    + "\"guessesMade\":" + guessesMade + ","
                    + "\"guessesRemaining\":" + guessesRemaining + ","
                    + "\"won\":" + won + ","
                    + "\"finished\":" + finished + ","
                    + "\"revealCode\":\"" + escapeJson(revealCode) + "\","
                    + "\"history\":" + historyJson
                    + "}";
        }

        private String generateCode() {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                builder.append((int) (Math.random() * 10));
            }
            return builder.toString();
        }

        private String validateGuess(String guess) {
            if (guess == null || guess.length() != 5) {
                return "Enter exactly 5 digits.";
            }

            for (int i = 0; i < guess.length(); i++) {
                char c = guess.charAt(i);
                if (c < '0' || c > '9') {
                    return "Use digits 0 through 9 only.";
                }
            }

            return null;
        }

        private String scoreGuess(String guess, String code) {
            int bulls = 0;
            int cows = 0;
            int[] freq = new int[10];

            for (int i = 0; i < 5; i++) {
                char codeChar = code.charAt(i);
                char guessChar = guess.charAt(i);
                if (codeChar == guessChar) {
                    bulls++;
                } else {
                    freq[codeChar - '0']++;
                }
            }

            for (int i = 0; i < 5; i++) {
                char codeChar = code.charAt(i);
                char guessChar = guess.charAt(i);
                if (codeChar != guessChar) {
                    int digit = guessChar - '0';
                    if (freq[digit] > 0) {
                        cows++;
                        freq[digit]--;
                    }
                }
            }

            StringBuilder result = new StringBuilder();
            for (int i = 0; i < bulls; i++) {
                result.append("B");
            }
            for (int i = 0; i < cows; i++) {
                result.append("C");
            }
            while (result.length() < 5) {
                result.append(" ");
            }
            return result.toString();
        }
    }

    private static class GuessEntry {
        private final String guess;
        private final String result;

        private GuessEntry(String guess, String result) {
            this.guess = guess;
            this.result = result;
        }
    }
}
