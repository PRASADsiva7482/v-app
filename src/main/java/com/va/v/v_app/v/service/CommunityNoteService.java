package com.va.v.v_app.v.service;

import com.va.v.v_app.v.exception.ResourceNotFoundException;
import com.va.v.v_app.v.model.CommunityNote;
import com.va.v.v_app.v.model.CommunityNoteVote;
import com.va.v.v_app.v.model.Post;
import com.va.v.v_app.v.repository.CommunityNoteRepository;
import com.va.v.v_app.v.repository.CommunityNoteVoteRepository;
import com.va.v.v_app.v.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityNoteService {

    private final CommunityNoteRepository communityNoteRepository;
    private final CommunityNoteVoteRepository communityNoteVoteRepository;
    private final PostRepository postRepository;

    private static final int APPROVAL_THRESHOLD = 5; // net upvotes needed

    @Transactional
    public Map<String, Object> createNote(Long postId, String authorId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        CommunityNote note = CommunityNote.builder()
                .post(post)
                .authorId(authorId)
                .content(content)
                .build();
        CommunityNote saved = communityNoteRepository.save(note);
        return mapNote(saved);
    }

    public List<Map<String, Object>> getNotesForPost(Long postId) {
        return communityNoteRepository.findByPostIdOrderByCreatedAtDesc(postId)
                .stream().map(this::mapNote).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getApprovedNotesForPost(Long postId) {
        return communityNoteRepository.findByPostIdAndStatusOrderByCreatedAtDesc(postId, "APPROVED")
                .stream().map(this::mapNote).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> voteOnNote(Long noteId, String userId, String voteType) {
        CommunityNote note = communityNoteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Community note not found"));

        // Check if user already voted
        var existingVote = communityNoteVoteRepository.findByNoteIdAndUserId(noteId, userId);
        if (existingVote.isPresent()) {
            // Change vote
            CommunityNoteVote vote = existingVote.get();
            String oldType = vote.getVoteType();
            vote.setVoteType(voteType);
            communityNoteVoteRepository.save(vote);

            // Adjust counts
            if ("UP".equals(oldType)) note.setUpvotes(note.getUpvotes() - 1);
            else note.setDownvotes(note.getDownvotes() - 1);
        } else {
            CommunityNoteVote vote = CommunityNoteVote.builder()
                    .note(note).userId(userId).voteType(voteType).build();
            communityNoteVoteRepository.save(vote);
        }

        if ("UP".equals(voteType)) note.setUpvotes(note.getUpvotes() + 1);
        else note.setDownvotes(note.getDownvotes() + 1);

        // Auto-approve if threshold met
        int netScore = note.getUpvotes() - note.getDownvotes();
        if (netScore >= APPROVAL_THRESHOLD && "PENDING".equals(note.getStatus())) {
            note.setStatus("APPROVED");
        } else if (netScore <= -APPROVAL_THRESHOLD && "PENDING".equals(note.getStatus())) {
            note.setStatus("REJECTED");
        }

        communityNoteRepository.save(note);
        return mapNote(note);
    }

    private Map<String, Object> mapNote(CommunityNote note) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", note.getId());
        map.put("postId", note.getPost().getId());
        map.put("authorId", note.getAuthorId());
        map.put("content", note.getContent());
        map.put("upvotes", note.getUpvotes());
        map.put("downvotes", note.getDownvotes());
        map.put("status", note.getStatus());
        map.put("createdAt", note.getCreatedAt());
        return map;
    }
}
