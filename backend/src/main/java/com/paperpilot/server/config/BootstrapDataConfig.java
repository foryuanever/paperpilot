package com.paperpilot.server.config;

import com.paperpilot.server.entity.PaperEntity;
import com.paperpilot.server.entity.InviteCodeEntity;
import com.paperpilot.server.entity.ForumPostEntity;
import com.paperpilot.server.entity.ForumReplyEntity;
import com.paperpilot.server.entity.ResearchTaskEntity;
import com.paperpilot.server.entity.AnnouncementEntity;
import com.paperpilot.server.entity.SharedResourceEntity;
import com.paperpilot.server.entity.CheckinEntity;
import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.entity.TranslationRecordEntity;
import com.paperpilot.server.entity.TeamEntity;

import com.paperpilot.server.repository.InviteCodeRepository;
import com.paperpilot.server.repository.PaperRepository;
import com.paperpilot.server.repository.AppUserRepository;
import com.paperpilot.server.repository.TranslationRecordRepository;
import com.paperpilot.server.repository.ForumPostRepository;
import com.paperpilot.server.repository.ForumReplyRepository;
import com.paperpilot.server.repository.ResearchTaskRepository;
import com.paperpilot.server.repository.AnnouncementRepository;
import com.paperpilot.server.repository.SharedResourceRepository;
import com.paperpilot.server.repository.CheckinRepository;
import com.paperpilot.server.repository.TeamRepository;

import com.paperpilot.server.service.CurrentUserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Configuration
public class BootstrapDataConfig {

    @Bean
    CommandLineRunner seedInitialData(
        CurrentUserService currentUserService,
        PaperRepository paperRepository,
        InviteCodeRepository inviteCodeRepository,
        AppUserRepository appUserRepository,
        TranslationRecordRepository translationRecordRepository,
        ForumPostRepository forumPostRepository,
        ForumReplyRepository forumReplyRepository,
        ResearchTaskRepository researchTaskRepository,
        AnnouncementRepository announcementRepository,
        SharedResourceRepository sharedResourceRepository,
        CheckinRepository checkinRepository,
        TeamRepository teamRepository
    ) {
        return (args) -> {
            seedInviteCodes(inviteCodeRepository);
            seedUsers(appUserRepository);
            seedTeam(teamRepository, appUserRepository);
            Long userId = currentUserService.getOrCreateDefaultUserId();
            seedTranslationRecords(translationRecordRepository, userId);
            
            // Seed the new collaborative components
            seedForumPosts(forumPostRepository, forumReplyRepository);
            seedResearchTasks(researchTaskRepository);
            seedAnnouncements(announcementRepository);
            seedSharedResources(sharedResourceRepository);
            seedCheckins(checkinRepository);

            if (paperRepository.count() > 0) {
                return;
            }

            paperRepository.saveAll(List.of(
                buildPaper(
                    userId,
                    "Attention Is All You Need",
                    "arXiv",
                    "Vaswani et al.",
                    "81%",
                    "A",
                    "重点关注自注意力结构和实验对比。",
                    "IF 17.9,CCF A,NLP",
                    "2017",
                    "https://arxiv.org/abs/1706.03762",
                    "实验复现",
                    LocalDateTime.of(2026, 6, 4, 20, 18),
                    LocalDate.of(2026, 6, 4)
                ),
                buildPaper(
                    userId,
                    "BERT: Pre-training of Deep Bidirectional Transformers",
                    "NAACL",
                    "Devlin et al.",
                    "64%",
                    "A",
                    "可作为预训练范式和下游迁移学习代表。",
                    "NAACL,预训练,经典",
                    "2019",
                    "https://aclanthology.org/N19-1423/",
                    "开题阶段",
                    LocalDateTime.of(2026, 6, 3, 15, 42),
                    LocalDate.of(2026, 6, 2)
                ),
                buildPaper(
                    userId,
                    "Retrieval-Augmented Generation for Knowledge-Intensive NLP",
                    "NeurIPS",
                    "Lewis et al.",
                    "43%",
                    "B",
                    "适合和自己的课题做 RAG 方向对照。",
                    "NeurIPS,RAG,LLM",
                    "2020",
                    "https://papers.nips.cc/paper/2020/hash/6b493230205f780e1bc26945df7481e5-Abstract.html",
                    "综述素材",
                    LocalDateTime.of(2026, 6, 1, 9, 26),
                    LocalDate.of(2026, 5, 30)
                )
            ));
        };
    }

