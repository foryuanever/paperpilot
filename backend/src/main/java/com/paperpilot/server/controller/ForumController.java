package com.paperpilot.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.entity.ForumPostEntity;
import com.paperpilot.server.entity.ForumReplyEntity;
import com.paperpilot.server.repository.AppUserRepository;
import com.paperpilot.server.repository.ForumPostRepository;
import com.paperpilot.server.repository.ForumReplyRepository;
import com.paperpilot.server.service.CurrentUserService;
import com.paperpilot.server.service.ForumModerationService;
import com.paperpilot.server.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/forum")
public class ForumController {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final ForumPostRepository forumPostRepository;
    private final ForumReplyRepository forumReplyRepository;
    private final AppUserRepository appUserRepository;
    private final ForumModerationService moderationService;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public ForumController(
        ForumPostRepository forumPostRepository,
        ForumReplyRepository forumReplyRepository,
        AppUserRepository appUserRepository,
        ForumModerationService moderationService,
        CurrentUserService currentUserService,
        NotificationService notificationService,
        ObjectMapper objectMapper
    ) {
        this.forumPostRepository = forumPostRepository;
        this.forumReplyRepository = forumReplyRepository;
        this.appUserRepository = appUserRepository;
        this.moderationService = moderationService;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/posts")
    public List<Map<String, Object>> getPosts() {
        AppUserEntity currentUser = currentUserService.getOrCreateDefaultUser();
        return forumPostRepository.findAllByOrderByCreatedAtDesc().stream()
            .filter(post -> !post.isBanned() || isAdmin(currentUser) || isOwner(post, currentUser))
            .map(post -> toMap(post, currentUser))
            .sorted((a, b) -> Boolean.compare(Boolean.TRUE.equals(b.get("pinned")), Boolean.TRUE.equals(a.get("pinned"))))
            .toList();
    }

    @PostMapping("/posts")
    public Map<String, Object> createPost(@RequestBody Map<String, Object> body) {
        ForumModerationService.ModerationResult moderation = moderationService.review(body);
        if (!moderation.approved()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, moderation.reason());
        }
        AppUserEntity currentUser = currentUserService.getOrCreateDefaultUser();
        ForumPostEntity post = new ForumPostEntity();
        applyPostFields(post, body);
        post.setUserId(currentUser.getId());
        post.setAuthor(defaultText(body, "author", currentUser.getUsername()));
        post.setAvatar(avatar("", post.getAuthor()));
        ForumPostEntity saved = forumPostRepository.save(post);
        return Map.of(
            "id", "post-" + saved.getId(),
            "title", saved.getTitle(),
            "moderation", Map.of("approved", true, "reason", moderation.reason(), "reviewer", moderation.reviewer())
        );
    }

