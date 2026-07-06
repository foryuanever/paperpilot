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
    private static final double DEFAULT_UNIT_PRICE = 0.01D;
    private static final double DEFAULT_MULTIPLIER = 1.0D;

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

    public double calculateCharge(long totalTokens) {
        if (totalTokens <= 0) return 0.0D;
        return BigDecimal.valueOf(totalTokens)
            .multiply(BigDecimal.valueOf(unitPrice()))
            .multiply(BigDecimal.valueOf(multiplier()))
            .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
            .doubleValue();
    }

    public Map<String, Object> settings() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("unitPrice", unitPrice());
        result.put("multiplier", multiplier());
        result.put("formula", "用户扣费 = Token 用量 × 站内单价 × 倍率 / 1000");
        result.put("currency", "CNY");
        return result;
    }

    public Map<String, Object> update(double unitPrice, double multiplier) {
        if (unitPrice <= 0 || multiplier <= 0) {
            throw new IllegalArgumentException("单价和倍率必须大于 0");
        }
        writeDouble(KEY_UNIT_PRICE, unitPrice);
        writeDouble(KEY_MULTIPLIER, multiplier);
        return settings();
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
