package com.redbus.repository;

import com.redbus.entity.User;
import com.redbus.entity.UserDeviceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceSessionRepository extends JpaRepository<UserDeviceSession, Long> {
    List<UserDeviceSession> findByUserOrderByLastActiveAtDesc(User user);
    Optional<UserDeviceSession> findByUserAndIpAddressAndBrowserAndOs(User user, String ipAddress, String browser, String os);

    @Modifying
    @Query("DELETE FROM UserDeviceSession s WHERE s.user = :user AND s.id <> :currentSessionId")
    void deleteAllByUserExceptCurrent(@Param("user") User user, @Param("currentSessionId") Long currentSessionId);
}
