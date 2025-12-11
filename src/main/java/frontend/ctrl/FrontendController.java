package frontend.ctrl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import frontend.data.Sms;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(path = "/sms")
public class FrontendController {

    private String modelHost;
    private RestTemplateBuilder rest;
    
    // Prometheus metrics
    private final Counter smsRequestsTotal;
    private final AtomicInteger queueSize = new AtomicInteger(0);
    private final Timer classificationDuration;

    public FrontendController(RestTemplateBuilder rest, Environment env, MeterRegistry registry) {
        this.rest = rest;
        this.modelHost = env.getProperty("MODEL_HOST");
        assertModelHost();
        
        // Initialize metrics
        this.smsRequestsTotal = Counter.builder("sms_requests_total")
            .description("Total SMS classification requests")
            .tag("endpoint", "/sms")
            .register(registry);
            
        Gauge.builder("sms_queue_size", queueSize, AtomicInteger::get)
            .description("Messages in processing queue")
            .tag("priority", "normal")
            .register(registry);
            
        this.classificationDuration = Timer.builder("sms_classification_duration_seconds")
            .description("Time to classify SMS")
            .tag("model_version", "v1")
            .register(registry);
    }

    private void assertModelHost() {
        if (modelHost == null || modelHost.strip().isEmpty()) {
            System.err.println("ERROR: ENV variable MODEL_HOST is null or empty");
            System.exit(1);
        }
        modelHost = modelHost.strip();
        if (modelHost.indexOf("://") == -1) {
            var m = "ERROR: ENV variable MODEL_HOST is missing protocol, like \"http://...\" (was: \"%s\")\n";
            System.err.printf(m, modelHost);
            System.exit(1);
        } else {
            System.out.printf("Working with MODEL_HOST=\"%s\"\n", modelHost);
        }
    }

    @GetMapping("")
    public String redirectToSlash(HttpServletRequest request) {
        // relative REST requests in JS will end up on / and not on /sms
        return "redirect:" + request.getRequestURI() + "/";
    }

    @GetMapping("/")
    public String index(Model m) {
        m.addAttribute("hostname", modelHost);
        return "sms/index";
    }

    @PostMapping({ "", "/" })
    @ResponseBody
    public Sms predict(@RequestBody Sms sms) {
        System.out.printf("Requesting prediction for \"%s\" ...\n", sms.sms);
        
        // Track metrics
        smsRequestsTotal.increment();
        queueSize.incrementAndGet();
        
        try {
            sms.result = classificationDuration.record(() -> getPrediction(sms));
            System.out.printf("Prediction: %s\n", sms.result);
            return sms;
        } finally {
            queueSize.decrementAndGet();
        }
    }

    private String getPrediction(Sms sms) {
        try {
            var url = new URI(modelHost + "/predict");
            var c = rest.build().postForEntity(url, sms, Sms.class);
            return c.getBody().result.trim();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}