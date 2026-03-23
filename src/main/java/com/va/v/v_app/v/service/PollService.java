package com.va.v.v_app.v.service;

import com.va.v.v_app.v.dto.request.CreatePollRequest;
import com.va.v.v_app.v.dto.response.PollOptionResponse;
import com.va.v.v_app.v.dto.response.PollResponse;
import com.va.v.v_app.v.exception.BusinessException;
import com.va.v.v_app.v.exception.ResourceNotFoundException;
import com.va.v.v_app.v.model.Poll;
import com.va.v.v_app.v.model.PollOption;
import com.va.v.v_app.v.model.PollVote;
import com.va.v.v_app.v.model.Post;
import com.va.v.v_app.v.repository.PollOptionRepository;
import com.va.v.v_app.v.repository.PollRepository;
import com.va.v.v_app.v.repository.PollVoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing polls.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PollService {

    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;

    /**
     * Create a poll for a post
     */
    @Transactional
    public Poll createPoll(Post post, CreatePollRequest request) {
        if (pollRepository.existsByPostId(post.getId())) {
            throw new BusinessException("POLL_EXISTS", "This post already has a poll");
        }

        // Calculate expiration
        int hours = request.getDurationHours() != null ? request.getDurationHours() : 24;
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(hours);

        Poll poll = Poll.builder()
                .post(post)
                .question(request.getQuestion())
                .durationHours(hours)
                .expiresAt(expiresAt)
                .build();

        Poll savedPoll = pollRepository.save(poll);

        // Create options
        for (int i = 0; i < request.getOptions().size(); i++) {
            PollOption option = PollOption.builder()
                    .poll(savedPoll)
                    .optionText(request.getOptions().get(i))
                    .position(i)
                    .build();
            pollOptionRepository.save(option);
        }

        log.info("Created poll ID: {} for post ID: {} with {} options, expires at {}",
                savedPoll.getId(), post.getId(), request.getOptions().size(), expiresAt);

        return savedPoll;
    }

    /**
     * Vote on a poll
     */
    @Transactional
    public PollResponse vote(Long pollId, Long optionId, String userId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException("Poll", "id", pollId));

        // Check if poll is expired or closed
        if (poll.getIsClosed() || LocalDateTime.now().isAfter(poll.getExpiresAt())) {
            throw new BusinessException("POLL_CLOSED", "This poll is no longer accepting votes");
        }

        // Check if user already voted
        if (pollVoteRepository.existsByPollIdAndUserId(pollId, userId)) {
            throw new BusinessException("ALREADY_VOTED", "You have already voted on this poll");
        }

        // Verify option belongs to this poll
        PollOption option = pollOptionRepository.findById(optionId)
                .orElseThrow(() -> new ResourceNotFoundException("PollOption", "id", optionId));
        if (!option.getPoll().getId().equals(pollId)) {
            throw new BusinessException("INVALID_OPTION", "This option does not belong to the specified poll");
        }

        // Record vote
        PollVote vote = PollVote.builder()
                .pollId(pollId)
                .optionId(optionId)
                .userId(userId)
                .build();
        pollVoteRepository.save(vote);

        // Update counts
        pollOptionRepository.incrementVoteCount(optionId);
        poll.setTotalVotes(poll.getTotalVotes() + 1);
        pollRepository.save(poll);

        log.info("User {} voted option {} on poll {}", userId, optionId, pollId);

        return getPollResponse(pollId, userId);
    }

    /**
     * Get poll response for a post
     */
    @Transactional(readOnly = true)
    public PollResponse getPollForPost(Long postId, String currentUserId) {
        Poll poll = pollRepository.findByPostIdWithOptions(postId).orElse(null);
        if (poll == null) return null;

        return mapToResponse(poll, currentUserId);
    }

    /**
     * Get poll response by poll ID
     */
    @Transactional(readOnly = true)
    public PollResponse getPollResponse(Long pollId, String currentUserId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException("Poll", "id", pollId));
        return mapToResponse(poll, currentUserId);
    }

    /**
     * Map Poll entity to PollResponse
     */
    public PollResponse mapToResponse(Poll poll, String currentUserId) {
        boolean isExpired = LocalDateTime.now().isAfter(poll.getExpiresAt());

        // Check if user has voted
        Long votedOptionId = null;
        if (currentUserId != null) {
            PollVote userVote = pollVoteRepository.findByPollIdAndUserId(poll.getId(), currentUserId).orElse(null);
            if (userVote != null) {
                votedOptionId = userVote.getOptionId();
            }
        }

        int totalVotes = poll.getTotalVotes();

        List<PollOptionResponse> optionResponses = poll.getOptions().stream()
                .map(option -> PollOptionResponse.builder()
                        .id(option.getId())
                        .optionText(option.getOptionText())
                        .position(option.getPosition())
                        .voteCount(option.getVoteCount())
                        .percentage(totalVotes > 0
                                ? Math.round(option.getVoteCount() * 1000.0 / totalVotes) / 10.0
                                : 0.0)
                        .build())
                .collect(Collectors.toList());

        return PollResponse.builder()
                .id(poll.getId())
                .question(poll.getQuestion())
                .durationHours(poll.getDurationHours())
                .totalVotes(totalVotes)
                .isClosed(poll.getIsClosed())
                .isExpired(isExpired)
                .createdAt(poll.getCreatedAt())
                .expiresAt(poll.getExpiresAt())
                .options(optionResponses)
                .votedOptionId(votedOptionId)
                .build();
    }
}
