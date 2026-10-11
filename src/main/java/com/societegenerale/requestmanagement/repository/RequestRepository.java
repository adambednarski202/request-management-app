package com.societegenerale.requestmanagement.repository;

import com.societegenerale.requestmanagement.domain.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long>, JpaSpecificationExecutor<Request> {

    @Query("SELECT COALESCE(MAX(r.publicationId), 0) FROM Request r")
    Long findMaxPublicationId();
}
