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
    private static final double DEFAULT_UNIT_PRICE = 0.0025D;
    private static final double DEFAULT_MULTIPLIER = 4.0D;
    private static final double DEFAULT_PPT_AGENT_MIN_CHARGE = 0.0D;

    private final BillingSettingRepository repository;

    public BillingService(BillingSettingRepository repository) {
        this.repository = repository;
    }

    public double unitPrice() {
        double saved = readDouble(KEY_UNIT_PRICE, DEFAULT_UNIT_PRICE);
        double multiplier = rawMultiplier();
        if (saved > DEFAULT_UNIT_PRICE * 2 && multiplier > 1.0D) {
            return saved / multiplier;
        }
        return saved;
    }

    public double multiplier() {
        double saved = rawMultiplier();
        return saved <= 1.0D ? DEFAULT_MULTIPLIER : saved;
    }

    private double rawMultiplier() {
        return readDouble(KEY_MULTIPLIER, DEFAULT_MULTIPLIER);
    }

    public double pptAgentMinCharge() {
        return readDouble(KEY_PPT_AGENT_MIN_CHARGE, DEFAULT_PPT_AGENT_MIN_CHARGE);
    }

    public double calculateCharge(long totalTokens) {
        if (totalTokens <= 0) return 0.0D;
        return calculateCharge(totalTokens, 0L);
    }

    public double calculateCharge(long promptTokens, long completionTokens) {
        long safePrompt = Math.max(0L, promptTokens);
        long safeCompletion = Math.max(0L, completionTokens);
        if (safePrompt + safeCompletion <= 0) return 0.0D;
        BigDecimal billableTokens = BigDecimal.valueOf(safePrompt)
            .add(BigDecimal.valueOf(safeCompletion).multiply(BigDecimal.valueOf(multiplier())));
        return billableTokens
            .multiply(BigDecimal.valueOf(unitPrice()))
            .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
            .doubleValue();
    }

    public double calculateCharge(String action, long totalTokens) {
        return calculateCharge(totalTokens);
    }

    public double calculateCharge(String action, long promptTokens, long completionTokens) {
        return calculateCharge(promptTokens, completionTokens);
    }

    public double normalizeInputUnitPrice(double savedUnitPrice, double multiplier) {
        if (savedUnitPrice <= 0) return unitPrice();
        if (savedUnitPrice > DEFAULT_UNIT_PRICE * 2 && multiplier > 1.0D) {
            return savedUnitPrice / multiplier;
        }
        return savedUnitPrice;
    }

    public double outputUnitPrice(double inputUnitPrice, double multiplier) {
        return Math.max(0.0D, inputUnitPrice) * Math.max(1.0D, multiplier);
    }

    public Map<String, Object> settings() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("unitPrice", unitPrice());
        result.put("outputUnitPrice", outputUnitPrice(unitPrice(), multiplier()));
        result.put("multiplier", multiplier());
        result.put("pptAgentMinCharge", pptAgentMinCharge());
        result.put("formula", "费用 = 输入单价 / 1000 × (文字输入 + 文字输出 × 补全倍率)");
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
