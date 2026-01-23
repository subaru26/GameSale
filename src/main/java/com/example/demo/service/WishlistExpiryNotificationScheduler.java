package com.example.demo.service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.entity.User;
import com.example.demo.entity.Wishlist;
import com.example.demo.repository.WishlistRepository;

@Component
public class WishlistExpiryNotificationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(WishlistExpiryNotificationScheduler.class);

    private final WishlistRepository wishlistRepository;
    private final UserService userService;
    private final MailService mailService;

    public WishlistExpiryNotificationScheduler(
            WishlistRepository wishlistRepository,
            UserService userService,
            MailService mailService) {
        this.wishlistRepository = wishlistRepository;
        this.userService = userService;
        this.mailService = mailService;
    }

    // 10分おきにチェック（重複送信はDBのnotify_24h_sent_atで防止）
    @Scheduled(cron = "0 */10 * * * *")
    public void notifyExpiringWishlist() {
        try {
            Instant now = Instant.now();
            Instant from = now.plus(Duration.ofHours(24)).minus(Duration.ofMinutes(10));
            Instant to = now.plus(Duration.ofHours(24)).plus(Duration.ofMinutes(10));

            List<Wishlist> candidates = wishlistRepository.findExpiringBetween(from, to);
            if (candidates.isEmpty()) return;

            logger.info("Wishlist expiry candidates: {}", candidates.size());

            // ユーザーごとに通知ON/OFF判定（同一ユーザーの取得を少しだけまとめる）
            Set<Long> checkedUsers = new HashSet<>();

            for (Wishlist item : candidates) {
                Long userId = item.getUserId();
                if (userId == null) continue;

                User user = userService.findById(userId);
                if (user == null || user.getEmail() == null || user.getEmail().isBlank()) continue;
                if (!user.isWishlistNotifyEnabled()) continue;

                // 送信
                mailService.sendWishlistExpiryReminder(
                        user.getEmail(),
                        item.getGameTitle(),
                        item.getShop(),
                        item.getUrl(),
                        item.getExpiry()
                );

                boolean marked = wishlistRepository.markNotify24hSent(item.getId(), now);
                if (!marked) {
                    logger.warn("Failed to mark notify_24h_sent_at (may re-send): wishlistId={}", item.getId());
                }

                checkedUsers.add(userId);
            }

        } catch (Exception e) {
            logger.error("Wishlist expiry notification job failed", e);
        }
    }
}

