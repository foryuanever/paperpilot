package com.paperpilot.server.controller;

import com.paperpilot.server.entity.*;
import com.paperpilot.server.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/team")
public class TeamDataController {

    private final AppUserRepository appUserRepository;
    private final ResearchTaskRepository researchTaskRepository;
    private final AnnouncementRepository announcementRepository;
    private final SharedResourceRepository sharedResourceRepository;
    private final CheckinRepository checkinRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final TeamRepository teamRepository;
    private final com.paperpilot.server.service.AuthService authService;
    private final com.paperpilot.server.service.CurrentUserService currentUserService;
    private final com.paperpilot.server.service.NotificationService notificationService;
    private final com.paperpilot.server.service.MembershipService membershipService;
    private final com.paperpilot.server.service.MonitoringSecurityService monitoringSecurityService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final ZoneId CN_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int BASE_TEAM_SEATS = 8;
    private static final int TEAM_MEMBER_SEATS = 8;
    private static final int TEAM_PLUS_SEATS = 15;

    public TeamDataController(
        AppUserRepository appUserRepository,
        ResearchTaskRepository researchTaskRepository,
        AnnouncementRepository announcementRepository,
        SharedResourceRepository sharedResourceRepository,
        CheckinRepository checkinRepository,
        UserNotificationRepository userNotificationRepository,
        TeamRepository teamRepository,
        com.paperpilot.server.service.AuthService authService,
        com.paperpilot.server.service.CurrentUserService currentUserService,
        com.paperpilot.server.service.NotificationService notificationService,
        com.paperpilot.server.service.MembershipService membershipService
        , com.paperpilot.server.service.MonitoringSecurityService monitoringSecurityService
    ) {
        this.appUserRepository = appUserRepository;
        this.researchTaskRepository = researchTaskRepository;
        this.announcementRepository = announcementRepository;
        this.sharedResourceRepository = sharedResourceRepository;
        this.checkinRepository = checkinRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.teamRepository = teamRepository;
        this.authService = authService;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
        this.membershipService = membershipService;
        this.monitoringSecurityService = monitoringSecurityService;
    }

    private AppUserEntity requireCurrentTutorOrAdmin() {
        AppUserEntity user = currentUserService.getOrCreateDefaultUser();
        if (!"管理员".equals(user.getRole()) && !"导师".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅导师或管理员有权进行团队管理操作");
        }
        return user;
    }

    // --- Members ---

