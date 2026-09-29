package com.urlshortener.repository;

import com.urlshortener.entity.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    interface DailyClicks {
        String getClickDay();
        Long getClicks();
    }

    @Query(value = "SELECT DATE_FORMAT(clicked_at, '%Y-%m-%d') AS clickDay, COUNT(*) AS clicks "
            + "FROM click_events WHERE short_code = :code AND clicked_at >= :since "
            + "GROUP BY DATE_FORMAT(clicked_at, '%Y-%m-%d') ORDER BY clickDay",
            nativeQuery = true)
    List<DailyClicks> countByDay(@Param("code") String code, @Param("since") LocalDateTime since);
}