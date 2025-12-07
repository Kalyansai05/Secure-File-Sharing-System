package com.tsv.implementation.dao;

import com.tsv.implementation.model.FileMetadata;
import com.tsv.implementation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<FileMetadata, Long> {
    List<FileMetadata> findByOwnerOrderByUploadedAtDesc(User owner);
}
