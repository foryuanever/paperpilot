package com.paperpilot.server.service;

import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.entity.MembershipPlanEntity;
import com.paperpilot.server.repository.AiUsageRecordRepository;
import com.paperpilot.server.repository.AppUserRepository;
import com.paperpilot.server.repository.MembershipPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MembershipServiceTest {
    private AppUserRepository users;
    private MembershipPlanRepository plans;
    private MembershipService service;

    @BeforeEach
    void setUp() {
        users = mock(AppUserRepository.class);
        AiUsageRecordRepository usage = mock(AiUsageRecordRepository.class);
        plans = mock(MembershipPlanRepository.class);
        when(users.save(any(AppUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(plans.save(any(MembershipPlanEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new MembershipService(users, usage, plans);
    }

    @Test
    void customPlanAppliesEveryConfiguredValueAndExactlyFiftyPoints() {
        MembershipPlanEntity custom = plan("custom_50", 50L, 10, 0, 12, 7, 3, 10, 10);
        when(plans.findById(any(String.class))).thenAnswer(invocation ->
            "custom_50".equals(invocation.getArgument(0)) ? Optional.of(custom) : Optional.empty());

        AppUserEntity user = userWithPoints(8);
        MembershipService.MembershipGrant grant = service.activate(user, "custom_50", "monthly");

        assertThat(grant.points()).isEqualTo(50L);
        assertThat(user.getFruitScore()).isEqualTo(58);
        assertThat(user.getReviewQuota()).isEqualTo(12);
        assertThat(user.getPptQuota()).isZero();
        assertThat(user.getChatQuota()).isEqualTo(7);
        assertThat(user.getResearchQuota()).isEqualTo(3);
        assertThat(user.getReportQuota()).isEqualTo(10);
        assertThat(user.getTranslateQuota()).isEqualTo(10);
        assertThat(user.getImmersiveQuota()).isEqualTo(10);
        assertThat(user.getTokenLimit()).isEqualTo(50L);
    }

    @Test
    void selectedPlanPointsDoNotComeFromOlderActivePlan() {
        MembershipPlanEntity selected = plan("custom_50", 50L, 10, 0, 12, 7, 3, 10, 10);
        MembershipPlanEntity old = plan("pro", 200_000L, 50, 6, 3600, 1200, 20, 50, 50);
        when(plans.findById(any(String.class))).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            if ("custom_50".equals(id)) return Optional.of(selected);
            if ("pro".equals(id)) return Optional.of(old);
            return Optional.empty();
        });

        AppUserEntity user = userWithPoints(100);
        user.setMembershipPlan("pro");
        user.setMembershipCycle("monthly");
        user.setMembershipStack("[{\"planId\":\"pro\",\"cycle\":\"monthly\",\"remainingSeconds\":2592000,\"status\":\"active\"}]");
        user.setLastMembershipEvaluationTime(LocalDateTime.now());

        MembershipService.MembershipGrant grant = service.activate(user, "custom_50", "monthly");

        assertThat(grant.points()).isEqualTo(50L);
        assertThat(user.getFruitScore()).isEqualTo(150);
        assertThat(user.getFruitScore()).isNotEqualTo(200_100);
    }

    private AppUserEntity userWithPoints(int points) {
        AppUserEntity user = new AppUserEntity();
        user.setFruitScore(points);
        user.setMembershipPlan("free");
        user.setMembershipStack("[]");
        user.setLastMembershipEvaluationTime(LocalDateTime.now());
        return user;
    }

    private MembershipPlanEntity plan(
        String id, long points, int translate, int ppt, int review, int chat,
        int research, int immersive, int report
    ) {
        MembershipPlanEntity plan = new MembershipPlanEntity();
        plan.setId(id);
        plan.setName(id);
        plan.setActiveFlag(true);
        plan.setTopUpPack(false);
        plan.setAgentEnabled(true);
        plan.setReviewEnabled(true);
        plan.setChatEnabled(true);
        plan.setAgentTokenQuota(points);
        plan.setTranslateQuota(translate);
        plan.setPptQuota(ppt);
        plan.setReviewQuota(review);
        plan.setChatQuota(chat);
        plan.setResearchQuota(research);
        plan.setImmersiveQuota(immersive);
        plan.setReportQuota(report);
        return plan;
    }
}
