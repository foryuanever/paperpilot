package com.paperpilot.server.controller;

import com.paperpilot.server.entity.PaymentOrderEntity;
import com.paperpilot.server.entity.PaymentTicketEntity;
import com.paperpilot.server.repository.PaymentOrderRepository;
import com.paperpilot.server.repository.PaymentTicketRepository;
import com.paperpilot.server.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final CurrentUserService currentUserService;
    private final PaymentOrderRepository orderRepository;
    private final PaymentTicketRepository ticketRepository;

    public PaymentController(
        CurrentUserService currentUserService,
        PaymentOrderRepository orderRepository,
        PaymentTicketRepository ticketRepository
    ) {
        this.currentUserService = currentUserService;
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
    }

    @GetMapping("/orders")
    public Map<String, Object> listOrders() {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orders", orderRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId).stream().map(this::orderToMap).toList());
        result.put("tickets", ticketRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId).stream().map(this::ticketToMap).toList());
        return result;
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
        String orderNo = "PP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + userId + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String paymentUrl = resolvePaymentUrl(provider, orderNo, planId, amount);
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setProvider(provider);
        order.setAmount(amount);
        order.setPaymentUrl(paymentUrl);
        order.setStatus(paymentUrl.isBlank() ? "config_required" : "pending_payment");
        order.setMessage(paymentUrl.isBlank()
            ? "订单已创建。请在后端配置支付宝/微信支付商户号、应用密钥和收银台地址后启用真实跳转。"
            : "订单已创建，正在跳转支付收银台。");
        return orderToMap(orderRepository.save(order));
    }

    @PostMapping("/tickets")
    public Map<String, Object> createTicket(@RequestBody Map<String, Object> body) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        String type = String.valueOf(body.getOrDefault("type", "support")).trim().toLowerCase();
        if (!List.of("support", "refund").contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "工单类型仅支持 support 或 refund");
        }
        PaymentTicketEntity ticket = new PaymentTicketEntity();
        ticket.setUserId(userId);
        ticket.setType(type);
        ticket.setOrderNo(String.valueOf(body.getOrDefault("orderNo", "")).trim());
        ticket.setSubject(type.equals("refund") ? "退款申请" : "支付工单");
        ticket.setStatus("open");
        return ticketToMap(ticketRepository.save(ticket));
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

    private Map<String, Object> orderToMap(PaymentOrderEntity order) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("orderNo", order.getOrderNo());
        row.put("provider", order.getProvider());
        row.put("amount", order.getAmount());
        row.put("status", order.getStatus());
        row.put("paymentUrl", order.getPaymentUrl());
        row.put("message", order.getMessage());
        row.put("createdAt", order.getCreatedAt());
        return row;
    }

    private Map<String, Object> ticketToMap(PaymentTicketEntity ticket) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", ticket.getId());
        row.put("type", ticket.getType());
        row.put("orderNo", ticket.getOrderNo());
        row.put("subject", ticket.getSubject());
        row.put("status", ticket.getStatus());
        row.put("createdAt", ticket.getCreatedAt());
        return row;
    }
}
