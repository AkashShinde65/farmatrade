package com.farmatrade.logistics.repository;

import com.farmatrade.logistics.entity.Truck;
import com.farmatrade.logistics.entity.TruckStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TruckRepository extends JpaRepository<Truck, Long> {

    List<Truck> findByStatus(TruckStatus status);

    List<Truck> findByStatusAndAvailableAgainAtBefore(TruckStatus status, LocalDateTime cutoff);

    /**
     * Atomic compare-and-swap booking: only succeeds (returns 1) if the truck was still
     * AVAILABLE at the moment MySQL executes this UPDATE. Returns 0 if another request booked
     * it first -- the caller falls through to the next-nearest truck when that happens.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Truck t SET t.status = com.farmatrade.logistics.entity.TruckStatus.BOOKED, "
            + "t.currentLogisticsRequestId = :requestId, t.availableAgainAt = :availableAgainAt "
            + "WHERE t.id = :truckId AND t.status = com.farmatrade.logistics.entity.TruckStatus.AVAILABLE")
    int tryBook(@Param("truckId") Long truckId, @Param("requestId") Long requestId,
                @Param("availableAgainAt") LocalDateTime availableAgainAt);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Truck t SET t.status = com.farmatrade.logistics.entity.TruckStatus.AVAILABLE, "
            + "t.currentLogisticsRequestId = NULL, t.availableAgainAt = NULL WHERE t.id = :truckId")
    void release(@Param("truckId") Long truckId);
}
