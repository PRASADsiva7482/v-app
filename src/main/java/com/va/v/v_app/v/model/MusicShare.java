package com.va.v.v_app.v.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "music_share", indexes = {
        @Index(name = "idx_music_user", columnList = "user_id"),
        @Index(name = "idx_music_post", columnList = "post_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MusicShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "song_title", nullable = false, length = 300)
    private String songTitle;

    @Column(name = "artist", nullable = false, length = 200)
    private String artist;

    @Column(name = "album_name", length = 300)
    private String albumName;

    @Column(name = "album_art_url", length = 500)
    private String albumArtUrl;

    @Column(name = "preview_url", length = 500)
    private String previewUrl;

    @Column(name = "spotify_url", length = 500)
    private String spotifyUrl;

    @Column(name = "apple_music_url", length = 500)
    private String appleMusicUrl;

    @Column(name = "platform", length = 30)
    @Builder.Default
    private String platform = "MANUAL";

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
