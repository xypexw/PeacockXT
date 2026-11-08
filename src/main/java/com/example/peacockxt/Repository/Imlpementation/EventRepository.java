package com.example.peacockxt.Repository.Imlpementation;

import com.example.peacockxt.Models.Event;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e WHERE e.team.teamId = :teamId AND e.eventId IN :eventIds ORDER BY e.eventId DESC")
    List<Event> fetchEventBatchByEventId(@Param("teamId") Long teamId,
                                         @Param("eventIds") List<Long> eventIds);

    @Query("SELECT e.eventId FROM Event e " +
            "WHERE e.team.teamId = :teamId AND e.eventId < :cursorId AND e.status != 'DELETED' " +
            " ORDER BY e.eventId DESC")
    List<Long> getIndexByTeamIdAndCursorId(@Param("teamId") Long teamId,
                                           @Param("cursorId") Long cursorId,
                                           Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Event e SET e.numberAttend = e.numberAttend + :number WHERE e.eventId = :eventId")
    void increasingAttended(@Param("eventId") Long eventId ,@Param("number") int number );
}