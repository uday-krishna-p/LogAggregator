package com.projects.logaggregator.repository;

import com.projects.logaggregator.model.LogStorageEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogStorageEntryRepository extends JpaRepository<LogStorageEntryEntity, Long> {

}
