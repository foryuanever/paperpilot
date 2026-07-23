package com.paperpilot.server.controller;

import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.entity.CampusVerificationEntity;
import com.paperpilot.server.repository.AppUserRepository;
import com.paperpilot.server.repository.CampusVerificationRepository;
import com.paperpilot.server.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/campus-verification")
public class CampusVerificationController {

    private static final int MAX_CARD_IMAGE_LENGTH = 5_600_000;

    private final AppUserRepository appUserRepository;
    private final CampusVerificationRepository campusVerificationRepository;
    private final CurrentUserService currentUserService;

    public CampusVerificationController(
        AppUserRepository appUserRepository,
        CampusVerificationRepository campusVerificationRepository,
        CurrentUserService currentUserService
    ) {
        this.appUserRepository = appUserRepository;
        this.campusVerificationRepository = campusVerificationRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/me")
    public Map<String, Object> getMyCampusVerification() {
        AppUserEntity user = currentUserService.getOrCreateDefaultUser();
        Map<String, Object> result = new HashMap<>();
        result.put("schoolName", user.getSchoolName());
        result.put("campusVerified", user.isCampusVerified());
        campusVerificationRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId()).ifPresent(request -> {
            result.put("id", request.getId());
            result.put("status", request.getStatus());
            result.put("submittedSchoolName", request.getSchoolName());
            result.put("realName", request.getRealName());
            result.put("adminNote", request.getAdminNote());
            result.put("createdAt", request.getCreatedAt());
            result.put("reviewedAt", request.getReviewedAt());
        });
        return result;
    }

    @PostMapping("/submit")
    public Map<String, Object> submitCampusVerification(@RequestBody Map<String, Object> body) {
        AppUserEntity user = currentUserService.getOrCreateDefaultUser();
        String schoolName = requiredText(body.get("schoolName"), "请填写学校名称");
        String realName = requiredText(body.get("realName"), "请填写真实姓名");
        String front = requiredImage(body.get("studentCardFront"), "请上传学生证正面");
        String back = requiredImage(body.get("chsiScreenshot"), "请上传学信网截图");

        CampusVerificationEntity request = new CampusVerificationEntity();
        request.setUserId(user.getId());
        request.setUserName(user.getUsername());
        request.setEmail(user.getEmail());
        request.setSchoolName(limit(schoolName, 128, "学校名称过长"));
        request.setRealName(limit(realName, 64, "姓名过长"));
        request.setStudentCardFront(front);
        request.setChsiScreenshot(back);
        request.setStatus("pending");
        request.setAdminNote("");
        CampusVerificationEntity saved = campusVerificationRepository.save(request);

        return Map.of(
            "id", saved.getId(),
            "status", saved.getStatus(),
            "schoolName", saved.getSchoolName(),
            "campusVerified", user.isCampusVerified()
        );
    }

    private String requiredText(Object value, String message) {
        String text = value == null ? "" : String.valueOf(value).trim();
        if (text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return text;
    }

    private String requiredImage(Object value, String message) {
        String text = requiredText(value, message);
        if (!text.startsWith("data:image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传图片格式的学生证");
        }
        if (text.length() > MAX_CARD_IMAGE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学生证图片过大，请压缩后再上传");
        }
        return text;
    }

    private String limit(String value, int max, String message) {
        if (value.length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value;
    }
}
