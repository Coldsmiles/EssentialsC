package cn.infstar.essentialsC.skinbridge;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.entity.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class MineSkinGateway implements SkinBridgeGateway {

    private static final String TEXTURES_PROPERTY = "textures";
    private static final String QUEUE_PATH = "/v2/queue";
    private static final long POLL_INTERVAL_MILLIS = 1000L;

    private final HttpClient httpClient;
    private final URI queueUri;
    private final String apiKey;
    private final String visibility;
    private final int timeoutSeconds;
    private final String userAgent;

    public MineSkinGateway(HttpClient httpClient, String endpoint, String apiKey, String visibility, int timeoutSeconds, String userAgent) {
        this.httpClient = httpClient;
        this.queueUri = URI.create(normalizeEndpoint(endpoint) + QUEUE_PATH);
        this.apiKey = apiKey;
        this.visibility = normalizeVisibility(visibility);
        this.timeoutSeconds = timeoutSeconds;
        this.userAgent = userAgent;
    }

    @Override
    public GeneratedSkin generateSkin(String skinUrl, SkinModel model) throws Exception {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("url", skinUrl);
        requestBody.addProperty("variant", model == SkinModel.SLIM ? "slim" : "classic");
        requestBody.addProperty("visibility", visibility);

        HttpResponse<String> response = send(HttpRequest.newBuilder(queueUri)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), StandardCharsets.UTF_8))
            .header("Content-Type", "application/json"));
        JsonObject body = parseResponse(response);
        if (response.statusCode() == 200) {
            return parseGeneratedSkin(body);
        }
        if (response.statusCode() != 202) {
            throw apiError("提交 MineSkin 生成请求", response, body);
        }

        JsonObject job = object(body, "job");
        String jobId = string(job, "id");
        return pollJob(jobId);
    }

    @Override
    public void applySkin(Player player, GeneratedSkin skin) {
        PlayerProfile profile = player.getPlayerProfile();
        profile.removeProperty(TEXTURES_PROPERTY);
        profile.setProperty(new ProfileProperty(TEXTURES_PROPERTY, skin.value(), skin.signature()));
        player.setPlayerProfile(profile);
    }

    private GeneratedSkin pollJob(String jobId) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(timeoutSeconds).toNanos();
        URI jobUri = URI.create(queueUri + "/" + jobId);
        while (System.nanoTime() < deadline) {
            HttpResponse<String> response = send(HttpRequest.newBuilder(jobUri).GET());
            JsonObject body = parseResponse(response);
            if (response.statusCode() != 200) {
                throw apiError("查询 MineSkin 生成任务", response, body);
            }

            JsonObject job = object(body, "job");
            String status = string(job, "status");
            if ("completed".equalsIgnoreCase(status)) {
                return parseGeneratedSkin(body);
            }
            if ("failed".equalsIgnoreCase(status)) {
                throw new IllegalStateException("MineSkin 生成任务失败: " + errorMessage(body));
            }
            if (!"waiting".equalsIgnoreCase(status) && !"active".equalsIgnoreCase(status)) {
                throw new IllegalStateException("MineSkin 返回未知任务状态: " + status);
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        throw new IllegalStateException("MineSkin 生成任务超时，请增大 skin-bridge.mineskin.request-timeout-seconds。");
    }

    private HttpResponse<String> send(HttpRequest.Builder builder) throws Exception {
        HttpRequest request = builder
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("Accept", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .header("User-Agent", userAgent)
            .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private JsonObject parseResponse(HttpResponse<String> response) {
        if (response.body() == null || response.body().isBlank()) {
            throw new IllegalStateException("MineSkin 返回空响应，HTTP " + response.statusCode());
        }
        if (response.body().length() > 1_048_576) {
            throw new IllegalStateException("MineSkin 响应超过 1 MiB 限制。");
        }
        try {
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IllegalStateException("MineSkin 返回了无效 JSON，HTTP " + response.statusCode(), exception);
        }
    }

    private GeneratedSkin parseGeneratedSkin(JsonObject body) {
        JsonObject skin = object(body, "skin");
        JsonObject texture = object(skin, "texture");
        JsonObject data = object(texture, "data");
        return new GeneratedSkin(string(data, "value"), string(data, "signature"));
    }

    private IllegalStateException apiError(String action, HttpResponse<String> response, JsonObject body) {
        return new IllegalStateException(action + "失败，HTTP " + response.statusCode() + ": " + errorMessage(body));
    }

    private String errorMessage(JsonObject body) {
        for (String key : new String[]{"message", "error", "code"}) {
            if (body.has(key) && body.get(key).isJsonPrimitive()) {
                return body.get(key).getAsString();
            }
        }
        return "未提供错误信息";
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (!parent.has(key) || !parent.get(key).isJsonObject()) {
            throw new IllegalStateException("MineSkin 响应缺少对象字段: " + key);
        }
        return parent.getAsJsonObject(key);
    }

    private static String string(JsonObject parent, String key) {
        if (!parent.has(key) || !parent.get(key).isJsonPrimitive()) {
            throw new IllegalStateException("MineSkin 响应缺少字符串字段: " + key);
        }
        String value = parent.get(key).getAsString();
        if (value.isBlank()) {
            throw new IllegalStateException("MineSkin 响应字符串字段为空: " + key);
        }
        return value;
    }

    private static String normalizeEndpoint(String endpoint) {
        String normalized = endpoint == null ? "" : endpoint.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("MineSkin API 地址不能为空。");
        }
        URI uri = URI.create(normalized);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("MineSkin API 必须使用 HTTPS。");
        }
        return normalized;
    }

    private static String normalizeVisibility(String visibility) {
        String normalized = visibility == null ? "" : visibility.trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalized.equals("public") && !normalized.equals("unlisted") && !normalized.equals("private")) {
            throw new IllegalArgumentException("MineSkin visibility 必须是 public、unlisted 或 private。");
        }
        return normalized;
    }
}
