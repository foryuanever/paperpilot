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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final ZoneId CN_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int BASE_TEAM_SEATS = 8;
    private static final int TEAM_MEMBER_SEATS = 20;

    public TeamDataController(
        AppUserRepository appUserRepository,
        ResearchTaskRepository researchTaskRepository,
        AnnouncementRepository announcementRepository,
        SharedResourceRepository sharedResourceRepository,
        CheckinRepository checkinRepository,
        UserNotificationRepository userNotificationRepository,
        TeamRepository teamRepository,
        com.paperpilot.server.service.AuthService authService
    ) {
        this.appUserRepository = appUserRepository;
        this.researchTaskRepository = researchTaskRepository;
        this.announcementRepository = announcementRepository;
        this.sharedResourceRepository = sharedResourceRepository;
        this.checkinRepository = checkinRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.teamRepository = teamRepository;
        this.authService = authService;
    }

    // --- Members ---

    @GetMapping("/info")
    public TeamEntity getTeamInfo() {
        TeamEntity team = teamRepository.findAll().stream()
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "尚未建立科研团队"));
        int effectiveSeatLimit = effectiveSeatLimit(team);
        if (team.getSeatLimit() == null || team.getSeatLimit() != effectiveSeatLimit) {
            team.setSeatLimit(effectiveSeatLimit);
            teamRepository.save(team);
        }
        return team;
    }

    @GetMapping("/members")
    public List<Map<String, Object>> getMembers() {
        TeamEntity team = getTeamInfo();
        List<AppUserEntity> users = appUserRepository.findByTeamIdOrderByCreatedAtAsc(team.getId());
        List<Map<String, Object>> result = new ArrayList<>();

        for (AppUserEntity user : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", "m-" + user.getId());
            map.put("name", user.getUsername());
            map.put("email", user.getEmail());
            map.put("role", user.getRole() != null ? user.getRole() : "学生");
            // Check if user logged in recently or just make them online
            map.put("status", "online"); 
            map.put("tokenUsed", user.getTokenUsed() != null ? user.getTokenUsed() : 0L);
            map.put("tokenLimit", user.getTokenLimit() != null ? user.getTokenLimit() : 5000000L);
            map.put("activeTime", user.getActiveTime() != null ? user.getActiveTime() : 0L);
            map.put("registerTime", user.getCreatedAt() != null ? user.getCreatedAt().format(DATE_FORMATTER) : "2026-06-08");
            result.add(map);
        }
        return result;
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

        TeamEntity team = getTeamInfo();
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
        user.setPlainPassword(defaultPw);
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
        Long id = parseId(idStr);
        authService.adminDeleteUser(id, "127.0.0.1");
    }

    @PatchMapping("/members/{id}/quota")
    public void updateMemberQuota(@PathVariable("id") String idStr, @RequestBody Map<String, Object> body) {
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
            .filter(user -> "学生".equals(user.getRole()) || "特权用户".equals(user.getRole()))
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
            map.put("streak", calculateStreak(c.getMemberId()));
            result.add(map);
        }
        return result;
    }

    @PostMapping("/checkins")
    public Map<String, Object> addCheckin(@RequestBody Map<String, Object> body) {
        String memberId = (String) body.get("memberId");
        String status = (String) body.get("status");

        if (memberId == null || memberId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "成员ID不能为空");
        }

        String today = LocalDate.now(CN_ZONE).format(DATE_FORMATTER);
        CheckinEntity checkin = checkinRepository.findByMemberIdAndDate(memberId, today)
            .orElse(new CheckinEntity());
            
        checkin.setMemberId(memberId);
        checkin.setDate(today);
        checkin.setStatus(status != null ? status : "已打卡");
        checkin.setTime(LocalDateTime.now(CN_ZONE).format(TIME_FORMATTER));

        CheckinEntity saved = checkinRepository.save(checkin);
        
        Map<String, Object> res = new HashMap<>();
        res.put("memberId", saved.getMemberId());
        res.put("status", saved.getStatus());
        res.put("time", saved.getTime());
        res.put("streak", calculateStreak(saved.getMemberId()));
        return res;
    }

    private int effectiveSeatLimit(TeamEntity team) {
        if (team == null || team.getId() == null) return BASE_TEAM_SEATS;
        LocalDateTime now = LocalDateTime.now(CN_ZONE);
        boolean hasTeamPlan = appUserRepository.findByTeamIdOrderByCreatedAtAsc(team.getId()).stream()
            .anyMatch(user -> "导师".equals(user.getRole())
                && "team".equals(user.getMembershipPlan())
                && user.getMembershipExpiresAt() != null
                && user.getMembershipExpiresAt().isAfter(now));
        return hasTeamPlan ? TEAM_MEMBER_SEATS : BASE_TEAM_SEATS;
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
