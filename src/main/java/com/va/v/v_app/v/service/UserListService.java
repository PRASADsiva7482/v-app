package com.va.v.v_app.v.service;

import com.va.v.v_app.v.exception.BusinessException;
import com.va.v.v_app.v.exception.ResourceNotFoundException;
import com.va.v.v_app.v.model.UserList;
import com.va.v.v_app.v.model.UserListMember;
import com.va.v.v_app.v.repository.UserListMemberRepository;
import com.va.v.v_app.v.repository.UserListRepository;
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
public class UserListService {

    private final UserListRepository userListRepository;
    private final UserListMemberRepository userListMemberRepository;

    @Transactional
    public Map<String, Object> createList(String ownerId, String name, String description, Boolean isPrivate) {
        UserList list = UserList.builder()
                .ownerId(ownerId)
                .name(name)
                .description(description)
                .isPrivate(isPrivate != null ? isPrivate : false)
                .build();
        UserList saved = userListRepository.save(list);
        return mapListToResponse(saved, 0L);
    }

    public List<Map<String, Object>> getMyLists(String userId) {
        List<UserList> lists = userListRepository.findByOwnerIdOrderByCreatedAtDesc(userId);
        return lists.stream().map(l -> mapListToResponse(l,
                userListMemberRepository.countByUserListId(l.getId()))).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getUserPublicLists(String userId) {
        List<UserList> lists = userListRepository.findByOwnerIdAndIsPrivateFalseOrderByCreatedAtDesc(userId);
        return lists.stream().map(l -> mapListToResponse(l,
                userListMemberRepository.countByUserListId(l.getId()))).collect(Collectors.toList());
    }

    @Transactional
    public void deleteList(Long listId, String userId) {
        UserList list = userListRepository.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found"));
        if (!list.getOwnerId().equals(userId)) {
            throw new BusinessException("FORBIDDEN", "You can only delete your own lists");
        }
        userListRepository.delete(list);
    }

    @Transactional
    public void addMember(Long listId, String memberId, String ownerId) {
        UserList list = userListRepository.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found"));
        if (!list.getOwnerId().equals(ownerId)) {
            throw new BusinessException("FORBIDDEN", "You can only modify your own lists");
        }
        if (userListMemberRepository.findByUserListIdAndUserId(listId, memberId).isPresent()) {
            throw new BusinessException("ALREADY_MEMBER", "User is already in this list");
        }
        UserListMember member = UserListMember.builder().userList(list).userId(memberId).build();
        userListMemberRepository.save(member);
    }

    @Transactional
    public void removeMember(Long listId, String memberId, String ownerId) {
        UserList list = userListRepository.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found"));
        if (!list.getOwnerId().equals(ownerId)) {
            throw new BusinessException("FORBIDDEN", "You can only modify your own lists");
        }
        userListMemberRepository.deleteByUserListIdAndUserId(listId, memberId);
    }

    public List<String> getListMemberIds(Long listId) {
        return userListMemberRepository.findUserIdsByListId(listId);
    }

    private Map<String, Object> mapListToResponse(UserList list, Long memberCount) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", list.getId());
        map.put("name", list.getName());
        map.put("description", list.getDescription());
        map.put("isPrivate", list.getIsPrivate());
        map.put("memberCount", memberCount);
        map.put("createdAt", list.getCreatedAt());
        return map;
    }
}
