package com.va.v.v_app.v.service;

import com.va.v.v_app.v.model.UserStreak;
import com.va.v.v_app.v.repository.UserStreakRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Gamification service for engagement streaks, XP, and badges.
 * 
 * Streak Rules:
 * - A "qualifying action" is: create post, like, comment, or share
 * - Performing any qualifying action marks the user as "active" for that day
 * - Consecutive active days build a streak
 * - Missing a day resets the current streak to 0
 * - Longest streak is always preserved
 * 
 * XP System:
 * - Post created: +20 XP
 * - Like given: +2 XP
 * - Comment posted: +5 XP
 * - Each streak day: bonus +10 XP × streak_length
 * 
 * Level Calculation:
 * - Level = floor(sqrt(totalXP / 100)) + 1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationService {

    private final UserStreakRepository streakRepository;

    // XP rewards
    private static final int XP_POST = 20;
    private static final int XP_LIKE = 2;
    private static final int XP_COMMENT = 5;
    private static final int XP_STREAK_BONUS_MULTIPLIER = 10;

    /**
     * Record a user action and update their streak/XP.
     * 
     * @param userId     The user performing the action
     * @param actionType One of: POST, LIKE, COMMENT, SHARE
     */
    @Transactional
    public Map<String, Object> recordAction(String userId, String actionType) {
        UserStreak streak = streakRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserStreak newStreak = UserStreak.builder()
                            .userId(userId)
                            .build();
                    return streakRepository.save(newStreak);
                });

        LocalDate today = LocalDate.now();

        // Reset daily counters if it's a new day
        if (streak.getLastDailyReset() == null || !streak.getLastDailyReset().equals(today)) {
            streak.setDailyPosts(0);
            streak.setDailyLikes(0);
            streak.setDailyComments(0);
            streak.setLastDailyReset(today);
        }

        // Award XP based on action type
        int xpAwarded = switch (actionType.toUpperCase()) {
            case "POST" -> {
                streak.setDailyPosts(streak.getDailyPosts() + 1);
                yield XP_POST;
            }
            case "LIKE" -> {
                streak.setDailyLikes(streak.getDailyLikes() + 1);
                yield XP_LIKE;
            }
            case "COMMENT" -> {
                streak.setDailyComments(streak.getDailyComments() + 1);
                yield XP_COMMENT;
            }
            default -> 1;
        };

        // Update streak
        boolean streakUpdated = false;
        if (streak.getLastActiveDate() == null) {
            // First ever action
            streak.setCurrentStreak(1);
            streak.setTotalActiveDays(1);
            streakUpdated = true;
        } else if (streak.getLastActiveDate().equals(today.minusDays(1))) {
            // Consecutive day — streak continues
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
            streak.setTotalActiveDays(streak.getTotalActiveDays() + 1);
            streakUpdated = true;
        } else if (!streak.getLastActiveDate().equals(today)) {
            // Day was skipped — reset streak
            streak.setCurrentStreak(1);
            streak.setTotalActiveDays(streak.getTotalActiveDays() + 1);
            streakUpdated = true;
        }
        // If lastActiveDate == today, streak was already counted today

        if (streakUpdated) {
            streak.setLastActiveDate(today);

            // Streak bonus XP
            int streakBonus = streak.getCurrentStreak() * XP_STREAK_BONUS_MULTIPLIER;
            xpAwarded += streakBonus;

            // Update longest streak
            if (streak.getCurrentStreak() > streak.getLongestStreak()) {
                streak.setLongestStreak(streak.getCurrentStreak());
            }
        }

        // Add XP and recalculate level
        streak.setXpPoints(streak.getXpPoints() + xpAwarded);
        streak.setLevel(calculateLevel(streak.getXpPoints()));

        streakRepository.save(streak);

        // Check for new badges
        List<String> newBadges = checkForBadges(streak);

        log.debug("🎮 {} action by user {}: +{} XP, streak: {}, level: {}",
                actionType, userId, xpAwarded, streak.getCurrentStreak(), streak.getLevel());

        return Map.of(
                "xpAwarded", xpAwarded,
                "totalXp", streak.getXpPoints(),
                "currentStreak", streak.getCurrentStreak(),
                "longestStreak", streak.getLongestStreak(),
                "level", streak.getLevel(),
                "newBadges", newBadges);
    }

    /**
     * Get a user's full gamification stats.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStats(String userId) {
        UserStreak streak = streakRepository.findByUserId(userId)
                .orElse(UserStreak.builder().userId(userId).build());

        // Check if streak is still valid (not broken)
        LocalDate today = LocalDate.now();
        int displayStreak = streak.getCurrentStreak();
        if (streak.getLastActiveDate() != null
                && !streak.getLastActiveDate().equals(today)
                && !streak.getLastActiveDate().equals(today.minusDays(1))) {
            displayStreak = 0; // Streak is broken but not yet saved
        }

        int nextLevelXp = (int) Math.pow((streak.getLevel()) * 10.0, 2);
        double progress = streak.getXpPoints() > 0
                ? Math.min((double) streak.getXpPoints() / nextLevelXp * 100, 100)
                : 0;

        return Map.of(
                "currentStreak", displayStreak,
                "longestStreak", streak.getLongestStreak(),
                "totalActiveDays", streak.getTotalActiveDays(),
                "xpPoints", streak.getXpPoints(),
                "level", streak.getLevel(),
                "levelProgress", Math.round(progress * 10.0) / 10.0,
                "nextLevelXp", nextLevelXp,
                "dailyStats", Map.of(
                        "posts", streak.getDailyPosts(),
                        "likes", streak.getDailyLikes(),
                        "comments", streak.getDailyComments()),
                "badges", getAllBadges(streak));
    }

    // ─── Helpers ───

    private int calculateLevel(long xp) {
        return (int) Math.floor(Math.sqrt((double) xp / 100)) + 1;
    }

    private List<String> checkForBadges(UserStreak streak) {
        List<String> newBadges = new ArrayList<>();

        if (streak.getCurrentStreak() == 7)
            newBadges.add("🔥 Week Warrior");
        if (streak.getCurrentStreak() == 30)
            newBadges.add("💎 Monthly Legend");
        if (streak.getCurrentStreak() == 100)
            newBadges.add("🏆 Century Club");
        if (streak.getCurrentStreak() == 365)
            newBadges.add("👑 Year-Long Devotee");
        if (streak.getXpPoints() >= 1000)
            newBadges.add("⭐ Rising Star");
        if (streak.getXpPoints() >= 10000)
            newBadges.add("🌟 Superstar");
        if (streak.getXpPoints() >= 100000)
            newBadges.add("💫 Legendary");
        if (streak.getLevel() >= 10)
            newBadges.add("🎯 Level 10 Achiever");
        if (streak.getLevel() >= 50)
            newBadges.add("🏅 Elite Creator");
        if (streak.getTotalActiveDays() >= 100)
            newBadges.add("📅 100-Day Veteran");

        return newBadges;
    }

    private List<Map<String, Object>> getAllBadges(UserStreak streak) {
        List<Map<String, Object>> badges = new ArrayList<>();

        addBadgeIfEarned(badges, "🔥", "Week Warrior", "7-day streak", streak.getLongestStreak() >= 7);
        addBadgeIfEarned(badges, "💎", "Monthly Legend", "30-day streak", streak.getLongestStreak() >= 30);
        addBadgeIfEarned(badges, "🏆", "Century Club", "100-day streak", streak.getLongestStreak() >= 100);
        addBadgeIfEarned(badges, "👑", "Year Devotee", "365-day streak", streak.getLongestStreak() >= 365);
        addBadgeIfEarned(badges, "⭐", "Rising Star", "1,000 XP earned", streak.getXpPoints() >= 1000);
        addBadgeIfEarned(badges, "🌟", "Superstar", "10,000 XP earned", streak.getXpPoints() >= 10000);
        addBadgeIfEarned(badges, "💫", "Legendary", "100,000 XP earned", streak.getXpPoints() >= 100000);
        addBadgeIfEarned(badges, "🎯", "Level 10", "Reach level 10", streak.getLevel() >= 10);
        addBadgeIfEarned(badges, "🏅", "Elite Creator", "Reach level 50", streak.getLevel() >= 50);
        addBadgeIfEarned(badges, "📅", "100-Day Veteran", "100 total active days", streak.getTotalActiveDays() >= 100);

        return badges;
    }

    private void addBadgeIfEarned(List<Map<String, Object>> badges, String icon, String name,
            String description, boolean earned) {
        badges.add(Map.of(
                "icon", icon,
                "name", name,
                "description", description,
                "earned", earned));
    }
}
