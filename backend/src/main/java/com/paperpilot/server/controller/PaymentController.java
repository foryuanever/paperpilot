package com.paperpilot.server.controller;

import com.paperpilot.server.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final CurrentUserService currentUserService;

    public PaymentController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @PostMapping("/orders")
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> body) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        String planId = String.valueOf(body.getOrDefault("planId", "custom-recharge")).trim();
        String provider = String.valueOf(body.getOrDefault("provider", "")).trim().toLowerCase();
        double amount = Double.parseDouble(String.valueOf(body.getOrDefault("amount", "0")).replace("¥", "").trim());
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "充值金额必须大于 0");
        }
        if (!provider.equals("alipay") && !provider.equals("wechat")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "支付方式仅支持支付宝或微信支付");
        }
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("orderNo", "PP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + userId + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        order.put("planId", planId);
        order.put("provider", provider);
        order.put("amount", amount);
        String paymentUrl = resolvePaymentUrl(provider, String.valueOf(order.get("orderNo")), planId, amount);
        order.put("status", paymentUrl.isBlank() ? "config_required" : "pending_payment");
        order.put("paymentUrl", paymentUrl);
        order.put("message", paymentUrl.isBlank()
            ? "订单已创建。请在后端配置支付宝/微信支付商户号、应用密钥和收银台地址后启用真实跳转。"
            : "订单已创建，正在跳转支付收银台。");
        return order;
    }

    private String resolvePaymentUrl(String provider, String orderNo, String planId, double amount) {
        String envKey = provider.equals("alipay") ? "PAPERPILOT_ALIPAY_CASHIER_URL" : "PAPERPILOT_WECHAT_CASHIER_URL";
        String baseUrl = System.getenv(envKey);
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator
            + "orderNo=" + java.net.URLEncoder.encode(orderNo, java.nio.charset.StandardCharsets.UTF_8)
            + "&planId=" + java.net.URLEncoder.encode(planId, java.nio.charset.StandardCharsets.UTF_8)
            + "&amount=" + java.net.URLEncoder.encode(String.valueOf(amount), java.nio.charset.StandardCharsets.UTF_8);
    }
}
