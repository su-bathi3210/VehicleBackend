package com.VehicleMIS.VehicleMIS.Service;

import com.VehicleMIS.VehicleMIS.Config.NotifyConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    private final NotifyConfig notifyConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SmsService(NotifyConfig notifyConfig) {
        this.notifyConfig = notifyConfig;
    }

    @PostConstruct
    public void validateConfig() {
        String user = notifyConfig.getUserId();
        String api = notifyConfig.getApiKey();
        String sender = notifyConfig.getSenderId();

        String maskedUser = user == null ? "null" : (user.length() > 4 ? user.substring(0, 2) + "****" : "****");
        logger.info("📦 Notify.lk config loaded -> userId={}, senderId={}", maskedUser, sender);

        if (user == null || api == null || sender == null) {
            logger.warn("⚠️ Notify.lk credentials incomplete. SMS requests will fail until config is provided.");
        }
    }

    public boolean sendSms(String to, String message, boolean unicode) {
        if (notifyConfig.getUserId() == null || notifyConfig.getApiKey() == null || notifyConfig.getSenderId() == null) {
            logger.error("❌ SMS aborted: Notify.lk credentials missing.");
            return false;
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://app.notify.lk/api/v1/send")
                    .queryParam("user_id", notifyConfig.getUserId())
                    .queryParam("api_key", notifyConfig.getApiKey())
                    .queryParam("sender_id", notifyConfig.getSenderId())
                    .queryParam("to", to)
                    .queryParam("message", message);

            if (unicode) {
                builder.queryParam("type", "unicode");
            }

            String url = builder.toUriString();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String body = response.getBody();

            if (body == null) {
                logger.warn("⚠️ Empty SMS response for {}", to);
                return false;
            }

            JsonNode node = objectMapper.readTree(body);
            String status = node.path("status").asText();

            if ("success".equalsIgnoreCase(status)) {
                logger.info("✅ SMS sent to {} | Preview: {}", to, preview(message));
                return true;
            } else {
                logger.warn("⚠️ SMS failed to {} | status={} | body={}", to, status, body);
                return false;
            }

        } catch (Exception e) {
            logger.error("❌ Error sending SMS to {}: {}", to, e.getMessage());
            return false;
        }
    }

    public boolean sendRequestConfirmationSms(String to, String requestId) {
        String message = "✅ Your vehicle request (" + requestId +
                ") has been submitted successfully and is pending approval.";
        return sendSms(to, message, true);
    }

    public boolean sendDriverAssignmentSms(String to, String requestId, String vehicleNumber, String model) {
        String message = "✅ You have been assigned to request " + requestId +
                ". Vehicle: " + vehicleNumber + " (" + model + "). Please contact the admin if needed.";
        return sendSms(to, message, true);
    }

    public boolean sendLicenseExpirySms(String to, String vehicleNumber, String expiryDate, long daysLeft) {
        String message;
        if (daysLeft >= 0) {
            message = "⚠️ Reminder: Vehicle " + vehicleNumber +
                    " license will expire in " + daysLeft + " day(s) (" + expiryDate + "). Please renew on time.";
        } else {
            message = "❌ ALERT: Vehicle " + vehicleNumber +
                    " license expired on " + expiryDate +
                    ". Vehicle is now blocked until renewal.";
        }
        return sendSms(to, message, true);
    }

    private String preview(String msg) {
        if (msg == null) return "";
        return msg.length() <= 80 ? msg : msg.substring(0, 77) + "...";
    }
}
