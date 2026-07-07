package com.paperpilot.server.service;

import com.paperpilot.server.entity.BillingSettingEntity;
import com.paperpilot.server.repository.BillingSettingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class BillingService {
    private static final String KEY_UNIT_PRICE = "unit_price";
    private static final String KEY_MULTIPLIER = "multiplier";
    private static final String KEY_PPT_AGENT_MIN_CHARGE = "ppt_agent_min_charge";
    private static final double DEFAULT_UNIT_PRICE = 0.01D;
    private static final double DEFAULT_MULTIPLIER = 1.0D;
    private static final double DEFAULT_PPT_AGENT_MIN_CHARGE = 1.30D;

    private final BillingSettingRepository repository;

    public BillingService(BillingSettingRepository repository) {
        this.repository = repository;
    }

    public double unitPrice() {
        return readDouble(KEY_UNIT_PRICE, DEFAULT_UNIT_PRICE);
    }

    public double multiplier() {
        return readDouble(KEY_MULTIPLIER, DEFAULT_MULTIPLIER);
    }

    public double pptAgentMinCharge() {
        return readDouble(KEY_PPT_AGENT_MIN_CHARGE, DEFAULT_PPT_AGENT_MIN_CHARGE);
    }

    public double calculateCharge(long totalTokens) {
        if (totalTokens <= 0) return 0.0D;
        return BigDecimal.valueOf(totalTokens)
            .multiply(BigDecimal.valueOf(unitPrice()))
            .multiply(BigDecimal.valueOf(multiplier()))
            .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
            .doubleValue();
    }

    public double calculateCharge(String action, long totalTokens) {
        double tokenCharge = calculateCharge(totalTokens);
        if (isPptAgentAction(action)) {
            return BigDecimal.valueOf(Math.max(tokenCharge, pptAgentMinCharge()))
                .setScale(6, RoundingMode.HALF_UP)
                .doubleValue();
        }
        return tokenCharge;
    }

    public Map<String, Object> settings() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("unitPrice", unitPrice());
        result.put("multiplier", multiplier());
        result.put("pptAgentMinCharge", pptAgentMinCharge());
        result.put("formula", "普通调用 = Token 用量 × 站内单价 × 倍率 / 1000；组会 PPT Agent = max(普通公式, PPT Agent 单次任务最低扣费)");
        result.put("currency", "CNY");
        return result;
    }

    public Map<String, Object> update(double unitPrice, double multiplier) {
        return update(unitPrice, multiplier, pptAgentMinCharge());
    }

    public Map<String, Object> update(double unitPrice, double multiplier, double pptAgentMinCharge) {
        if (unitPrice <= 0 || multiplier <= 0) {
            throw new IllegalArgumentException("单价和倍率必须大于 0");
        }
        if (pptAgentMinCharge < 0) {
            throw new IllegalArgumentException("PPT Agent 单次任务扣费不能小于 0");
        }
        writeDouble(KEY_UNIT_PRICE, unitPrice);
        writeDouble(KEY_MULTIPLIER, multiplier);
        writeDouble(KEY_PPT_AGENT_MIN_CHARGE, pptAgentMinCharge);
        return settings();
    }

    public boolean isPptAgentAction(String action) {
        String value = action == null ? "" : action.toLowerCase();
        return value.contains("ppt") || value.contains("agent");
    }

    private double readDouble(String key, double fallback) {
        return repository.findById(key)
            .map(BillingSettingEntity::getValue)
            .map(value -> {
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException ignored) {
                    return fallback;
                }
            })
            .orElse(fallback);
    }

    private void writeDouble(String key, double value) {
        BillingSettingEntity entity = repository.findById(key).orElseGet(BillingSettingEntity::new);
        entity.setKey(key);
        entity.setValue(String.valueOf(value));
        repository.save(entity);
    }
}
