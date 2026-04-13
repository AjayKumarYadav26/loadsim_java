package com.ktlo.simulator.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for API documentation and root endpoint.
 */
@Controller
public class ApiDocController {

    @GetMapping("/")
    public String redirectToPortal() {
        return "redirect:/portal";
    }

    @ResponseBody
    @GetMapping(value = "/api-docs", produces = MediaType.TEXT_HTML_VALUE)
    public String home() {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<title>KTLO Simulator - API Documentation</title>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }" +
                "h1 { color: #333; }" +
                "h2 { color: #555; margin-top: 30px; border-bottom: 2px solid #ddd; padding-bottom: 10px; }" +
                "table { width: 100%; border-collapse: collapse; background: white; margin: 20px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
                "th { background: #4CAF50; color: white; padding: 12px; text-align: left; }" +
                "td { padding: 12px; border-bottom: 1px solid #ddd; }" +
                "tr:hover { background: #f9f9f9; }" +
                ".method { font-weight: bold; padding: 4px 8px; border-radius: 3px; font-size: 12px; }" +
                ".get { background: #61affe; color: white; }" +
                ".post { background: #49cc90; color: white; }" +
                ".endpoint { font-family: monospace; color: #333; }" +
                ".info { background: #e3f2fd; padding: 15px; border-left: 4px solid #2196F3; margin: 20px 0; }" +
                ".swagger-link { background: #ff6b6b; color: white; padding: 15px 30px; text-decoration: none; border-radius: 5px; display: inline-block; margin: 20px 0; font-weight: bold; }" +
                ".swagger-link:hover { background: #ff5252; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<h1>🚀 KTLO Simulator - API Documentation</h1>" +
                "<div class='info'>" +
                "<strong>Application Status:</strong> Running on port 8090<br>" +
                "<strong>Version:</strong> 1.0.0<br>" +
                "<strong>Tech Stack:</strong> Java 11, Spring Boot 2.7.18, PostgreSQL<br><br>" +
                "<a href='/swagger-ui/index.html' class='swagger-link'>📝 Interactive Swagger UI Documentation →</a>" +
                "</div>" +

                "<h2>💻 CPU Load Simulation (5 endpoints)</h2>" +
                "<table>" +
                "<tr><th>Method</th><th>Endpoint</th><th>Description</th></tr>" +
                "<tr><td><span class='method post'>POST</span></td><td class='endpoint'>/api/cpu/exhaust?taskCount=25&duration=60</td><td>Exhaust threadpool with tasks</td></tr>" +
                "<tr><td><span class='method post'>POST</span></td><td class='endpoint'>/api/cpu/intensive?duration=10</td><td>Single CPU-intensive task</td></tr>" +
                "<tr><td><span class='method post'>POST</span></td><td class='endpoint'>/api/cpu/fibonacci/{n}</td><td>Calculate Fibonacci number</td></tr>" +
                "<tr><td><span class='method post'>POST</span></td><td class='endpoint'>/api/cpu/primes?limit=100000</td><td>Find prime numbers</td></tr>" +
                "<tr><td><span class='method get'>GET</span></td><td class='endpoint'>/api/cpu/status</td><td>Get threadpool status</td></tr>" +
                "</table>" +

                "<h2>🗄️ Database Failure Simulation (7 endpoints)</h2>" +
                "<table>" +
                "<tr><th>Method</th><th>Endpoint</th><th>Description</th></tr>" +
                "<tr><td><span class='method post'>POST</span></td><td class='endpoint'>/api/db/timeout</td><td>Trigger DB timeout</td></tr>" +
                "<tr><td><span class='method post'>POST</span></td><td class='endpoint'>/api/db/slow-query?delay=5</td><td>Execute slow query</td></tr>" +
                "<tr><td><span class='method post'>POST</span></td><td class='endpoint'>/api/db/schema-mismatch</td><td>Trigger schema error</td></tr>" +
                "<tr><td><span class='method post'>POST</span></td><td class='endpoint'>/api/db/connection-failure</td><td>Connection failure</td></tr>" +
                "<tr><td><span class='method post'>POST</span></td><td class='endpoint'>/api/db/auth-failure</td><td>Authentication error</td></tr>" +
                "<tr><td><span class='method get'>GET</span></td><td class='endpoint'>/api/db/test-connection</td><td>Test DB connectivity</td></tr>" +
                "<tr><td><span class='method get'>GET</span></td><td class='endpoint'>/api/db/pool-stats</td><td>Get connection pool stats</td></tr>" +
                "</table>" +

                "<h2>❤️ Health & Monitoring (5 endpoints)</h2>" +
                "<table>" +
                "<tr><th>Method</th><th>Endpoint</th><th>Description</th></tr>" +
                "<tr><td><span class='method get'>GET</span></td><td class='endpoint'>/api/health</td><td>Overall health status</td></tr>" +
                "<tr><td><span class='method get'>GET</span></td><td class='endpoint'>/api/health/threadpool</td><td>Threadpool metrics</td></tr>" +
                "<tr><td><span class='method get'>GET</span></td><td class='endpoint'>/api/health/database</td><td>Database health</td></tr>" +
                "<tr><td><span class='method get'>GET</span></td><td class='endpoint'>/actuator/health</td><td>Spring Actuator health</td></tr>" +
                "<tr><td><span class='method get'>GET</span></td><td class='endpoint'>/actuator/metrics</td><td>Application metrics</td></tr>" +
                "</table>" +

                "<h2>📊 JMX Monitoring</h2>" +
                "<div class='info'>" +
                "<strong>JMX Port:</strong> 9010<br>" +
                "<strong>Connect:</strong> jconsole localhost:9010<br>" +
                "<strong>MBeans Available:</strong><br>" +
                "• com.ktlo.simulator:type=Application,name=KtloSimulator<br>" +
                "• com.ktlo.simulator:type=ThreadPool,name=AsyncExecutor<br>" +
                "• com.ktlo.simulator:type=Database,name=HikariCP" +
                "</div>" +

                "<h2>📝 Quick Examples</h2>" +
                "<pre style='background: #2d2d2d; color: #f8f8f2; padding: 15px; border-radius: 5px; overflow-x: auto;'>" +
                "# Exhaust threadpool\n" +
                "curl -X POST \"http://localhost:8090/api/cpu/exhaust?taskCount=25&duration=60\"\n\n" +
                "# Check threadpool status\n" +
                "curl http://localhost:8090/api/cpu/status\n\n" +
                "# Check overall health\n" +
                "curl http://localhost:8090/api/health\n\n" +
                "# Trigger database timeout\n" +
                "curl -X POST http://localhost:8090/api/db/timeout" +
                "</pre>" +

                "<div style='margin-top: 40px; padding: 20px; background: white; border-radius: 5px;'>" +
                "<strong>📚 Documentation:</strong> See README.md for complete documentation<br>" +
                "<strong>🔧 Configuration:</strong> Port 8090, Threadpool: 20 max threads, Database: Azure PostgreSQL<br>" +
                "<strong>📦 Version:</strong> 1.0.0" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
