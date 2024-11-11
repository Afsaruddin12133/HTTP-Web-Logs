import java.io.*;
import java.net.*;
import java.util.*;
import org.json.JSONObject;
import org.json.JSONArray;

public class SimpleHTTPServer {
    private static final int PORT = 8080;
    private List<RequestLog> requestLogs = new ArrayList<>();

    public static void main(String[] args) {
        SimpleHTTPServer server = new SimpleHTTPServer();
        server.start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    handleRequest(clientSocket);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRequest(Socket clientSocket) throws IOException {
        long startTime = System.currentTimeMillis();
        InputStreamReader isr = new InputStreamReader(clientSocket.getInputStream());
        BufferedReader reader = new BufferedReader(isr);
        OutputStream outputStream = clientSocket.getOutputStream();

        String requestLine = reader.readLine();
        if (requestLine == null) return;

        String method = requestLine.split(" ")[0];
        String path = requestLine.split(" ")[1];
        String clientIP = clientSocket.getInetAddress().getHostAddress();

        long responseTime = System.currentTimeMillis() - startTime;

        // Log each request except /metrics
        if (!path.equals("/metrics")) {
            requestLogs.add(new RequestLog(method, responseTime, clientIP));
        }

        String corsHeader = "Access-Control-Allow-Origin: *\r\n";

        if (path.equals("/metrics")) {
            // Calculate metrics for response
            JSONObject jsonResponse = generateMetrics();
            String response = "HTTP/1.1 200 OK\r\n" +
                    corsHeader +
                    "Content-Type: application/json\r\n" +
                    "\r\n" +
                    jsonResponse.toString();
            outputStream.write(response.getBytes());
        } else {
            // Simulate different responses
            String responseMessage = "Request received with method: " + method;
            String response = "HTTP/1.1 200 OK\r\n" +
                    corsHeader +
                    "Content-Type: text/plain\r\n" +
                    "\r\n" +
                    responseMessage;
            outputStream.write(response.getBytes());
        }

        outputStream.flush();
    }

    private JSONObject generateMetrics() {
        int totalRequests = requestLogs.size();
        int getRequests = 0;
        int postRequests = 0;
        Map<String, Integer> ipCount = new HashMap<>();
        double totalResponseTime = 0;

        for (RequestLog log : requestLogs) {
            switch (log.method) {
                case "GET": getRequests++; break;
                case "POST": postRequests++; break;
            }
            ipCount.put(log.clientIP, ipCount.getOrDefault(log.clientIP, 0) + 1);
            totalResponseTime += log.responseTime;
        }

        double avgResponseTime = totalRequests > 0 ? totalResponseTime / totalRequests : 0;

        JSONObject json = new JSONObject();
        json.put("totalRequests", totalRequests);
        json.put("getRequests", getRequests);
        json.put("postRequests", postRequests);
        json.put("otherRequests", totalRequests - getRequests - postRequests);
        json.put("avgResponseTime", avgResponseTime);

        JSONArray requestLogArray = new JSONArray();
        for (RequestLog log : requestLogs) {
            requestLogArray.put(log.toJson());
        }
        json.put("requestLogs", requestLogArray);

        JSONObject ipCounts = new JSONObject();
        for (Map.Entry<String, Integer> entry : ipCount.entrySet()) {
            ipCounts.put(entry.getKey(), entry.getValue());
        }
        json.put("ipCount", ipCounts);

        return json;
    }

    // Inner class to store request data
    private static class RequestLog {
        String method;
        long responseTime;
        String clientIP;

        public RequestLog(String method, long responseTime, String clientIP) {
            this.method = method;
            this.responseTime = responseTime;
            this.clientIP = clientIP;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            json.put("method", method);
            json.put("responseTime", responseTime);
            json.put("clientIP", clientIP);
            return json;
        }
    }
}
