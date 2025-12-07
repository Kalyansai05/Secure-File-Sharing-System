package com.tsv.implementation.dao;

import com.tsv.implementation.model.FileMetadata;
import com.tsv.implementation.model.FileShare;
import com.tsv.implementation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileShareRepository extends JpaRepository<FileShare, Long> {

    // Find all non-revoked shares for a recipient (for "Shared with Me" page)
    List<FileShare> findByRecipientAndRevokedFalse(User recipient);

    // Find a share by its secure token (for download link access)
    Optional<FileShare> findByShareToken(String token);

    // Find all shares created by owner for a specific file (for management UI)
    List<FileShare> findByFileAndOwner(FileMetadata file, User owner);

    // Find all shares owned by a user (for "My Shares" management page)
    List<FileShare> findByOwnerOrderByCreatedAtDesc(User owner);
}