    @GetMapping("/info")
    public TeamEntity getTeamInfo() {
        TeamEntity team = teamRepository.findAll().stream()
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "尚未建立科研团队"));
        return normalizeTeamMembers(team);
    }

    @GetMapping("/members")
    public List<Map<String, Object>> getMembers() {
        TeamEntity team = normalizeTeamMembers(getTeamInfo());
        List<AppUserEntity> users = appUserRepository.findByTeamIdOrderByCreatedAtAsc(team.getId());
        List<Map<String, Object>> result = new ArrayList<>();

        for (AppUserEntity user : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", "m-" + user.getId());
            map.put("name", user.getUsername());
            map.put("email", user.getEmail());
            map.put("role", user.getRole() != null ? user.getRole() : "普通用户");
            boolean online = monitoringSecurityService.isUserOnline(user.getId());
            map.put("status", online ? "online" : "offline");
            map.put("isOnline", online);
            map.put("tokenUsed", user.getTokenUsed() != null ? user.getTokenUsed() : 0L);
            map.put("tokenLimit", user.getTokenLimit() != null ? user.getTokenLimit() : 5000000L);
            map.put("fruitScore", user.getFruitScore() != null ? user.getFruitScore() : 0);
            map.put("checkinScore", user.getCheckinScore() != null ? user.getCheckinScore() : 0);
            map.put("membershipPlan", user.getMembershipPlan() != null ? user.getMembershipPlan() : "free");
            map.put("membershipExpiresAt", user.getMembershipExpiresAt());
            map.put("activeTime", user.getActiveTime() != null ? user.getActiveTime() : 0L);
            map.put("registerTime", user.getCreatedAt() != null ? user.getCreatedAt().format(DATE_FORMATTER) : "2026-06-08");
            result.add(map);
        }
        return result;
    }

    @PostMapping("/presence")
    public Map<String, Object> presenceHeartbeat() {
        AppUserEntity user = currentUserService.getOrCreateDefaultUser();
        monitoringSecurityService.heartbeat(user.getId());
        return Map.of("online", true, "expiresInSeconds", 90);
    }

    @PostMapping("/members/active-time")
    public void incrementActiveTime(@RequestBody Map<String, Object> body) {
        String email = (String) body.get("email");
        Number secondsVal = (Number) body.get("seconds");
        if (email == null || secondsVal == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参数非法");
        }
        long seconds = secondsVal.longValue();

        AppUserEntity user = appUserRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));

        Long current = user.getActiveTime() != null ? user.getActiveTime() : 0L;
        user.setActiveTime(current + seconds);
        appUserRepository.save(user);
    }

    @PostMapping("/members")
    public Map<String, Object> addMember(@RequestBody Map<String, Object> body) {
        requireCurrentTutorOrAdmin();
        String name = (String) body.get("name");
        String email = (String) body.get("email");
        String role = (String) body.get("role");
        Number limitVal = (Number) body.get("tokenLimit");

        if (name == null || email == null || role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "姓名、邮箱和角色不能为空");
        }

        if (appUserRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该邮箱已被使用");
        }

        TeamEntity team = normalizeTeamMembers(getTeamInfo());
        int usedSeats = appUserRepository.findByTeamIdOrderByCreatedAtAsc(team.getId()).size();
        if (usedSeats >= effectiveSeatLimit(team)) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "当前团队默认 8 个席位；继续加人需要导师开通“导师车队会员”。");
        }

        AppUserEntity user = new AppUserEntity();
        user.setUsername(name);
        user.setEmail(email);
        user.setRole(role);
        user.setInviteCode("PAPERPILOT2026");
        user.setTokenLimit(limitVal != null ? limitVal.longValue() : 1000000L);
        user.setTokenUsed(0L);
        user.setActiveTime(0L);
        user.setLastIp(null);
        user.setTeamId(team.getId());
        
        String defaultPw = role.equals("导师") ? "Tutor2026!" : (role.equals("管理员") ? "Admin2026!" : "Student2026!");
        user.setPlainPassword(null);
        user.setPasswordHash(hash(defaultPw));

        AppUserEntity saved = appUserRepository.save(user);

        Map<String, Object> map = new HashMap<>();
        map.put("id", "m-" + saved.getId());
        map.put("name", saved.getUsername());
        map.put("email", saved.getEmail());
        map.put("role", saved.getRole());
        map.put("tokenLimit", saved.getTokenLimit());
        map.put("tokenUsed", saved.getTokenUsed());
        return map;
    }

    @DeleteMapping("/members/{id}")
    public void deleteMember(@PathVariable("id") String idStr) {
        requireCurrentTutorOrAdmin();
        Long id = parseId(idStr);
        authService.adminDeleteUser(id, "127.0.0.1");
    }

    @PatchMapping("/members/{id}/quota")
    public void updateMemberQuota(@PathVariable("id") String idStr, @RequestBody Map<String, Object> body) {
        requireCurrentTutorOrAdmin();
        Long id = parseId(idStr);
        Number limitVal = (Number) body.get("tokenLimit");
        if (limitVal == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "限额不能为空");
        }
        AppUserEntity user = appUserRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        user.setTokenLimit(limitVal.longValue());
        appUserRepository.save(user);
    }

    @PatchMapping("/members/{id}/role")
    public void updateMemberRole(@PathVariable("id") String idStr, @RequestBody Map<String, Object> body) {
        requireCurrentTutorOrAdmin();
        Long id = parseId(idStr);
        String role = (String) body.get("role");
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色不能为空");
        }
        AppUserEntity user = appUserRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        user.setRole(role);
        appUserRepository.save(user);
    }

    private Long parseId(String idStr) {
        try {
            if (idStr.startsWith("m-")) {
                return Long.parseLong(idStr.substring(2));
            }
            return Long.parseLong(idStr);
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的ID格式");
        }
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


    // --- Tasks ---

    @GetMapping("/tasks")
    public List<ResearchTaskEntity> getTasks() {
        return researchTaskRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/tasks")
    public ResearchTaskEntity createTask(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String description = (String) body.get("description");
        String deadline = (String) body.get("deadline");
        String attachments = (String) body.get("attachments"); // JSON String representing attachment list

        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "任务名称不能为空");
        }

        ResearchTaskEntity task = new ResearchTaskEntity();
        task.setTitle(title);
        task.setDescription(description);
        task.setDeadline(deadline);
        task.setStatus("进行中");
        task.setAttachments(attachments != null ? attachments : "[]");

        ResearchTaskEntity saved = researchTaskRepository.save(task);
        notifyStudents("task", saved.getId(), "新科研任务提醒", "导师发布了新任务：" + saved.getTitle());
        return saved;
    }

    @PatchMapping("/tasks/{id}")
    public ResearchTaskEntity updateTask(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        ResearchTaskEntity task = researchTaskRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在"));
        String title = (String) body.get("title");
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "任务名称不能为空");
        }
        task.setTitle(title);
        task.setDescription((String) body.get("description"));
        task.setDeadline((String) body.get("deadline"));
        if (body.containsKey("attachments")) {
            task.setAttachments((String) body.get("attachments"));
        }
        return researchTaskRepository.save(task);
    }

    @DeleteMapping("/tasks/{id}")
    public void deleteTask(@PathVariable("id") Long id) {
        ResearchTaskEntity task = researchTaskRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在"));
        researchTaskRepository.delete(task);
    }

    // --- Announcements ---

    @GetMapping("/announcements")
    public List<AnnouncementEntity> getAnnouncements() {
        return announcementRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/announcements")
    public AnnouncementEntity createAnnouncement(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        String image = (String) body.get("image");
        String link = (String) body.get("link");
        String attachmentName = (String) body.get("attachmentName");
        String attachmentType = (String) body.get("attachmentType");
        String attachmentData = (String) body.get("attachmentData");
        String attachmentSize = (String) body.get("attachmentSize");

        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "标题和内容不能为空");
        }

        AnnouncementEntity ann = new AnnouncementEntity();
        ann.setTitle(title);
        ann.setContent(content);
        ann.setImage(image);
        ann.setLink(link);
        ann.setAttachmentName(attachmentName);
        ann.setAttachmentType(attachmentType);
        ann.setAttachmentData(attachmentData);
        ann.setAttachmentSize(attachmentSize);
        ann.setPublishTime(LocalDateTime.now().format(TIME_FORMATTER));

        AnnouncementEntity saved = announcementRepository.save(ann);
        notifyStudents("announcement", saved.getId(), "新通知公告", "导师发布了新通知：" + saved.getTitle());
        return saved;
    }

    @PatchMapping("/announcements/{id}")
    public AnnouncementEntity updateAnnouncement(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        AnnouncementEntity announcement = announcementRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "公告不存在"));
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "标题和内容不能为空");
        }
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setImage((String) body.get("image"));
        announcement.setLink((String) body.get("link"));
        announcement.setAttachmentName((String) body.get("attachmentName"));
        announcement.setAttachmentType((String) body.get("attachmentType"));
        announcement.setAttachmentData((String) body.get("attachmentData"));
        announcement.setAttachmentSize((String) body.get("attachmentSize"));
        return announcementRepository.save(announcement);
    }

    @DeleteMapping("/announcements/{id}")
    public void deleteAnnouncement(@PathVariable("id") Long id) {
        AnnouncementEntity announcement = announcementRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "公告不存在"));
        announcementRepository.delete(announcement);
    }

    @GetMapping("/announcements/{id}/attachment")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.ByteArrayResource> downloadAnnouncementAttachment(
        @PathVariable("id") Long id
    ) {
        AnnouncementEntity announcement = announcementRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "公告不存在"));
        String encodedData = announcement.getAttachmentData();
        if (encodedData == null || encodedData.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "公告附件不存在");
        }

        int commaIndex = encodedData.indexOf(',');
        String base64Data = commaIndex >= 0 ? encodedData.substring(commaIndex + 1) : encodedData;
        byte[] data;
        try {
            data = Base64.getDecoder().decode(base64Data);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "公告附件数据损坏");
        }

        String fileName = announcement.getAttachmentName() != null
            ? announcement.getAttachmentName()
            : "attachment";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        String contentType = announcement.getAttachmentType() != null && !announcement.getAttachmentType().isBlank()
            ? announcement.getAttachmentType()
            : "application/octet-stream";

        return org.springframework.http.ResponseEntity.ok()
            .header("Content-Type", contentType)
            .header("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName)
            .contentLength(data.length)
            .body(new org.springframework.core.io.ByteArrayResource(data));
    }

    private void notifyStudents(String type, Long referenceId, String title, String description) {
        List<UserNotificationEntity> notifications = appUserRepository.findAll().stream()
            .filter(user -> "普通用户".equals(user.getRole()) || "特权用户".equals(user.getRole()))
            .map(user -> {
                UserNotificationEntity notification = new UserNotificationEntity();
                notification.setUserId(user.getId());
                notification.setType(type);
                notification.setReferenceId(referenceId);
                notification.setTitle(title);
                notification.setDescription(description);
                notification.setReadFlag(false);
                return notification;
            })
            .toList();
        userNotificationRepository.saveAll(notifications);
    }

    // --- Resources ---

    @GetMapping("/resources")
    public List<SharedResourceEntity> getResources() {
        return sharedResourceRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/resources")
    public SharedResourceEntity uploadResource(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String size = (String) body.get("size");
        String type = (String) body.get("type");
        String uploader = (String) body.get("uploader");
        String data = (String) body.get("data");

        if (name == null || name.isBlank() || data == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "资源名称和内容不能为空");
        }

        SharedResourceEntity res = new SharedResourceEntity();
        res.setName(name);
        res.setSize(size != null ? size : "0 KB");
        res.setType(type != null ? type : "application/octet-stream");
        res.setUploader(uploader != null ? uploader : "匿名");
        res.setUploadTime(LocalDateTime.now().format(TIME_FORMATTER));
        res.setData(data);

        return sharedResourceRepository.save(res);
    }

    @GetMapping("/resources/{id}")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.ByteArrayResource> downloadResource(@PathVariable("id") Long id) {
        SharedResourceEntity res = sharedResourceRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "资源不存在"));
        byte[] data = java.util.Base64.getDecoder().decode(res.getData());
        org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(data);
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Type", res.getType())
                .header("Content-Disposition", "attachment; filename=\"" + res.getName() + "\"")
                .body(resource);
    }

    // --- Checkins ---
    @DeleteMapping("/resources/{id}")
    public void deleteResource(@PathVariable("id") Long id) {
        SharedResourceEntity res = sharedResourceRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "资源不存在"));
        sharedResourceRepository.delete(res);
    }

    @GetMapping("/checkins")
    public List<Map<String, Object>> getCheckins(@RequestParam(name = "date", required = false) String date) {
        String queryDate = date != null ? date : LocalDate.now(CN_ZONE).format(DATE_FORMATTER);
        List<CheckinEntity> list = checkinRepository.findAllByDate(queryDate);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (CheckinEntity c : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("memberId", c.getMemberId());
            map.put("time", c.getTime());
            map.put("status", c.getStatus());
            map.put("fruitAward", c.getFruitAward() != null ? c.getFruitAward() : 0);
            map.put("fruitClaimed", Boolean.TRUE.equals(c.getFruitClaimed()));
            map.put("awardType", c.getAwardType());
            map.put("awardName", c.getAwardName());
            map.put("streak", calculateStreak(c.getMemberId()));
            result.add(map);
        }
        return result;
    }

    @GetMapping("/checkins/history")
    public List<Map<String, Object>> getCheckinHistory(
        @RequestParam(name = "memberId") String memberId,
        @RequestParam(name = "year", required = false) Integer year
    ) {
        if (memberId == null || memberId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "成员ID不能为空");
        }
        int targetYear = year != null ? year : LocalDate.now(CN_ZONE).getYear();
        String start = LocalDate.of(targetYear, 1, 1).format(DATE_FORMATTER);
        String end = LocalDate.of(targetYear, 12, 31).format(DATE_FORMATTER);
        return checkinRepository.findAllByMemberIdAndDateBetweenOrderByDateAsc(memberId, start, end).stream()
            .map(item -> {
                Map<String, Object> map = new HashMap<>();
                map.put("memberId", item.getMemberId());
                map.put("date", item.getDate());
                map.put("time", item.getTime());
                map.put("status", item.getStatus());
                map.put("fruitAward", item.getFruitAward() != null ? item.getFruitAward() : 0);
                map.put("fruitClaimed", Boolean.TRUE.equals(item.getFruitClaimed()));
                return map;
            })
            .toList();
    }

    @PostMapping("/checkins")
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> addCheckin(@RequestBody(required = false) Map<String, Object> body) {
        AppUserEntity currentUser = currentUserService.getOrCreateDefaultUser();
        String memberId = currentUser.getEmail();

        String status = (body != null && body.get("status") != null) ? (String) body.get("status") : "已打卡";

        String today = LocalDate.now(CN_ZONE).format(DATE_FORMATTER);
        java.util.Optional<CheckinEntity> existing = checkinRepository.findByMemberIdAndDate(memberId, today);
        CheckinEntity checkin = existing.orElse(new CheckinEntity());
        boolean firstCheckinToday = existing.isEmpty();
            
        checkin.setMemberId(memberId);
        checkin.setDate(today);
        checkin.setStatus(status);
        checkin.setTime(LocalDateTime.now(CN_ZONE).format(TIME_FORMATTER));
        if (firstCheckinToday) {
            checkin.setFruitAward(0);
            checkin.setFruitClaimed(false);
        }

        CheckinEntity saved = checkinRepository.save(checkin);
        
        if (firstCheckinToday) {
            currentUser.setCheckinScore((currentUser.getCheckinScore() != null ? currentUser.getCheckinScore() : 0) + 2);
            appUserRepository.save(currentUser);
            notificationService.createSystemNotice(
                currentUser.getId(), null, "checkin_reward", saved.getId(), "签到奖励已到账",
                "今日签到奖励已到账：+2 签到积分，完成抽奖还可领取额外奖励。"
            );
        }

        Map<String, Object> res = new HashMap<>();
        res.put("memberId", saved.getMemberId());
        res.put("status", saved.getStatus());
        res.put("time", saved.getTime());
        res.put("fruitAward", saved.getFruitAward() != null ? saved.getFruitAward() : 0);
        res.put("fruitClaimed", Boolean.TRUE.equals(saved.getFruitClaimed()));
        res.put("streak", calculateStreak(saved.getMemberId()));
        res.put("fruitScore", currentUser.getFruitScore() != null ? currentUser.getFruitScore() : 0);
        res.put("checkinScore", currentUser.getCheckinScore() != null ? currentUser.getCheckinScore() : 0);
        return res;
    }

    public record WheelPrize(int index, String type, String name, int points, int fullTranslate, int bilingualTranslate) {}

    private static final WheelPrize[] WHEEL_PRIZES = new WheelPrize[] {
        new WheelPrize(0, "points", "1 积分", 1, 0, 0),
        new WheelPrize(1, "bilingual_translate", "对照翻译 1 次", 0, 0, 1),
        new WheelPrize(2, "full_translate", "沉浸翻译 1 次", 0, 1, 0),
        new WheelPrize(3, "bilingual_translate", "对照翻译 2 次", 0, 0, 2),
        new WheelPrize(4, "full_translate", "沉浸翻译 2 次", 0, 2, 0),
        new WheelPrize(5, "nothing", "遗憾未中奖", 0, 0, 0)
    };

    @PostMapping("/checkins/draw")
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> drawCheckinFruit(@RequestBody(required = false) Map<String, Object> body) {
        AppUserEntity user = currentUserService.getOrCreateDefaultUser();
        String memberId = user.getEmail();

        String today = LocalDate.now(CN_ZONE).format(DATE_FORMATTER);
        CheckinEntity checkin = checkinRepository.findByMemberIdAndDate(memberId, today)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "今天还没有签到，不能抽取积分"));
        if (!"已打卡".equals(checkin.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "今天还没有完成签到");
        }
        if (Boolean.TRUE.equals(checkin.getFruitClaimed())) {
            return checkinResponse(checkin);
        }

        int previousStreak = Math.max(0, calculateStreak(memberId) - 1);
        WheelPrize prize = rollPrize(previousStreak);
        checkin.setFruitAward(prize.points());
        checkin.setAwardType(prize.type());
        checkin.setAwardName(prize.name());
        checkin.setFruitClaimed(true);
        CheckinEntity saved = checkinRepository.save(checkin);
        
        if (prize.points() > 0) {
            user.setFruitScore((user.getFruitScore() != null ? user.getFruitScore() : 0) + prize.points());
            user.setCheckinScore((user.getCheckinScore() != null ? user.getCheckinScore() : 0) + prize.points());
        }
        if (prize.fullTranslate() > 0) {
            membershipService.addImmersiveDailyBonus(user, prize.fullTranslate());
        }
        if (prize.bilingualTranslate() > 0) {
            membershipService.addTranslateDailyBonus(user, prize.bilingualTranslate());
        }
        appUserRepository.save(user);

        if (!"nothing".equalsIgnoreCase(prize.type())) {
            String rewardDescription = switch (prize.type()) {
                case "points" -> "签到抽奖奖励已到账：+" + prize.points() + " 积分。";
                case "full_translate", "bilingual_translate" -> "签到抽奖奖励已到账：" + prize.name() + "。";
                default -> "";
            };
            notificationService.createSystemNotice(
                user.getId(), null, "checkin_reward", saved.getId(), "签到奖励已到账", rewardDescription
            );
        }

        Map<String, Object> resp = checkinResponse(saved);
        resp.put("prizeIndex", prize.index());
        resp.put("awardType", prize.type());
        resp.put("awardName", prize.name());
        resp.put("fruitScore", user.getFruitScore() != null ? user.getFruitScore() : 0);
        resp.put("checkinScore", user.getCheckinScore() != null ? user.getCheckinScore() : 0);
        return resp;
    }

    private Map<String, Object> checkinResponse(CheckinEntity saved) {
        Map<String, Object> res = new HashMap<>();
        res.put("memberId", saved.getMemberId());
        res.put("status", saved.getStatus());
        res.put("time", saved.getTime());
        res.put("fruitAward", saved.getFruitAward() != null ? saved.getFruitAward() : 0);
        res.put("awardType", saved.getAwardType() != null ? saved.getAwardType() : "points");
        res.put("awardName", saved.getAwardName() != null ? saved.getAwardName() : (saved.getFruitAward() + " 积分"));
        res.put("fruitClaimed", Boolean.TRUE.equals(saved.getFruitClaimed()));
        res.put("streak", calculateStreak(saved.getMemberId()));
        findUserByMemberId(saved.getMemberId()).ifPresent(user -> {
            res.put("fruitScore", user.getFruitScore() != null ? user.getFruitScore() : 0);
            res.put("checkinScore", user.getCheckinScore() != null ? user.getCheckinScore() : 0);
            res.put("translateQuota", user.getTranslateQuota() != null ? user.getTranslateQuota() : 0);
            res.put("immersiveQuota", user.getImmersiveQuota() != null ? user.getImmersiveQuota() : 0);
        });
        return res;
    }

    private java.util.Optional<AppUserEntity> findUserByMemberId(String memberId) {
        if (memberId == null || memberId.isBlank()) return java.util.Optional.empty();
        if (memberId.startsWith("m-")) {
            try {
                return appUserRepository.findById(Long.parseLong(memberId.substring(2)));
            } catch (NumberFormatException ignored) {
                return java.util.Optional.empty();
            }
        }
        return appUserRepository.findByEmail(memberId).or(() -> appUserRepository.findByUsername(memberId));
    }

    private WheelPrize rollPrize(int previousStreak) {
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        return WHEEL_PRIZES[random.nextInt(WHEEL_PRIZES.length)];
    }

    private int effectiveSeatLimit(TeamEntity team) {
        if (team == null || team.getId() == null) return BASE_TEAM_SEATS;
        LocalDateTime now = LocalDateTime.now(CN_ZONE);
        List<AppUserEntity> members = appUserRepository.findByTeamIdOrderByCreatedAtAsc(team.getId());
        boolean hasTeamPlusPlan = members.stream()
            .anyMatch(user -> "导师".equals(user.getRole())
                && "team_plus".equals(user.getMembershipPlan())
                && user.getMembershipExpiresAt() != null
                && user.getMembershipExpiresAt().isAfter(now));
        if (hasTeamPlusPlan) return TEAM_PLUS_SEATS;
        boolean hasTeamPlan = members.stream()
            .anyMatch(user -> "导师".equals(user.getRole())
                && "team".equals(user.getMembershipPlan())
                && user.getMembershipExpiresAt() != null
                && user.getMembershipExpiresAt().isAfter(now));
        return hasTeamPlan ? TEAM_MEMBER_SEATS : BASE_TEAM_SEATS;
    }

    private TeamEntity normalizeTeamMembers(TeamEntity team) {
        if (team == null || team.getId() == null) return team;
        int seatLimit = effectiveSeatLimit(team);
        List<AppUserEntity> members = appUserRepository.findByTeamIdOrderByCreatedAtAsc(team.getId());
        if (members.size() > seatLimit) {
            List<AppUserEntity> overflow = members.subList(seatLimit, members.size());
            for (AppUserEntity user : overflow) {
                user.setTeamId(null);
            }
            appUserRepository.saveAll(overflow);
            members = appUserRepository.findByTeamIdOrderByCreatedAtAsc(team.getId());
        }
        team.setSeatLimit(seatLimit);
        team.setMemberCount(members.size());
        return teamRepository.save(team);
    }

    private int calculateStreak(String memberId) {
        if (memberId == null || memberId.isBlank()) return 0;
        List<CheckinEntity> checkins = checkinRepository.findAllByMemberIdOrderByDateDesc(memberId);
        if (checkins.isEmpty()) return 0;
        java.util.Set<String> dates = new java.util.HashSet<>();
        for (CheckinEntity checkin : checkins) {
            if ("已打卡".equals(checkin.getStatus()) && checkin.getDate() != null) {
                dates.add(checkin.getDate());
            }
        }
        LocalDate cursor = LocalDate.now(CN_ZONE);
        int streak = 0;
        while (dates.contains(cursor.format(DATE_FORMATTER))) {
            streak += 1;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
