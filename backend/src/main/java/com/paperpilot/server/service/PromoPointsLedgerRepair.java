package com.paperpilot.server.service;

import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.entity.PromoCodeEntity;
import com.paperpilot.server.entity.PromoCodeRedemptionEntity;
import com.paperpilot.server.entity.RechargeRecordEntity;
import com.paperpilot.server.repository.AppUserRepository;
import com.paperpilot.server.repository.PromoCodeRedemptionRepository;
import com.paperpilot.server.repository.PromoCodeRepository;
import com.paperpilot.server.repository.RechargeRecordRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Repairs promo grants written by releases that mixed model tokens with AI points. */
@Service
public class PromoPointsLedgerRepair {
    private static final long MATCH_WINDOW_SECONDS = 120L;

    private final PromoCodeRedemptionRepository redemptions;
    private final PromoCodeRepository promoCodes;
    private final RechargeRecordRepository recharges;
    private final AppUserRepository users;
    private final MembershipService memberships;

    public PromoPointsLedgerRepair(
        PromoCodeRedemptionRepository redemptions,
        PromoCodeRepository promoCodes,
        RechargeRecordRepository recharges,
        AppUserRepository users,
        MembershipService memberships
    ) {
        this.redemptions = redemptions;
        this.promoCodes = promoCodes;
        this.recharges = recharges;
        this.users = users;
        this.memberships = memberships;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void repairLegacyPromoPointGrants() {
        Set<Long> matchedRechargeIds = new HashSet<>();
        for (PromoCodeRedemptionEntity redemption : redemptions.findAll()) {
            AppUserEntity user = users.findById(redemption.getUserId()).orElse(null);
            PromoCodeEntity promo = promoCodes.findById(redemption.getPromoCodeId()).orElse(null);
            if (user == null || promo == null || user.getEmail() == null || redemption.getRedeemedAt() == null) continue;

            RechargeRecordEntity record = closestLegacyPromoRecord(
                recharges.findByEmailOrderByCreatedAtDesc(user.getEmail()), redemption, matchedRechargeIds
            );
            if (record == null) continue;

            long expectedPoints = number(memberships.plan(promo.getPlanId()).get("agentTokenQuota"));
            long recordedPoints = record.getTokens() == null ? 0L : Math.max(0L, record.getTokens());
            if (recordedPoints != expectedPoints) {
                long current = user.getFruitScore() == null ? 0L : user.getFruitScore();
                user.setFruitScore(safeScore(current - recordedPoints + expectedPoints));
                users.save(user);
            }

            record.setTokens(0L);
            record.setPointsGranted(expectedPoints);
            record.setRecordType("points");
            record.setPlanId(promo.getPlanId());
            recharges.save(record);
            matchedRechargeIds.add(record.getId());
        }
    }

    private RechargeRecordEntity closestLegacyPromoRecord(
        List<RechargeRecordEntity> candidates,
        PromoCodeRedemptionEntity redemption,
        Set<Long> matchedIds
    ) {
        RechargeRecordEntity closest = null;
        long closestSeconds = Long.MAX_VALUE;
        for (RechargeRecordEntity record : candidates) {
            if (record.getId() == null || matchedIds.contains(record.getId())) continue;
            if (record.getRecordType() != null || record.getCreatedAt() == null) continue;
            if (record.getAmount() == null || Math.abs(record.getAmount()) > 0.000001D) continue;
            long seconds = Math.abs(Duration.between(redemption.getRedeemedAt(), record.getCreatedAt()).getSeconds());
            if (seconds <= MATCH_WINDOW_SECONDS && seconds < closestSeconds) {
                closest = record;
                closestSeconds = seconds;
            }
        }
        return closest;
    }

    private long number(Object value) {
        if (value instanceof Number number) return Math.max(0L, number.longValue());
        try {
            return Math.max(0L, Long.parseLong(String.valueOf(value)));
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private int safeScore(long value) {
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, value));
    }
}
