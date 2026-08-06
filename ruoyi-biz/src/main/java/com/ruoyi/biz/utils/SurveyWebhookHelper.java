package com.ruoyi.biz.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.spring.SpringUtils;

/**
 * Fire-and-forget survey webhook POST with optional HMAC signature and retries.
 * Blocks private / link-local / metadata targets to reduce SSRF risk.
 */
public final class SurveyWebhookHelper
{
    private static final Logger log = LoggerFactory.getLogger(SurveyWebhookHelper.class);
    private static final int[] RETRY_DELAYS_MS = { 10, 1000, 5000, 15000 };

    private SurveyWebhookHelper()
    {
    }

    public static boolean isValidUrl(String url)
    {
        if (StringUtils.isEmpty(url))
        {
            return false;
        }
        String u = url.trim();
        if (!(u.startsWith("http://") || u.startsWith("https://")) || u.length() > 500)
        {
            return false;
        }
        try
        {
            URI uri = URI.create(u);
            String host = uri.getHost();
            if (StringUtils.isEmpty(host))
            {
                return false;
            }
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
            {
                return false;
            }
            if (isBlockedHost(host))
            {
                return false;
            }
            InetAddress[] addrs = InetAddress.getAllByName(host);
            for (InetAddress addr : addrs)
            {
                if (isBlockedAddress(addr))
                {
                    return false;
                }
            }
            return true;
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    private static boolean isBlockedHost(String host)
    {
        String h = host.trim().toLowerCase();
        if ("localhost".equals(h) || h.endsWith(".localhost") || h.endsWith(".local")
            || "metadata.google.internal".equals(h))
        {
            return true;
        }
        return false;
    }

    private static boolean isBlockedAddress(InetAddress addr)
    {
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress()
            || addr.isSiteLocalAddress() || addr.isMulticastAddress())
        {
            return true;
        }
        byte[] b = addr.getAddress();
        if (b.length == 4)
        {
            int a = b[0] & 0xff;
            int c = b[1] & 0xff;
            // 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16 already covered by isSiteLocalAddress
            // 169.254.0.0/16 link-local; 100.64.0.0/10 CGNAT; 0.0.0.0/8
            if (a == 0 || a == 127 || (a == 100 && c >= 64 && c <= 127) || a >= 224)
            {
                return true;
            }
            // AWS/GCP metadata
            if (a == 169 && c == 254)
            {
                return true;
            }
        }
        return false;
    }

    public static void dispatchAsync(String webhookUrl, String jsonBody)
    {
        dispatchAsync(webhookUrl, jsonBody, null);
    }

    public static void dispatchAsync(String webhookUrl, String jsonBody, String secret)
    {
        if (!isValidUrl(webhookUrl) || StringUtils.isEmpty(jsonBody))
        {
            return;
        }
        final String url = webhookUrl.trim();
        final String body = jsonBody;
        final String sec = secret;
        try
        {
            ScheduledExecutorService executor = SpringUtils.getBean("scheduledExecutorService");
            scheduleAttempt(executor, url, body, sec, 0);
        }
        catch (Exception ex)
        {
            try
            {
                postOnce(url, body, sec);
            }
            catch (Exception ignored)
            {
            }
        }
    }

    private static void scheduleAttempt(ScheduledExecutorService executor, String url, String body, String secret, int attempt)
    {
        long delay = RETRY_DELAYS_MS[Math.min(attempt, RETRY_DELAYS_MS.length - 1)];
        executor.schedule(() -> {
            try
            {
                // Re-validate before each attempt (DNS may change)
                if (!isValidUrl(url))
                {
                    log.warn("survey webhook blocked by SSRF guard: {}", url);
                    return;
                }
                postOnce(url, body, secret);
            }
            catch (Exception ex)
            {
                log.warn("survey webhook attempt {} failed: {} -> {}", attempt + 1, url, ex.getMessage());
                if (attempt + 1 < RETRY_DELAYS_MS.length)
                {
                    scheduleAttempt(executor, url, body, secret, attempt + 1);
                }
                else
                {
                    log.error("survey webhook gave up after {} attempts: {}", RETRY_DELAYS_MS.length, url);
                }
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private static void postOnce(String url, String body, String secret) throws Exception
    {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(15000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "TongChaYun-Webhook/1.0");
        if (StringUtils.isNotEmpty(secret))
        {
            conn.setRequestProperty("X-TongChaYun-Signature", "sha256=" + hmacSha256(body, secret));
            conn.setRequestProperty("X-TongChaYun-Timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream())
        {
            os.write(bytes);
        }
        int code = conn.getResponseCode();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
            code >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8)))
        {
            StringBuilder sb = new StringBuilder();
            String line;
            while (in != null && (line = in.readLine()) != null)
            {
                sb.append(line);
            }
            if (code < 200 || code >= 300)
            {
                throw new IllegalStateException("HTTP " + code + " " + sb);
            }
        }
    }

    private static String hmacSha256(String data, String secret) throws Exception
    {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(raw.length * 2);
        for (byte b : raw)
        {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
