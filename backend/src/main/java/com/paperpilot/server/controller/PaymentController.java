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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
        double amount = Double.parseDouble(String.valueOf(body.getOrDefault("amount", "0")).replace("¥", "").trim());
        if (!"custom-recharge".equals(planId)) {
            amount = membershipService.price(planId, planCycle);
        }
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "充值金额必须大于 0");
        }
        if (!provider.equals("alipay") && !provider.equals("wechat")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "支付方式仅支持支付宝或微信支付");
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
        order.setMessage("订单已创建，正在请求支付 FM。");
        order = orderRepository.save(order);
        order = startZhifuFmOrder(order);
        return orderToMap(orderRepository.save(order));
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