    private void seedTeam(TeamRepository teamRepository, AppUserRepository appUserRepository) {
        TeamEntity team = teamRepository.findByIdentifier("LAB-2026-PILOT").orElseGet(() -> {
            TeamEntity created = new TeamEntity();
            created.setName("PaperSlover 科研团队");
            created.setIdentifier("LAB-2026-PILOT");
            created.setMemberCount(0);
            created.setSeatLimit(8);
            return teamRepository.save(created);
        });
        List<AppUserEntity> users = appUserRepository.findAll();
        users.sort((left, right) -> {
            LocalDateTime leftTime = left.getCreatedAt() != null ? left.getCreatedAt() : LocalDateTime.MIN;
            LocalDateTime rightTime = right.getCreatedAt() != null ? right.getCreatedAt() : LocalDateTime.MIN;
            return leftTime.compareTo(rightTime);
        });
        int baseSeatLimit = 8;
        int assignedSeats = 0;
        for (AppUserEntity user : users) {
            if (user.getEmail() != null && user.getEmail().endsWith("@paperslover.community")) {
                user.setTeamId(null);
                continue;
            }
            if (assignedSeats < baseSeatLimit) {
                user.setTeamId(team.getId());
                assignedSeats += 1;
            } else if (team.getId().equals(user.getTeamId())) {
                user.setTeamId(null);
            }
        }
        appUserRepository.saveAll(users);
        team.setMemberCount(assignedSeats);
        team.setSeatLimit(baseSeatLimit);
        teamRepository.save(team);
    }

    private void seedInviteCodes(InviteCodeRepository inviteCodeRepository) {
        List<String> codes = List.of("PAPERPILOT2026", "PAPERSLOVER2026", "RESEARCH-LAB", "INVITE-ONLY");
        for (String code : codes) {
            if (inviteCodeRepository.findByCodeAndActiveTrue(code).isPresent()) {
                continue;
            }
            InviteCodeEntity entity = new InviteCodeEntity();
            entity.setCode(code);
            entity.setActive(true);
            inviteCodeRepository.save(entity);
        }
    }

    private PaperEntity buildPaper(
        Long userId,
        String title,
        String source,
        String authors,
        String progress,
        String importance,
        String note,
        String tags,
        String publishYear,
        String paperUrl,
        String folder,
        LocalDateTime readAt,
        LocalDate uploadedAt
    ) {
        PaperEntity paper = new PaperEntity();
        paper.setWorkspaceId("ws-" + UUID.randomUUID());
        paper.setUserId(userId);
        paper.setTitle(title);
        paper.setSource(source);
        paper.setAuthors(authors);
        paper.setProgress(progress);
        paper.setImportance(importance);
        paper.setNote(note);
        paper.setJournalTags(tags);
        paper.setPublishYear(publishYear);
        paper.setPaperUrl(paperUrl);
        paper.setFolder(folder);
        paper.setReadAt(readAt);
        paper.setUploadedAt(uploadedAt);
        return paper;
    }

