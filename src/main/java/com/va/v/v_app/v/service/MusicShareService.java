package com.va.v.v_app.v.service;

import com.va.v.v_app.v.model.MusicShare;
import com.va.v.v_app.v.repository.MusicShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MusicShareService {

    private final MusicShareRepository musicShareRepository;

    public MusicShare shareMusic(MusicShare musicShare) {
        return musicShareRepository.save(musicShare);
    }

    public List<MusicShare> getUserShares(String userId) {
        return musicShareRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<MusicShare> getByPostId(Long postId) {
        return musicShareRepository.findByPostId(postId);
    }

    public List<MusicShare> getRecentShares() {
        return musicShareRepository.findTop20ByOrderByCreatedAtDesc();
    }

    public void deleteShare(Long id, String userId) {
        musicShareRepository.findById(id).ifPresent(share -> {
            if (share.getUserId().equals(userId)) {
                musicShareRepository.delete(share);
            }
        });
    }
}
