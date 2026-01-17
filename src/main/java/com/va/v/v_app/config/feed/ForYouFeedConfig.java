package com.va.v.v_app.config.feed;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for "For You" feed system
 * Loads from application-feature.yml → feature.social.for-you
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "feature.social.for-you")
public class ForYouFeedConfig {

    private RankingConfig ranking = new RankingConfig();
    private PersonalizationConfig personalization = new PersonalizationConfig();
    private CompositionConfig composition = new CompositionConfig();
    private CacheConfig cache = new CacheConfig();
    private JobsConfig jobs = new JobsConfig();
    private PerformanceConfig performance = new PerformanceConfig();
    private EdgeCasesConfig edgeCases = new EdgeCasesConfig();

    @Data
    public static class RankingConfig {
        private WeightsConfig weights = new WeightsConfig();
        private TimeDecayConfig timeDecay = new TimeDecayConfig();
        private RecencyBonusConfig recencyBonus = new RecencyBonusConfig();
        private MediaBonusConfig mediaBonus = new MediaBonusConfig();
    }

    @Data
    public static class WeightsConfig {
        private double likes = 3.0;
        private double comments = 5.0;
        private double shares = 7.0;
        private double views = 0.1;
        private double engagementVelocity = 10.0;
    }

    @Data
    public static class TimeDecayConfig {
        private boolean enabled = true;
        private double decayRate = 0.5;
        private int halfLifeHours = 24;
        private int maxAgeDays = 30;
    }

    @Data
    public static class RecencyBonusConfig {
        private double lastHour = 200.0;
        private double last6Hours = 150.0;
        private double last24Hours = 100.0;
        private double last7Days = 50.0;
        private double last30Days = 10.0;
    }

    @Data
    public static class MediaBonusConfig {
        private double hasImage = 20.0;
        private double hasVideo = 30.0;
        private double hasMultiple = 40.0;
    }

    @Data
    public static class PersonalizationConfig {
        private boolean enabled = true;
        private AffinityConfig affinity = new AffinityConfig();
        private InterestMatchingConfig interestMatching = new InterestMatchingConfig();
    }

    @Data
    public static class AffinityConfig {
        private double followingBoost = 1.5;
        private double mutualBoost = 2.0;
        private double interactionBoost = 1.3;
    }

    @Data
    public static class InterestMatchingConfig {
        private boolean enabled = true;
        private double hashtagMatchBoost = 50.0;
        private double topicMatchBoost = 30.0;
    }

    @Data
    public static class CompositionConfig {
        private int globalTrending = 40;
        private int following = 35;
        private int interestBased = 20;
        private int discovery = 5;
    }

    @Data
    public static class CacheConfig {
        private CacheSettingsConfig globalTrending = new CacheSettingsConfig();
        private CacheSettingsConfig userFeed = new CacheSettingsConfig();
        private CacheSettingsConfig postScores = new CacheSettingsConfig();
        private CacheSettingsConfig userInterests = new CacheSettingsConfig();
    }

    @Data
    public static class CacheSettingsConfig {
        private int ttlSeconds = 300;
        private int recalculateInterval = 300;
        private int maxCacheSize = 1000;
    }

    @Data
    public static class JobsConfig {
        private JobSettingsConfig trendingCalculator = new JobSettingsConfig();
        private JobSettingsConfig scoreUpdater = new JobSettingsConfig();
        private JobSettingsConfig coldStartHandler = new JobSettingsConfig();
    }

    @Data
    public static class JobSettingsConfig {
        private boolean enabled = true;
        private String cron = "0 */5 * * * *";
        private int batchSize = 1000;
    }

    @Data
    public static class PerformanceConfig {
        private int maxPostsToScore = 5000;
        private int minEngagementThreshold = 5;
        private boolean enableScoreCaching = true;
        private boolean enableParallelProcessing = true;
        private int parallelThreads = 4;
    }

    @Data
    public static class EdgeCasesConfig {
        private boolean newUserDefaultTrending = true;
        private int minFollowingForPersonalization = 5;
        private boolean fallbackToGlobal = true;
        private boolean filterNsfw = true;
        private boolean filterShadowbanned = true;
    }
}