    private void seedUsers(AppUserRepository appUserRepository) {
        // Seed default system users with realistic IPs, active times, and token quotas
        seedUser(appUserRepository, "学生小张", "student@paperslover.app", "Student2026!", "学生", null, 18000L, 1000000L, 420000L);
        seedUser(appUserRepository, "导师王教授", "tutor@paperslover.app", "Tutor2026!", "导师", null, 12600L, 5000000L, 240000L);
        seedUser(appUserRepository, "超级管理员", "admin@paperslover.app", "Admin2026!", "管理员", null, 14400L, 1500000L, 98000L);
        
        // Seed My Team roster specific members
        seedUser(appUserRepository, "李小明", "xm.li@paperslover.com", "Student2026!", "学生", null, 18000L, 1000000L, 420000L);
        seedUser(appUserRepository, "张美华", "mh.zhang@paperslover.com", "Special2026!", "特权用户", null, 9000L, 2000000L, 780000L);
        seedUser(appUserRepository, "王大锤", "dc.wang@paperslover.com", "Student2026!", "学生", null, 0L, 500000L, 120000L);
        seedUser(appUserRepository, "赵铁柱", "tz.zhao@paperslover.com", "Admin2026!", "管理员", null, 14400L, 1500000L, 98000L);

        // Keep legacy forum authors addressable by profile cards, messages and friend requests.
        seedUser(appUserRepository, "李明航 (NLP 博士在读)", "liminghang@paperslover.community", "Research2026!", "科研用户", null, 9600L, 500000L, 80000L);
        seedUser(appUserRepository, "王小东 (算法工程师)", "wangxiaodong@paperslover.community", "Research2026!", "科研用户", null, 7200L, 500000L, 52000L);
        seedUser(appUserRepository, "AI研习者", "ai-researcher@paperslover.community", "Research2026!", "科研用户", null, 5400L, 500000L, 36000L);
        seedUser(appUserRepository, "张文杰 (CV 副教授)", "zhangwenjie@paperslover.community", "Research2026!", "导师", null, 8400L, 1000000L, 110000L);
        seedUser(appUserRepository, "Chen_RAG", "chen-rag@paperslover.community", "Research2026!", "科研用户", null, 4200L, 500000L, 22000L);
        seedUser(appUserRepository, "刘培强 (研究员)", "liupeiqiang@paperslover.community", "Research2026!", "科研用户", null, 7800L, 800000L, 74000L);
        seedUser(appUserRepository, "赵子明 (NLP工程师)", "zhaoziming@paperslover.community", "Research2026!", "科研用户", null, 6600L, 500000L, 48000L);
    }

    private void seedUser(AppUserRepository appUserRepository, String username, String email, String password, String role, String lastIp, long activeTime, long tokenLimit, long tokenUsed) {
        java.util.Optional<AppUserEntity> existingOpt = appUserRepository.findByEmail(email);
        AppUserEntity user;
        boolean isNew = existingOpt.isEmpty();
        if (existingOpt.isPresent()) {
            user = existingOpt.get();
        } else {
            user = new AppUserEntity();
            user.setEmail(email);
            user.setInviteCode("PAPERPILOT2026");
        }
        user.setUsername(username);
        user.setRole(role);
        user.setPasswordHash(hash(password));
        user.setPlainPassword(password);
        user.setLastIp(lastIp);
        user.setActiveTime(activeTime);
        if (isNew || user.getTokenLimit() == null) {
            user.setTokenLimit(tokenLimit);
        }
        if (isNew || user.getTokenUsed() == null) {
            user.setTokenUsed(tokenUsed);
        }
        appUserRepository.save(user);
    }

