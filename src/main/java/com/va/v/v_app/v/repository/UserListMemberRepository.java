package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.UserListMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserListMemberRepository extends JpaRepository<UserListMember, Long> {
    List<UserListMember> findByUserListId(Long listId);
    Optional<UserListMember> findByUserListIdAndUserId(Long listId, String userId);
    void deleteByUserListIdAndUserId(Long listId, String userId);
    long countByUserListId(Long listId);

    @Query("SELECT m.userId FROM UserListMember m WHERE m.userList.id = :listId")
    List<String> findUserIdsByListId(Long listId);
}
