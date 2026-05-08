package com.va.v.v_app.social.api;

import com.va.v.v_app.social.service.UserListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/lists")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Lists", description = "User Lists / Curated Feeds APIs")
public class ListController {

    private final UserListService userListService;

    @Operation(summary = "Create a new list")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createList(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        String userId = authentication.getName();
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        Boolean isPrivate = (Boolean) body.getOrDefault("isPrivate", false);
        return ResponseEntity.ok(userListService.createList(userId, name, description, isPrivate));
    }

    @Operation(summary = "Get my lists")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getMyLists(Authentication authentication) {
        return ResponseEntity.ok(userListService.getMyLists(authentication.getName()));
    }

    @Operation(summary = "Get a user's public lists")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getUserLists(@PathVariable String userId) {
        return ResponseEntity.ok(userListService.getUserPublicLists(userId));
    }

    @Operation(summary = "Delete a list")
    @DeleteMapping("/{listId}")
    public ResponseEntity<Void> deleteList(@PathVariable Long listId, Authentication authentication) {
        userListService.deleteList(listId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add a member to list")
    @PostMapping("/{listId}/members/{memberId}")
    public ResponseEntity<Void> addMember(
            @PathVariable Long listId,
            @PathVariable String memberId,
            Authentication authentication) {
        userListService.addMember(listId, memberId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remove a member from list")
    @DeleteMapping("/{listId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long listId,
            @PathVariable String memberId,
            Authentication authentication) {
        userListService.removeMember(listId, memberId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get list member IDs")
    @GetMapping("/{listId}/members")
    public ResponseEntity<List<String>> getMembers(@PathVariable Long listId) {
        return ResponseEntity.ok(userListService.getListMemberIds(listId));
    }
}
