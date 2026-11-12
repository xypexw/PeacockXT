package com.github.peacockxt.repository.implementation;

import com.github.peacockxt.models.PrivateSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface PrivateSessionRepository extends JpaRepository<PrivateSession,Long> {

    @Query("select ps.id from PrivateSession ps where ps.connectorId = :userId OR ps.receiverId = :userId ")
    List<Long> getPrivateSessionIdsByUserId(@Param("userId") Long userId );

    @Query("select ps from PrivateSession ps where ps.id IN :ids ")
    List<PrivateSession> getPrivateSessionListByIds(@Param("ids")  List<Long> ids);
    PrivateSession getPrivateSessionById(Long id);
}
