package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.UserList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserListRepository extends JpaRepository<UserList, Long> {
    List<UserList> findByOwnerIdOrderByCreatedAtDesc(String ownerId);
    List<UserList> findByOwnerIdAndIsPrivateFalseOrderByCreatedAtDesc(String ownerId);
}