    private String hash(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    private void seedTranslationRecords(TranslationRecordRepository repo, Long userId) {
        if (repo.count() > 0) {
            return;
        }

        // DeepL: ~120 records, charCount around 1500 each, latency around 320ms
        for (int i = 0; i < 120; i++) {
            repo.save(buildRecord(userId, "deepl", 1000 + (long)(Math.random() * 1000), 200 + (long)(Math.random() * 300), true));
        }
        // Baidu: ~40 records
        for (int i = 0; i < 40; i++) {
            repo.save(buildRecord(userId, "baidu", 800 + (long)(Math.random() * 800), 120 + (long)(Math.random() * 180), true));
        }
        // Google: ~50 records
        for (int i = 0; i < 50; i++) {
            repo.save(buildRecord(userId, "google", 600 + (long)(Math.random() * 600), 150 + (long)(Math.random() * 200), true));
        }
        // Youdao: ~20 records
        for (int i = 0; i < 20; i++) {
            repo.save(buildRecord(userId, "youdao", 500 + (long)(Math.random() * 500), 180 + (long)(Math.random() * 220), true));
        }
        // AI: ~30 records
        for (int i = 0; i < 30; i++) {
            repo.save(buildRecord(userId, "ai", 1200 + (long)(Math.random() * 1500), 600 + (long)(Math.random() * 800), true));
        }

        // Seed a few failures to show success rate dynamically
        repo.save(buildRecord(userId, "deepl", 1200L, 2000L, false));
        repo.save(buildRecord(userId, "ai", 1500L, 5000L, false));
        repo.save(buildRecord(userId, "baidu", 600L, 1000L, false));
    }

    private TranslationRecordEntity buildRecord(Long userId, String provider, Long charCount, Long latencyMs, boolean success) {
        TranslationRecordEntity r = new TranslationRecordEntity();
        r.setUserId(userId);
        r.setProvider(provider);
        r.setCharCount(charCount);
        r.setLatencyMs(latencyMs);
        r.setSuccess(success);
        r.setCreatedAt(LocalDateTime.now().minusHours((long)(Math.random() * 48)));
        return r;
    }

    private void seedForumPosts(ForumPostRepository postRepo, ForumReplyRepository replyRepo) {
        if (postRepo.count() > 0) {
            return;
        }

        // Post 1
        ForumPostEntity post1 = new ForumPostEntity();
        post1.setTitle("Shuo Wang 在 Springer 新发的《AI for AI, not AI for science?》大家怎么看？学术界的研究泡沫真的这么严重吗？");
        post1.setAuthor("李明航 (NLP 博士在读)");
        post1.setAvatar("LM");
        post1.setPaperTitle("AI for AI, not AI for science?");
        post1.setPublishYear("2025");
        post1.setContent("刚刚精读了这篇文章，作者指出目前的 ‘AI for Science’ 很多演变成了 ‘AI for AI’，即简单应用技术以水论文，而不是真正去解决科学领域的本质痛点（比如精准医疗中狂飙的文献量，但平均引用和影响因子极低）。大家觉得这是否反映了现在的学术大环境？我们该怎么平衡技术热度和解决科学问题？");
        post1.setLikes(42);
        post1.setHasLiked(false);
        post1.setCreatedAt(LocalDateTime.now().minusHours(5));
        post1 = postRepo.save(post1);

        ForumReplyEntity r1_1 = new ForumReplyEntity();
        r1_1.setPostId(post1.getId());
        r1_1.setAuthor("张文杰 (CV 副教授)");
        r1_1.setAvatar("ZW");
        r1_1.setContent("非常赞同作者的观点。现在的很多工作就是把别人的医学影像数据集拿过来，套个最新的 Transformer 或者 Diffusion 模型，改一改注意力机制，刷几个百分点就发出来了。这其实是技术应用，不是科学创新。真正的 AI for Science 应该深入到蛋白质三维建模、新材料发现等基础科学规律上。");
        r1_1.setLikes(18);
        r1_1.setHasLiked(false);
        r1_1.setCreatedAt(LocalDateTime.now().minusHours(4));
        replyRepo.save(r1_1);

        ForumReplyEntity r1_2 = new ForumReplyEntity();
        r1_2.setPostId(post1.getId());
        r1_2.setAuthor("Chen_RAG");
        r1_2.setAvatar("CR");
        r1_2.setContent("我觉得这也是因为目前考核体制导致的，发论文短平快。但不可否认，这种应用型工作对于技术落地和工程优化也是有一定价值的，只是一味地泡沫化确实值得警惕。");
        r1_2.setLikes(9);
        r1_2.setHasLiked(false);
        r1_2.setCreatedAt(LocalDateTime.now().minusHours(3));
        replyRepo.save(r1_2);

        // Post 2
        ForumPostEntity post2 = new ForumPostEntity();
        post2.setTitle("Transformer 的 Scaled Dot-Product Attention 中，为什么缩放因子是 1/sqrt(d_k)?");
        post2.setAuthor("王小东 (算法工程师)");
        post2.setAvatar("WX");
        post2.setPaperTitle("Attention Is All You Need");
        post2.setPublishYear("2017");
        post2.setContent("在阅读经典论文时，论文提到当 d_k 很大时，点积结果在数量级上会增长得很大，导致 softmax 函数被推向具有极小梯度的区域。为了对抗这个影响，将其缩放 1/sqrt(d_k)。想请教大家，这个 1/sqrt(d_k) 的数学推导是怎么假设的？");
        post2.setLikes(31);
        post2.setHasLiked(false);
        post2.setCreatedAt(LocalDateTime.now().minusDays(1));
        post2 = postRepo.save(post2);

        ForumReplyEntity r2_1 = new ForumReplyEntity();
        r2_1.setPostId(post2.getId());
        r2_1.setAuthor("刘培强 (研究员)");
        r2_1.setAvatar("LP");
        r2_1.setContent("假设 q 和 k 是独立随机变量，均值为 0，方差为 1。那么它们的分量点积的均值为 0，方差为 1。根据方差的加性，d_k 个分量相加点积的方差就是 d_k。为了让点积结果的方差重新回到 1（防止 softmax 饱和），我们需要除以标准差，即 sqrt(d_k)。这就是 1/sqrt(d_k) 的由来。");
        r2_1.setLikes(25);
        r2_1.setHasLiked(false);
        r2_1.setCreatedAt(LocalDateTime.now().minusHours(18));
        replyRepo.save(r2_1);

        // Post 3
        ForumPostEntity post3 = new ForumPostEntity();
        post3.setTitle("BERT 的 Pre-training 任务中，Masked LM 的 15% 比例是如何确定的？");
        post3.setAuthor("AI研习者");
        post3.setAvatar("AY");
        post3.setPaperTitle("BERT: Pre-training of Deep Bidirectional Transformers for Language Understanding");
        post3.setPublishYear("2019");
        post3.setContent("论文中说对每句话随机 Mask 掉 15% 的 Token。如果比例太小，模型学习速度慢，开销大；如果比例太大，就会缺少足够的上下文环境来预测 Mask 掉的词。有大佬了解这个 15% 是否是经验最优值？有没有人做过更低或更高比例（比如 20% 或 30%）的消融实验？");
        post3.setLikes(19);
        post3.setHasLiked(false);
        post3.setCreatedAt(LocalDateTime.now().minusDays(3));
        post3 = postRepo.save(post3);

        ForumReplyEntity r3_1 = new ForumReplyEntity();
        r3_1.setPostId(post3.getId());
        r3_1.setAuthor("赵子明 (NLP工程师)");
        r3_1.setAvatar("ZZ");
        r3_1.setContent("后来的 RoBERTa 和一些重度 Mask 的论文（如 SpanBERT）做过消融。RoBERTa 在大批量下尝试过 15% 依然是比较稳健的折中点。其实如果做生成式预训练（如 T5），Mask 比例可以提到 15%-25%。对于 BERT 这种自编码结构，15% 刚好平衡了遮蔽噪音和语义密度。");
        r3_1.setLikes(12);
        r3_1.setHasLiked(false);
        r3_1.setCreatedAt(LocalDateTime.now().minusDays(2));
        replyRepo.save(r3_1);
    }

    private void seedResearchTasks(ResearchTaskRepository repo) {
        if (repo.count() > 0) {
            return;
        }

        ResearchTaskEntity t1 = new ResearchTaskEntity();
        t1.setTitle("撰写并提交 NeurIPS 论文初稿");
        t1.setDescription("完成实验数据整理及主图表绘制，撰写方法与讨论章节。");
        t1.setDeadline("2026-06-15 23:59");
        t1.setStatus("进行中");
        t1.setAttachments("[\n" +
            "  { \"name\": \"NeurIPS_Template.pdf\", \"size\": \"1.2 MB\", \"type\": \"application/pdf\", \"data\": \"data:application/pdf;base64,JVBERi0xLjQKJcfsj6IKMSAwIG9iago8PC9MZW5ndGggMiAwIFIvRmlsdGVyL0ZsYXRlRGVjb2RlPj4Kc3RyZWFtCnicS0wuyUxUCOTIzCnJSFXwS8xNVQj0c/NzUYgGABt6B2UKZW5kc3RyZWFtCmVuZG9iagoyIDAgb2JqCjMzCmVuZG9iagozIDAgb2JqCjw8L1R5cGUvUGFnZS9QYXJlbnQgNCAwIFIvUmVzb3VyY2VzPDwvRm9udDw8L0YxIDUgMCBSPj4+Pi9NZWRpYUJveFswIDAgNTk1IDg0Ml0vQ29udGVudHMgMSAwIFI+PgplbmRvYmoKNCAwIG9iago8PC9UeXBlL1BhZ2VzL0tpZHNbMyAwIFJdL0NvdW50IDE+PgplbmRvYmoKNSAwIG9iago8PC9UeXBlL0ZvbnQvU3VidHlwZS9UeXBlMS9CYXNlRm9udC9IZWx2ZXRpY2E+PgplbmRvYmoKNiAwIG9iago8PC9UeXBlL0NhdGFsb2cvUGFnZXMgNCAwIFI+PgplbmRvYmoKeHJlZgowIDcKMDAwMDAwMDAwMCA2NTUzNSBmIAowMDAwMDAwMDE5IDAwMDAwIG4gCjAwMDAwMDAxMDcgMDAwMDAgbiAKMDAwMDAwMDEyNiAwMDAwMCBuIAowMDAwMDAwMjMwIDAwMDAwIG4gCjAwMDAwMDAyNzQgMDAwMDAgbiAKMDAwMDAwMDMzNiAwMDAwMCBuIAp0cmFpbGVyCjw8L1NpemUgNy9Sb290IDYgMCBSPj4Kc3RhcnR4cmVmCjM4NQolJUVPRg==\" },\n" +
            "  { \"name\": \"Draft_Figures.png\", \"size\": \"3.4 MB\", \"type\": \"image/png\", \"data\": \"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==\" }\n" +
            "]");
        t1.setCreatedAt(LocalDateTime.now().minusDays(3));
        repo.save(t1);

        ResearchTaskEntity t2 = new ResearchTaskEntity();
        t2.setTitle("准备周五大组会 PPT 汇报");
        t2.setDescription("汇报最新学术对照翻译模块的架构设计与前端分栏优化进展。");
        t2.setDeadline("2026-06-12 14:00");
        t2.setStatus("进行中");
        t2.setAttachments("[\n" +
            "  { \"name\": \"Meeting_Agenda.docx\", \"size\": \"340 KB\", \"type\": \"application/vnd.openxmlformats-officedocument.wordprocessingml.document\", \"data\": \"data:application/octet-stream;base64,AAAA\" }\n" +
            "]");
        t2.setCreatedAt(LocalDateTime.now().minusDays(2));
        repo.save(t2);

        ResearchTaskEntity t3 = new ResearchTaskEntity();
        t3.setTitle("双栏布局排版算法调研");
        t3.setDescription("对比 PDF.js 输出 of 段落包围盒，并优化水平与垂直 Gap 合并策略。");
        t3.setDeadline("2026-06-05 18:00");
        t3.setStatus("已完成");
        t3.setAttachments("[]");
        t3.setCreatedAt(LocalDateTime.now().minusDays(8));
        repo.save(t3);
    }

    private void seedAnnouncements(AnnouncementRepository repo) {
        if (repo.count() > 0) {
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        AnnouncementEntity a1 = new AnnouncementEntity();
        a1.setTitle("关于实验室值班与安全的通知");
        a1.setContent("请离校前关闭工位设备电源，检查服务器运行温度。如有突发情况请联系值班管理员。");
        a1.setPublishTime(LocalDateTime.now().minusDays(2).format(formatter));
        a1.setCreatedAt(LocalDateTime.now().minusDays(2));
        repo.save(a1);

        AnnouncementEntity a2 = new AnnouncementEntity();
        a2.setTitle("本周五下午 14:00 举行实验室大组会");
        a2.setContent("请同学们提前上传本周工作报告，准备 10 分钟的工作展示 PPT。导师将进行工作进度点评。");
        a2.setPublishTime(LocalDateTime.now().minusDays(3).format(formatter));
        a2.setCreatedAt(LocalDateTime.now().minusDays(3));
        repo.save(a2);
    }

    private void seedSharedResources(SharedResourceRepository repo) {
        if (repo.count() > 0) {
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        SharedResourceEntity r1 = new SharedResourceEntity();
        r1.setName("学术规范与伦理指南.pdf");
        r1.setSize("1.4 MB");
        r1.setType("application/pdf");
        r1.setUploader("导师王教授");
        r1.setUploadTime(LocalDateTime.now().minusDays(2).format(formatter));
        r1.setData("data:application/pdf;base64,JVBERi0xLjQKJcfsj6IKMSAwIG9iago8PC9MZW5ndGggMiAwIFIvRmlsdGVyL0ZsYXRlRGVjb2RlPj4Kc3RyZWFtCnicS0wuyUxUCOTIzCnJSFXwS8xNVQj0c/NzUYgGABt6B2UKZW5kc3RyZWFtCmVuZG9iagoyIDAgb2JqCjMzCmVuZG9iagozIDAgb2JqCjw8L1R5cGUvUGFnZS9QYXJlbnQgNCAwIFIvUmVzb3VyY2VzPDwvRm9udDw8L0YxIDUgMCBSPj4+Pi9NZWRpYUJveFswIDAgNTk1IDg0Ml0vQ29udGVudHMgMSAwIFI+PgplbmRvYmoKNCAwIG9iago8PC9UeXBlL1BhZ2VzL0tpZHNbMyAwIFJdL0NvdW50IDE+PgplbmRvYmoKNSAwIG9iago8PC9UeXBlL0ZvbnQvU3VidHlwZS9UeXBlMS9CYXNlRm9udC9IZWx2ZXRpY2E+PgplbmRvYmoKNiAwIG9iago8PC9UeXBlL0NhdGFsb2cvUGFnZXMgNCAwIFI+PgplbmRvYmoKeHJlZgowIDcKMDAwMDAwMDAwMCA2NTUzNSBmIAowMDAwMDAwMDE5IDAwMDAwIG4gCjAwMDAwMDAxMDcgMDAwMDAgbiAKMDAwMDAwMDEyNiAwMDAwMCBuIAowMDAwMDAwMjMwIDAwMDAwIG4gCjAwMDAwMDAyNzQgMDAwMDAgbiAKMDAwMDAwMDMzNiAwMDAwMCBuIAp0cmFpbGVyCjw8L1NpemUgNy9Sb290IDYgMCBSPj4Kc3RhcnR4cmVmCjM4NQolJUVPRg==");
        r1.setCreatedAt(LocalDateTime.now().minusDays(2));
        repo.save(r1);

        SharedResourceEntity r2 = new SharedResourceEntity();
        r2.setName("实验公开数据集_BERT_Embedding.zip");
        r2.setSize("14.2 MB");
        r2.setType("application/zip");
        r2.setUploader("李小明");
        r2.setUploadTime(LocalDateTime.now().minusDays(1).format(formatter));
        r2.setData("data:application/octet-stream;base64,UEsFBgAAAAAAAAAAAAAAAAAAAAAAAA==");
        r2.setCreatedAt(LocalDateTime.now().minusDays(1));
        repo.save(r2);
    }

    private void seedCheckins(CheckinRepository repo) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        if (repo.count() > 0) {
            return;
        }

        // Seed check-ins for active users
        seedCheckin(repo, "tutor@paperslover.app", today + " 08:30", "已打卡", today);
        seedCheckin(repo, "xm.li@paperslover.com", today + " 08:45", "已打卡", today);
        seedCheckin(repo, "mh.zhang@paperslover.com", today + " 09:00", "已打卡", today);
        seedCheckin(repo, "tz.zhao@paperslover.com", today + " 08:55", "已打卡", today);
    }

    private void seedCheckin(CheckinRepository repo, String memberId, String time, String status, String date) {
        CheckinEntity c = new CheckinEntity();
        c.setMemberId(memberId);
        c.setTime(time);
        c.setStatus(status);
        c.setDate(date);
        repo.save(c);
    }
}