    @PatchMapping("/posts/{id}")
    public Map<String, Object> updatePost(@PathVariable String id, @RequestBody Map<String, Object> body) {
        AppUserEntity currentUser = currentUserService.getOrCreateDefaultUser();
        ForumPostEntity post = findPost(id);
        ensureOwner(post, currentUser);
        ForumModerationService.ModerationResult moderation = moderationService.review(body);
        if (!moderation.approved()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, moderation.reason());
        }
        applyPostFields(post, body);
        forumPostRepository.save(post);
        return Map.of("message", "帖子已更新", "reviewer", moderation.reviewer());
    }

    @DeleteMapping("/posts/{id}")
    @Transactional
    public void deletePost(@PathVariable String id) {
        AppUserEntity currentUser = currentUserService.getOrCreateDefaultUser();
        ForumPostEntity post = findPost(id);
        ensureOwner(post, currentUser);
        forumReplyRepository.deleteAllByPostId(post.getId());
        forumPostRepository.delete(post);
    }

    @PostMapping("/posts/review")
    public Map<String, Object> reviewPost(@RequestBody Map<String, Object> body) {
        ForumModerationService.ModerationResult moderation = moderationService.review(body);
        return Map.of("approved", moderation.approved(), "reason", moderation.reason(), "reviewer", moderation.reviewer());
    }

    @PostMapping("/posts/{id}/like")
    public void likePost(@PathVariable String id) {
        AppUserEntity actor = currentUserService.getOrCreateDefaultUser();
        ForumPostEntity post = findPost(id);
        post.setHasLiked(!post.isHasLiked());
        post.setLikes(Math.max(0, value(post.getLikes()) + (post.isHasLiked() ? 1 : -1)));
        forumPostRepository.save(post);
        if (post.isHasLiked()) {
            notificationService.create(post.getUserId(), actor.getId(), "forum_like", post.getId(),
                "你的帖子收到了赞同", actor.getUsername() + " 赞同了《" + post.getTitle() + "》");
        }
    }

    @PostMapping("/posts/{id}/bookmark")
    public void bookmarkPost(@PathVariable String id) {
        AppUserEntity actor = currentUserService.getOrCreateDefaultUser();
        ForumPostEntity post = findPost(id);
        post.setHasBookmarked(!post.isHasBookmarked());
        post.setBookmarks(Math.max(0, value(post.getBookmarks()) + (post.isHasBookmarked() ? 1 : -1)));
        forumPostRepository.save(post);
        if (post.isHasBookmarked()) {
            notificationService.create(post.getUserId(), actor.getId(), "forum_bookmark", post.getId(),
                "你的帖子被收藏", actor.getUsername() + " 收藏了《" + post.getTitle() + "》");
        }
    }

    @PostMapping("/posts/{id}/reply")
    public Map<String, Object> replyPost(@PathVariable String id, @RequestBody Map<String, Object> body) {
        AppUserEntity actor = currentUserService.getOrCreateDefaultUser();
        ForumPostEntity post = findPost(id);
        String content = text(body, "content");
        if (content.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "回复内容不能为空");
        ForumReplyEntity reply = new ForumReplyEntity();
        reply.setPostId(post.getId());
        reply.setUserId(actor.getId());
        reply.setContent(content);
        reply.setAuthor(defaultText(body, "author", actor.getUsername()));
        reply.setAvatar(avatar("", reply.getAuthor()));
        reply.setReplyToReplyId(text(body, "replyToReplyId"));
        reply.setReplyToAuthor(text(body, "replyToAuthor"));
        ForumReplyEntity saved = forumReplyRepository.save(reply);
        Long receiverId = post.getUserId();
        String replyToId = text(body, "replyToReplyId");
        if (StringUtils.hasText(replyToId)) {
            forumReplyRepository.findById(parseId(replyToId, "reply-"))
                .map(ForumReplyEntity::getUserId)
                .ifPresent(value -> {
                    if (value != null) notificationService.create(value, actor.getId(), "forum_reply", post.getId(),
                        "你的评论收到回复", reply.getAuthor() + " 回复了你在《" + post.getTitle() + "》下的评论");
                });
        }
        if (receiverId != null && !Objects.equals(receiverId, actor.getId())) {
            notificationService.create(receiverId, actor.getId(), "forum_reply", post.getId(),
                "你的帖子收到新评论", reply.getAuthor() + " 评论了《" + post.getTitle() + "》");
        }
        return Map.of("id", "reply-" + saved.getId());
    }

    @PostMapping("/posts/{id}/pin")
    public Map<String, Object> togglePin(@PathVariable String id) {
        AppUserEntity actor = currentUserService.getOrCreateDefaultUser();
        ensureAdmin(actor);
        ForumPostEntity post = findPost(id);
        post.setPinned(!post.isPinned());
        forumPostRepository.save(post);
        if (post.getUserId() != null) {
            notificationService.create(post.getUserId(), actor.getId(), "forum_pin", post.getId(),
                post.isPinned() ? "你的帖子已被置顶" : "你的帖子已取消置顶",
                "管理员" + (post.isPinned() ? "置顶了" : "取消置顶了") + "《" + post.getTitle() + "》");
        }
        return Map.of("pinned", post.isPinned());
    }

    @PostMapping("/posts/{id}/ban")
    public Map<String, Object> toggleBan(@PathVariable String id) {
        AppUserEntity actor = currentUserService.getOrCreateDefaultUser();
        ensureAdmin(actor);
        ForumPostEntity post = findPost(id);
        post.setBanned(!post.isBanned());
        forumPostRepository.save(post);
        if (post.getUserId() != null) {
            notificationService.create(post.getUserId(), actor.getId(), "forum_ban", post.getId(),
                post.isBanned() ? "你的帖子已被封禁" : "你的帖子已解除封禁",
                "管理员" + (post.isBanned() ? "封禁了" : "解除封禁了") + "《" + post.getTitle() + "》");
        }
        return Map.of("banned", post.isBanned());
    }

    @PostMapping("/posts/{postId}/reply/{replyId}/like")
    public void likeReply(@PathVariable String postId, @PathVariable String replyId) {
        AppUserEntity actor = currentUserService.getOrCreateDefaultUser();
        ForumReplyEntity reply = forumReplyRepository.findById(parseId(replyId, "reply-"))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "回复不存在"));
        reply.setHasLiked(!reply.isHasLiked());
        reply.setLikes(Math.max(0, value(reply.getLikes()) + (reply.isHasLiked() ? 1 : -1)));
        forumReplyRepository.save(reply);
        if (reply.isHasLiked()) {
            notificationService.create(reply.getUserId(), actor.getId(), "forum_reply_like", reply.getId(),
                "你的评论收到了赞同", actor.getUsername() + " 赞同了你的评论");
        }
    }

    private Map<String, Object> toMap(ForumPostEntity post, AppUserEntity currentUser) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", "post-" + post.getId());
        map.put("title", post.getTitle());
        map.put("author", post.getAuthor());
        map.put("authorUserId", resolveUserId(post.getUserId(), post.getAuthor()));
        map.put("avatar", avatar(post.getAvatar(), post.getAuthor()));
        map.put("postType", fallback(post.getPostType(), inferPostType(post)));
        map.put("direction", fallback(post.getResearchArea(), "人工智能"));
        map.put("discipline", fallback(post.getDiscipline(), "计算机科学"));
        map.put("researchArea", fallback(post.getResearchArea(), "人工智能"));
        map.put("tags", splitTags(post.getTags()));
        map.put("paperTitle", post.getPaperTitle());
        map.put("publishYear", post.getPublishYear());
        map.put("venueName", post.getVenueName());
        map.put("venueLevel", post.getVenueLevel());
        map.put("resourceLink", post.getResourceLink());
        map.put("images", readAttachments(post.getImagesJson()));
        map.put("attachments", readAttachments(post.getAttachmentsJson()));
        map.put("content", post.getContent());
        map.put("likes", value(post.getLikes()));
        map.put("bookmarks", value(post.getBookmarks()));
        map.put("views", value(post.getViews()));
        map.put("hasLiked", post.isHasLiked());
        map.put("hasBookmarked", post.isHasBookmarked());
        map.put("resolved", post.isResolved());
        map.put("canManage", isOwner(post, currentUser));
        map.put("canAdminManage", isAdmin(currentUser));
        map.put("pinned", post.isPinned());
        map.put("banned", post.isBanned());
        map.put("time", post.getCreatedAt().format(FORMATTER));
        List<Map<String, Object>> replies = new ArrayList<>();
        for (ForumReplyEntity reply : forumReplyRepository.findAllByPostIdOrderByCreatedAtAsc(post.getId())) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", "reply-" + reply.getId());
            item.put("author", reply.getAuthor());
            item.put("authorUserId", resolveUserId(reply.getUserId(), reply.getAuthor()));
            item.put("avatar", avatar(reply.getAvatar(), reply.getAuthor()));
            item.put("content", reply.getContent());
            item.put("replyToReplyId", reply.getReplyToReplyId());
            item.put("replyToAuthor", reply.getReplyToAuthor());
            item.put("likes", value(reply.getLikes()));
            item.put("hasLiked", reply.isHasLiked());
            item.put("time", reply.getCreatedAt().format(FORMATTER));
            replies.add(item);
        }
        map.put("replies", replies);
        return map;
    }

    private void applyPostFields(ForumPostEntity post, Map<String, Object> body) {
        String title = text(body, "title");
        String content = text(body, "content");
        if (!StringUtils.hasText(title) || !StringUtils.hasText(content) || content.length() <= 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "标题不能为空，内容需要大于 5 个字");
        }
        post.setTitle(title);
        post.setContent(content);
        post.setPostType(defaultText(body, "postType", "研究讨论"));
        post.setResearchArea(defaultText(body, "direction", defaultText(body, "researchArea", "人工智能")));
        post.setDiscipline(defaultText(body, "discipline", "综合研究"));
        post.setTags(joinTags(body.get("tags")));
        post.setPaperTitle(text(body, "paperTitle"));
        post.setPublishYear(defaultText(body, "publishYear", String.valueOf(Year.now().getValue())));
        post.setVenueName(text(body, "venueName"));
        post.setVenueLevel(text(body, "venueLevel"));
        post.setResourceLink(text(body, "resourceLink"));
        post.setImagesJson(writeJson(body.getOrDefault("images", List.of())));
        post.setAttachmentsJson(writeJson(body.getOrDefault("attachments", List.of())));
    }

    private void ensureOwner(ForumPostEntity post, AppUserEntity currentUser) {
        if (!isOwner(post, currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能管理自己发布的帖子");
        }
        if (post.getUserId() == null) post.setUserId(currentUser.getId());
    }

    private boolean isOwner(ForumPostEntity post, AppUserEntity currentUser) {
        return Objects.equals(post.getUserId(), currentUser.getId())
            || (post.getUserId() == null && Objects.equals(post.getAuthor(), currentUser.getUsername()));
    }

    private boolean isAdmin(AppUserEntity user) {
        return user != null && "管理员".equals(user.getRole());
    }

    private void ensureAdmin(AppUserEntity user) {
        if (!isAdmin(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅管理员可管理论坛帖子");
        }
    }

    private ForumPostEntity findPost(String id) {
        return forumPostRepository.findById(parseId(id, "post-"))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "帖子不存在"));
    }

    private List<Map<String, Object>> readAttachments(String json) {
        if (!StringUtils.hasText(json)) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception ignored) { return List.of(); }
    }

    private String writeJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception ignored) { return "[]"; }
    }

    private String inferPostType(ForumPostEntity post) {
        String value = (post.getTitle() + " " + post.getContent()).toLowerCase();
        if (value.contains("数据集") || value.contains("dataset")) return "数据集求助";
        if (value.contains("期刊") || value.contains("论文")) return "论文期刊";
        if (value.contains("优惠") || value.contains("免费") || value.contains("资源")) return "科研羊毛";
        return "研究讨论";
    }

    private List<String> splitTags(String tags) {
        if (!StringUtils.hasText(tags)) return List.of();
        return Arrays.stream(tags.split(",")).map(String::trim).filter(item -> !item.isBlank()).distinct().toList();
    }

    private String joinTags(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).map(String::trim).filter(item -> !item.isBlank())
                .distinct().limit(8).reduce((a, b) -> a + "," + b).orElse("");
        }
        return value == null ? "" : String.valueOf(value);
    }

    private int value(Integer number) { return number == null ? 0 : number; }
    private Long resolveUserId(Long userId, String author) {
        if (userId != null && userId > 0 && appUserRepository.existsById(userId)) return userId;
        if (!StringUtils.hasText(author)) return null;
        return appUserRepository.findByUsername(author.trim()).map(AppUserEntity::getId).orElse(null);
    }
    private String fallback(String value, String fallback) { return StringUtils.hasText(value) ? value : fallback; }
    private String text(Map<String, Object> body, String key) { return body.get(key) == null ? "" : String.valueOf(body.get(key)).trim(); }
    private String defaultText(Map<String, Object> body, String key, String fallback) { return fallback(text(body, key), fallback); }
    private String avatar(String value, String author) { return fallback(value, !StringUtils.hasText(author) ? "U" : author.substring(0, 1).toUpperCase()); }

    private Long parseId(String value, String prefix) {
        try { return Long.parseLong(value.startsWith(prefix) ? value.substring(prefix.length()) : value); }
        catch (NumberFormatException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的ID格式"); }
    }
}
