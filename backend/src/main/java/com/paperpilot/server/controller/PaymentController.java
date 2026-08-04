package com.paperpilot.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.entity.PaymentOrderEntity;
import com.paperpilot.server.entity.PaymentTicketEntity;
import com.paperpilot.server.entity.RechargeRecordEntity;
import com.paperpilot.server.repository.AppUserRepository;
import com.paperpilot.server.repository.PaymentOrderRepository;
import com.paperpilot.server.repository.PaymentTicketRepository;
import com.paperpilot.server.repository.RechargeRecordRepository;
import com.paperpilot.server.service.CurrentUserService;
import com.paperpilot.server.service.MembershipService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final CurrentUserService currentUserService;
    private final PaymentOrderRepository orderRepository;
    private final PaymentTicketRepository ticketRepository;
    private final AppUserRepository appUserRepository;
    private final RechargeRecordRepository rechargeRecordRepository;
    private final ObjectMapper objectMapper;
    private final MembershipService membershipService;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Map<String, X509Certificate> wechatPlatformCertificates = new ConcurrentHashMap<>();
    private volatile long wechatCertificateLoadedAt = 0L;
    private volatile String lastWechatCertApiError = null;

    @Value("${PAPERPILOT_ZHIFUFM_API_BASE_URL:${paperpilot.payment.zhifufm.api-base-url:}}")
    private String zhifuApiBaseUrl;

    @Value("${PAPERPILOT_ZHIFUFM_MERCHANT_NUM:${paperpilot.payment.zhifufm.merchant-num:}}")
    private String zhifuMerchantNum;

    @Value("${PAPERPILOT_ZHIFUFM_SECRET:${paperpilot.payment.zhifufm.secret:}}")
    private String zhifuSecret;

    @Value("${PAPERPILOT_PUBLIC_BASE_URL:${paperpilot.payment.zhifufm.public-base-url:}}")
    private String publicBaseUrl;

    @Value("${PAPERPILOT_ZHIFUFM_NOTIFY_URL:${paperpilot.payment.zhifufm.notify-url:}}")
    private String configuredNotifyUrl;

    @Value("${PAPERPILOT_ZHIFUFM_RETURN_URL:${paperpilot.payment.zhifufm.return-url:}}")
    private String configuredReturnUrl;

    @Value("${PAPERPILOT_ZHIFUFM_ALIPAY_PAY_TYPE:${paperpilot.payment.zhifufm.alipay-pay-type:alipay}}")
    private String alipayPayType;

    @Value("${PAPERPILOT_ZHIFUFM_WECHAT_PAY_TYPE:${paperpilot.payment.zhifufm.wechat-pay-type:wechat}}")
    private String wechatPayType;

    @Value("${PAPERPILOT_WECHAT_PAY_APP_ID:${paperpilot.payment.wechat.app-id:}}")
    private String wechatAppId;

    @Value("${PAPERPILOT_WECHAT_PAY_MCH_ID:${paperpilot.payment.wechat.mch-id:}}")
    private String wechatMchId;

    @Value("${PAPERPILOT_WECHAT_PAY_API_V3_KEY:${paperpilot.payment.wechat.api-v3-key:}}")
    private String wechatApiV3Key;

    @Value("${PAPERPILOT_WECHAT_PAY_SERIAL_NO:${paperpilot.payment.wechat.serial-no:}}")
    private String wechatSerialNo;

    @Value("${PAPERPILOT_WECHAT_PAY_PRIVATE_KEY:${paperpilot.payment.wechat.private-key:}}")
    private String wechatPrivateKeyPem;

    @Value("${PAPERPILOT_WECHAT_PAY_PRIVATE_KEY_PATH:${paperpilot.payment.wechat.private-key-path:}}")
    private String wechatPrivateKeyPath;

    @Value("${PAPERPILOT_WECHAT_PAY_NOTIFY_URL:${paperpilot.payment.wechat.notify-url:}}")
    private String wechatNotifyUrl;

    @Value("${PAPERPILOT_WECHAT_PAY_PUBLIC_KEY:${paperpilot.payment.wechat.public-key:}}")
    private String wechatPublicKeyPem;

    @Value("${PAPERPILOT_WECHAT_PAY_PUBLIC_KEY_PATH:${paperpilot.payment.wechat.public-key-path:}}")
    private String wechatPublicKeyPath;

    @Value("${PAPERPILOT_WECHAT_PAY_PUBLIC_KEY_ID:${paperpilot.payment.wechat.public-key-id:}}")
    private String wechatPublicKeyId;

    public PaymentController(
        CurrentUserService currentUserService,
        PaymentOrderRepository orderRepository,
        PaymentTicketRepository ticketRepository,
        AppUserRepository appUserRepository,
        RechargeRecordRepository rechargeRecordRepository,
        ObjectMapper objectMapper,
        MembershipService membershipService
    ) {
        this.currentUserService = currentUserService;
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.appUserRepository = appUserRepository;
        this.rechargeRecordRepository = rechargeRecordRepository;
        this.objectMapper = objectMapper;
        this.membershipService = membershipService;
    }

    @GetMapping("/orders")
    public Map<String, Object> listOrders() {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orders", orderRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId).stream().map(this::orderToMap).toList());
        result.put("tickets", ticketRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId).stream()
            .filter(this::isUsableTicket)
            .map(this::ticketToMap)
            .toList());
        return result;
    }

    @PostMapping("/orders")
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> body) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        String provider = String.valueOf(body.getOrDefault("provider", "")).trim().toLowerCase();
        String planId = String.valueOf(body.getOrDefault("planId", "custom-recharge")).trim();
        String planCycle = String.valueOf(body.getOrDefault("planCycle", "monthly")).trim();
        int quantity = 1;
        try {
            quantity = Integer.parseInt(String.valueOf(body.getOrDefault("quantity", body.getOrDefault("teamMemberCount", "1"))).trim());
        } catch (Exception ignored) {}
        double amount = Double.parseDouble(String.valueOf(body.getOrDefault("amount", "0")).replace("¥", "").trim());
        if (!"custom-recharge".equals(planId)) {
            amount = membershipService.price(planId, planCycle, quantity);
        }
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "充值金额必须大于 0");
        }
        if (!provider.equals("wechat")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前只支持微信支付");
        }
        String orderNo = "PP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + userId + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setProvider(provider);
        order.setPlanId(planId);
        order.setPlanCycle(planCycle);
        order.setAmount(normalizeMoney(amount).doubleValue());
        order.setStatus("created");
        order.setMessage("订单已创建，正在请求微信支付。");
        order = orderRepository.save(order);
        order = startWechatNativeOrder(order);
        return orderToMap(orderRepository.save(order));
    }

    @PostMapping(value = "/notify/wechat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> wechatNotify(
        @RequestHeader Map<String, String> headers,
        @RequestBody String rawBody
    ) {
        try {
            if (!wechatConfigured(true)) {
                return wechatCallbackResponse("FAIL", "微信支付未配置");
            }
            if (!verifyWechatNotify(headers, rawBody)) {
                return wechatCallbackResponse("FAIL", "签名校验失败");
            }
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode resource = root.path("resource");
            String plain = decryptWechatResource(
                resource.path("associated_data").asText(""),
                resource.path("nonce").asText(""),
                resource.path("ciphertext").asText("")
            );
            JsonNode payload = objectMapper.readTree(plain);
            if (!"SUCCESS".equals(payload.path("trade_state").asText(""))) {
                return wechatCallbackResponse("SUCCESS", "非成功交易已忽略");
            }
            String orderNo = payload.path("out_trade_no").asText("");
            PaymentOrderEntity order = orderRepository.findById(orderNo).orElse(null);
            if (order == null) {
                return wechatCallbackResponse("FAIL", "订单不存在");
            }
            int paidCents = payload.path("amount").path("payer_total").asInt(payload.path("amount").path("total").asInt(0));
            if (!sameWechatAmount(order.getAmount(), paidCents)) {
                order.setStatus("amount_mismatch");
                order.setMessage("微信支付回调金额与订单金额不一致，已阻止自动入账。");
                order.setNotifyPayload(truncate(rawBody, 1800));
                orderRepository.save(order);
                return wechatCallbackResponse("FAIL", "金额不一致");
            }
            if (!"paid".equals(order.getStatus())) {
                activatePaidOrder(order, payload.path("transaction_id").asText(""));
                order.setNotifyPayload(truncate(plain, 1800));
                orderRepository.save(order);
            }
            return wechatCallbackResponse("SUCCESS", "成功");
        } catch (Exception error) {
            return wechatCallbackResponse("FAIL", error.getMessage());
        }
    }

    @GetMapping("/wechat/health")
    public Map<String, Object> wechatHealth() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("configured", wechatConfigured(true));
        result.put("appIdConfigured", !blank(wechatAppId));
        result.put("merchantConfigured", !blank(wechatMchId));
        result.put("serialConfigured", !blank(wechatSerialNo));
        result.put("apiV3KeyConfigured", !blank(wechatApiV3Key));
        result.put("notifyUrlConfigured", !normalizedWechatNotifyUrl().isBlank());
        result.put("privateKeyConfigured", !blank(wechatPrivateKeyPem) || !blank(wechatPrivateKeyPath));
        result.put("privateKeyLoaded", false);
        result.put("wechatApiReachable", false);
        result.put("platformCertificateCount", 0);
        if (!wechatConfigured(true)) {
            result.put("ok", false);
            result.put("message", "微信支付配置不完整");
            return result;
        }
        try {
            wechatPrivateKey();
            result.put("privateKeyLoaded", true);
            if (wechatPublicKeyConfigured()) {
                // 微信支付公钥模式（新模式，替代平台证书）
                result.put("mode", "公钥模式");
                loadWechatPublicKey();
                result.put("wechatApiReachable", true);
                result.put("platformCertificateCount", -1);
                result.put("ok", true);
                result.put("wechatApiError", null);
                result.put("message", "微信支付公钥模式配置正常（公钥ID: " + wechatPublicKeyId.trim() + ")");
            } else {
                // 平台证书模式（旧模式，新商户已不支持）
                result.put("mode", "平台证书模式");
                lastWechatCertApiError = null;
                wechatPlatformCertificates.clear();
                wechatCertificateLoadedAt = 0L;
                wechatPlatformCertificate("__papersolver_health_check__");
                int certificateCount = wechatPlatformCertificates.size();
                result.put("platformCertificateCount", certificateCount);
                result.put("wechatApiReachable", certificateCount > 0);
                result.put("ok", certificateCount > 0);
                result.put("wechatApiError", lastWechatCertApiError);
                result.put("message", certificateCount > 0 ? "微信支付配置和平台证书拉取正常" : "未能拉取微信平台证书，微信返回：" + lastWechatCertApiError);
            }
        } catch (Exception error) {
            result.put("ok", false);
            result.put("wechatApiError", lastWechatCertApiError);
            result.put("message", "微信支付连通检查失败：" + error.getMessage());
        }
        return result;
    }

    @PostMapping("/tickets")
    public Map<String, Object> createTicket(@RequestBody Map<String, Object> body) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        String type = String.valueOf(body.getOrDefault("type", "support")).trim().toLowerCase();
        if (!List.of("support", "refund").contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "工单类型仅支持 support 或 refund");
        }
        String orderNo = String.valueOf(body.getOrDefault("orderNo", "")).trim();
        String subject = String.valueOf(body.getOrDefault("subject", type.equals("refund") ? "退款申请" : "支付工单")).trim();
        String detail = String.valueOf(body.getOrDefault("detail", "")).trim();
        if (subject.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写工单标题");
        }
        if (detail.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写更具体的问题说明");
        }
        if (orderNo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "售后申请必须选择一笔充值订单");
        }
        PaymentOrderEntity order = orderRepository.findById(orderNo)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "充值订单不存在"));
        if (!userId.equals(order.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不能为其他用户订单提交售后");
        }
        PaymentTicketEntity ticket = new PaymentTicketEntity();
        ticket.setUserId(userId);
        ticket.setType(type);
        ticket.setOrderNo(orderNo);
        ticket.setSubject(subject);
        ticket.setDetail(detail);
        ticket.setStatus("open");
        return ticketToMap(ticketRepository.save(ticket));
    }

    @RequestMapping(
        value = "/notify/zhifufm",
        method = {RequestMethod.GET, RequestMethod.POST},
        produces = MediaType.TEXT_PLAIN_VALUE
    )
    public String zhifuFmNotify(@RequestParam Map<String, String> params) {
        if (!zhifuConfigured(false)) {
            return "fail";
        }
        String merchantNum = params.getOrDefault("merchantNum", "");
        String orderNo = params.getOrDefault("orderNo", "");
        String amount = params.getOrDefault("amount", "");
        String state = params.getOrDefault("state", "");
        String sign = params.getOrDefault("sign", "");
        String expected = md5(state + zhifuMerchantNum.trim() + orderNo + amount + zhifuSecret.trim());
        if (!zhifuMerchantNum.trim().equals(merchantNum) || !expected.equalsIgnoreCase(sign)) {
            return "fail";
        }
        if (!"1".equals(state)) {
            return "fail";
        }
        PaymentOrderEntity order = orderRepository.findById(orderNo).orElse(null);
        if (order == null) {
            return "fail";
        }
        if (!sameMoney(order.getAmount(), amount)) {
            order.setStatus("amount_mismatch");
            order.setMessage("支付 FM 回调金额与订单金额不一致，已阻止自动入账。");
            order.setNotifyPayload(params.toString());
            orderRepository.save(order);
            return "fail";
        }
        if ("paid".equals(order.getStatus())) {
            return "success";
        }

        AppUserEntity user = appUserRepository.findById(order.getUserId()).orElse(null);
        if (user == null) {
            return "fail";
        }
        double paidAmount = normalizeMoney(order.getAmount()).doubleValue();
        if (!"custom-recharge".equals(order.getPlanId())) {
            membershipService.activate(user, order.getPlanId(), order.getPlanCycle());
        } else {
            user.setBalanceAmount((user.getBalanceAmount() == null ? 0.0 : user.getBalanceAmount()) + paidAmount);
            appUserRepository.save(user);
        }

        RechargeRecordEntity record = new RechargeRecordEntity();
        record.setEmail(user.getEmail());
        record.setAmount(paidAmount);
        record.setTokens(0L);
        rechargeRecordRepository.save(record);

        order.setStatus("paid");
        order.setActualPayAmount(parseMoney(params.getOrDefault("actualPayAmount", amount)).doubleValue());
        order.setPlatformOrderNo(params.getOrDefault("platformOrderNo", ""));
        order.setPaidAt(LocalDateTime.now());
        order.setNotifyPayload(params.toString());
        order.setMessage("custom-recharge".equals(order.getPlanId()) ? "支付成功，余额已入账。" : "支付成功，会员套餐已生效，功能额度已重置。");
        orderRepository.save(order);
        return "success";
    }

    private Map<String, Object> orderToMap(PaymentOrderEntity order) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("orderNo", order.getOrderNo());
        row.put("provider", order.getProvider());
        row.put("planId", order.getPlanId());
        row.put("planCycle", order.getPlanCycle());
        row.put("amount", order.getAmount());
        row.put("actualPayAmount", order.getActualPayAmount());
        row.put("status", order.getStatus());
        row.put("paymentUrl", order.getPaymentUrl());
        row.put("platformOrderNo", order.getPlatformOrderNo());
        row.put("message", order.getMessage());
        row.put("createdAt", order.getCreatedAt());
        row.put("paidAt", order.getPaidAt());
        return row;
    }

    private Map<String, Object> ticketToMap(PaymentTicketEntity ticket) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", ticket.getId());
        row.put("type", ticket.getType());
        row.put("orderNo", ticket.getOrderNo());
        row.put("subject", ticket.getSubject());
        row.put("detail", ticket.getDetail());
        row.put("status", ticket.getStatus());
        row.put("adminNote", ticket.getAdminNote());
        row.put("createdAt", ticket.getCreatedAt());
        row.put("processedAt", ticket.getProcessedAt());
        return row;
    }

    private boolean isUsableTicket(PaymentTicketEntity ticket) {
        return ticket.getDetail() != null && !ticket.getDetail().isBlank()
            && ticket.getOrderNo() != null && !ticket.getOrderNo().isBlank();
    }

    private PaymentOrderEntity startWechatNativeOrder(PaymentOrderEntity order) {
        if (!wechatConfigured(true)) {
            order.setStatus("config_required");
            order.setMessage("微信支付未配置完整：请配置 AppID、商户号、API v3 密钥、商户证书序列号、商户私钥和公网回调地址。");
            return order;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("appid", wechatAppId.trim());
            payload.put("mchid", wechatMchId.trim());
            payload.put("description", truncateUtf8("PaperSolver " + planLabel(order.getPlanId()) + "会员开通", 120));
            payload.put("out_trade_no", order.getOrderNo());
            payload.put("notify_url", normalizedWechatNotifyUrl());
            payload.put("amount", Map.of("total", toCents(order.getAmount()), "currency", "CNY"));
            payload.put("attach", truncate("user=" + order.getUserId() + ";plan=" + order.getPlanId() + ";cycle=" + order.getPlanCycle(), 120));
            String body = objectMapper.writeValueAsString(payload);
            URI uri = URI.create("https://api.mch.weixin.qq.com/v3/pay/transactions/native");
            HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", wechatAuthorization("POST", "/v3/pay/transactions/native", body))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode json = objectMapper.readTree(response.body());
            if (response.statusCode() >= 200 && response.statusCode() < 300 && json.hasNonNull("code_url")) {
                order.setPaymentUrl(json.path("code_url").asText(""));
                order.setStatus("pending_payment");
                order.setMessage("微信支付订单已创建，请使用微信扫码完成支付。");
            } else {
                order.setStatus("failed");
                order.setMessage("微信支付创建订单失败：" + json.path("message").asText(response.body()));
            }
        } catch (Exception error) {
            order.setStatus("failed");
            order.setMessage("微信支付创建订单异常：" + error.getMessage());
        }
        return order;
    }

    private boolean verifyWechatNotify(Map<String, String> headers, String rawBody) throws Exception {
        String timestamp = header(headers, "Wechatpay-Timestamp");
        String nonce = header(headers, "Wechatpay-Nonce");
        String signatureText = header(headers, "Wechatpay-Signature");
        String serial = header(headers, "Wechatpay-Serial");
        if (timestamp.isBlank() || nonce.isBlank() || signatureText.isBlank() || serial.isBlank()) return false;
        java.security.PublicKey publicKey;
        if (wechatPublicKeyConfigured() && serial.trim().equals(wechatPublicKeyId.trim())) {
            // 新模式：使用微信支付公钥验签
            publicKey = loadWechatPublicKey();
        } else {
            // 旧模式：使用平台证书验签
            X509Certificate certificate = wechatPlatformCertificate(serial);
            if (certificate == null) return false;
            publicKey = certificate.getPublicKey();
        }
        String message = timestamp + "\n" + nonce + "\n" + rawBody + "\n";
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update(message.getBytes(StandardCharsets.UTF_8));
        return verifier.verify(Base64.getDecoder().decode(signatureText));
    }

    private X509Certificate wechatPlatformCertificate(String serial) throws Exception {
        if (wechatPlatformCertificates.containsKey(serial)) return wechatPlatformCertificates.get(serial);
        if (Instant.now().toEpochMilli() - wechatCertificateLoadedAt < 60_000L && !wechatPlatformCertificates.isEmpty()) {
            return wechatPlatformCertificates.get(serial);
        }
        String body = "";
        URI uri = URI.create("https://api.mch.weixin.qq.com/v3/certificates");
        HttpRequest request = HttpRequest.newBuilder(uri)
            .header("Accept", "application/json")
            .header("Authorization", wechatAuthorization("GET", "/v3/certificates", body))
            .GET()
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            lastWechatCertApiError = "HTTP " + response.statusCode() + ": " + response.body();
            return null;
        }
        JsonNode root = objectMapper.readTree(response.body());
        for (JsonNode item : root.path("data")) {
            JsonNode encrypted = item.path("encrypt_certificate");
            String pem = decryptWechatResource(
                encrypted.path("associated_data").asText(""),
                encrypted.path("nonce").asText(""),
                encrypted.path("ciphertext").asText("")
            );
            X509Certificate certificate = parseCertificate(pem);
            wechatPlatformCertificates.put(item.path("serial_no").asText(""), certificate);
        }
        wechatCertificateLoadedAt = Instant.now().toEpochMilli();
        return wechatPlatformCertificates.get(serial);
    }

    private String wechatAuthorization(String method, String pathWithQuery, String body) throws Exception {
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String message = method + "\n" + pathWithQuery + "\n" + timestamp + "\n" + nonce + "\n" + Objects.toString(body, "") + "\n";
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(wechatPrivateKey());
        signer.update(message.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(signer.sign());
        return "WECHATPAY2-SHA256-RSA2048 "
            + "mchid=\"" + wechatMchId.trim() + "\","
            + "nonce_str=\"" + nonce + "\","
            + "timestamp=\"" + timestamp + "\","
            + "serial_no=\"" + wechatSerialNo.trim() + "\","
            + "signature=\"" + signature + "\"";
    }

    private PrivateKey wechatPrivateKey() throws Exception {
        String pem = wechatPrivateKeyPem == null ? "" : wechatPrivateKeyPem.trim();
        if (pem.isBlank() && wechatPrivateKeyPath != null && !wechatPrivateKeyPath.isBlank()) {
            pem = java.nio.file.Files.readString(java.nio.file.Path.of(wechatPrivateKeyPath.trim()), StandardCharsets.UTF_8);
        }
        pem = pem.replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replaceAll("\\s+", "");
        byte[] bytes = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    private java.security.PublicKey loadWechatPublicKey() throws Exception {
        String pem = wechatPublicKeyPem == null ? "" : wechatPublicKeyPem.trim();
        if (pem.isBlank() && !blank(wechatPublicKeyPath)) {
            pem = java.nio.file.Files.readString(java.nio.file.Path.of(wechatPublicKeyPath.trim()), StandardCharsets.UTF_8);
        }
        pem = pem.replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s+", "");
        byte[] bytes = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA").generatePublic(new java.security.spec.X509EncodedKeySpec(bytes));
    }

    private boolean wechatPublicKeyConfigured() {
        return !blank(wechatPublicKeyId) && (!blank(wechatPublicKeyPem) || !blank(wechatPublicKeyPath));
    }

    private String decryptWechatResource(String associatedData, String nonce, String ciphertext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec key = new SecretKeySpec(wechatApiV3Key.trim().getBytes(StandardCharsets.UTF_8), "AES");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
        if (associatedData != null && !associatedData.isBlank()) {
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        }
        return new String(cipher.doFinal(Base64.getDecoder().decode(ciphertext)), StandardCharsets.UTF_8);
    }

    private X509Certificate parseCertificate(String pem) throws Exception {
        try (java.io.ByteArrayInputStream input = new java.io.ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8))) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(input);
        }
    }

    private void activatePaidOrder(PaymentOrderEntity order, String transactionId) {
        AppUserEntity user = appUserRepository.findById(order.getUserId()).orElse(null);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单用户不存在");
        }
        double paidAmount = normalizeMoney(order.getAmount()).doubleValue();
        if (!"custom-recharge".equals(order.getPlanId())) {
            membershipService.activate(user, order.getPlanId(), order.getPlanCycle());
        } else {
            user.setBalanceAmount((user.getBalanceAmount() == null ? 0.0 : user.getBalanceAmount()) + paidAmount);
            appUserRepository.save(user);
        }
        RechargeRecordEntity record = new RechargeRecordEntity();
        record.setEmail(user.getEmail());
        record.setAmount(paidAmount);
        record.setTokens(0L);
        rechargeRecordRepository.save(record);
        order.setStatus("paid");
        order.setActualPayAmount(paidAmount);
        order.setPlatformOrderNo(transactionId);
        order.setPaidAt(LocalDateTime.now());
        order.setMessage("custom-recharge".equals(order.getPlanId()) ? "微信支付成功，余额已入账。" : "微信支付成功，会员套餐已生效，功能额度已重置。");
    }

    private boolean wechatConfigured(boolean requireNotifyUrl) {
        boolean basic = !blank(wechatAppId) && !blank(wechatMchId) && !blank(wechatApiV3Key)
            && !blank(wechatSerialNo) && (!blank(wechatPrivateKeyPem) || !blank(wechatPrivateKeyPath));
        if (!requireNotifyUrl) return basic;
        return basic && !normalizedWechatNotifyUrl().isBlank();
    }

    private String normalizedWechatNotifyUrl() {
        if (!blank(wechatNotifyUrl)) return wechatNotifyUrl.trim();
        if (!blank(publicBaseUrl)) return publicBaseUrl.trim().replaceAll("/+$", "") + "/api/payments/notify/wechat";
        return "";
    }

    private Map<String, String> wechatCallbackResponse(String code, String message) {
        return Map.of("code", code, "message", message == null ? "" : message);
    }

    private String header(Map<String, String> headers, String name) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue() == null ? "" : entry.getValue().trim();
            }
        }
        return "";
    }

    private boolean sameWechatAmount(Double expected, int paidCents) {
        return toCents(expected) == paidCents;
    }

    private int toCents(Double amount) {
        return normalizeMoney(amount == null ? 0.0 : amount)
            .multiply(BigDecimal.valueOf(100))
            .setScale(0, RoundingMode.HALF_UP)
            .intValueExact();
    }

    private String planLabel(String planId) {
        return switch (String.valueOf(planId)) {
            case "lite" -> "Lite";
            case "plus" -> "Plus";
            case "pro" -> "Pro";
            case "team_plus" -> "团队 Plus";
            case "team_pro" -> "团队 Pro";
            default -> "会员";
        };
    }

    private String truncate(String value, int maxLength) {
        String text = value == null ? "" : value;
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private String truncateUtf8(String value, int maxBytes) {
        String text = value == null ? "" : value;
        while (text.getBytes(StandardCharsets.UTF_8).length > maxBytes && !text.isEmpty()) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private boolean blank(String value) {
        return value == null || value.trim().isBlank();
    }

    private PaymentOrderEntity startZhifuFmOrder(PaymentOrderEntity order) {
        if (!zhifuConfigured(true)) {
            order.setStatus("config_required");
            order.setMessage("支付 FM 未配置完整：请配置接口根地址、商户号、接入密钥和公网回调域名。");
            return order;
        }
        String amount = normalizeMoney(order.getAmount()).toPlainString();
        String notifyUrl = zhifuNotifyUrl();
        String sign = md5(zhifuMerchantNum.trim() + order.getOrderNo() + amount + notifyUrl + zhifuSecret.trim());
        Map<String, String> params = new LinkedHashMap<>();
        params.put("merchantNum", zhifuMerchantNum.trim());
        params.put("orderNo", order.getOrderNo());
        params.put("amount", amount);
        params.put("notifyUrl", notifyUrl);
        params.put("returnUrl", zhifuReturnUrl());
        params.put("payType", payTypeFor(order.getProvider()));
        params.put("returnType", "json");
        params.put("apiMode", "post_form");
        params.put("subject", "PaperPilot 余额充值");
        params.put("body", "PaperPilot 账户余额充值 " + amount + " 元");
        params.put("sign", sign);
        try {
            String endpoint = startOrderEndpoint();
            String query = params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
            URI uri = URI.create(endpoint + (endpoint.contains("?") ? "&" : "?") + query);
            HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode json = objectMapper.readTree(response.body());
            boolean success = json.path("success").asBoolean(false) && json.path("code").asInt(0) == 200;
            if (response.statusCode() >= 200 && response.statusCode() < 300 && success) {
                JsonNode data = json.path("data");
                order.setPlatformOrderNo(data.path("id").asText(""));
                order.setPaymentUrl(data.path("payUrl").asText(""));
                order.setStatus(order.getPaymentUrl().isBlank() ? "failed" : "pending_payment");
                order.setMessage(order.getPaymentUrl().isBlank() ? "支付 FM 未返回支付链接。" : "订单已创建，正在跳转支付 FM 收银台。");
            } else {
                order.setStatus("failed");
                order.setMessage("支付 FM 创建订单失败：" + json.path("msg").asText("未知错误"));
            }
        } catch (Exception error) {
            order.setStatus("failed");
            order.setMessage("支付 FM 创建订单异常：" + error.getMessage());
        }
        return order;
    }

    private boolean zhifuConfigured(boolean requirePublicUrl) {
        boolean basic = !zhifuApiBaseUrl.isBlank() && !zhifuMerchantNum.isBlank() && !zhifuSecret.isBlank();
        if (!requirePublicUrl) {
            return basic;
        }
        return basic && (!configuredNotifyUrl.isBlank() || !publicBaseUrl.isBlank());
    }

    private String startOrderEndpoint() {
        String base = zhifuApiBaseUrl.trim().replaceAll("/+$", "");
        if (base.endsWith("/startOrder")) {
            return base;
        }
        return base + "/startOrder";
    }

    private String zhifuNotifyUrl() {
        if (!configuredNotifyUrl.isBlank()) {
            return configuredNotifyUrl.trim();
        }
        return publicBaseUrl.trim().replaceAll("/+$", "") + "/api/payments/notify/zhifufm";
    }

    private String zhifuReturnUrl() {
        if (!configuredReturnUrl.isBlank()) {
            return configuredReturnUrl.trim();
        }
        if (!publicBaseUrl.isBlank()) {
            return publicBaseUrl.trim().replaceAll("/+$", "") + "/#/models?tab=recharge";
        }
        return "";
    }

    private String payTypeFor(String provider) {
        return "wechat".equals(provider) ? wechatPayType.trim() : alipayPayType.trim();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String md5(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("MD5 algorithm unavailable", error);
        }
    }

    private BigDecimal normalizeMoney(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal parseMoney(String value) {
        return new BigDecimal(String.valueOf(value == null ? "0" : value).trim()).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean sameMoney(Double expected, String actual) {
        return normalizeMoney(expected == null ? 0.0 : expected).compareTo(parseMoney(actual)) == 0;
    }
}
