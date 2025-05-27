package ru.itmopai.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import jakarta.validation.constraints.NotNull;
import ru.itmopai.backend.entity.NotificationEntity;

@Repository
public interface NotificationRepository extends CrudRepository<NotificationEntity, Long> {
  List<NotificationEntity> findAllByReadAtIsNotNullAndCreatedAtBetween(@NotNull LocalDateTime from,
      @NotNull LocalDateTime to);

  List<NotificationEntity> findAllByReadAtIsNullAndCreatedAtBetween(@NotNull LocalDateTime from,
      @NotNull LocalDateTime to);

  List<NotificationEntity> findAllByCreatedAtBetween(@NotNull LocalDateTime from,
      @NotNull LocalDateTime to);

  List<NotificationEntity> findAllByReadAtIsNotNullAndCreatedAtGreaterThanEquals(
      @NotNull LocalDateTime from);

  List<NotificationEntity> findAllByReadAtIsNotNullAndCreatedAtLessThanEquals(
      @NotNull LocalDateTime to);

  List<NotificationEntity> findAllByReadAtIsNullAndCreatedAtGreaterThanEquals(
      @NotNull LocalDateTime from);

  List<NotificationEntity> findAllByReadAtIsNullAndCreatedAtLessThanEquals(
      @NotNull LocalDateTime to);

  List<NotificationEntity> findAllByCreatedAtGreaterThanEquals(@NotNull LocalDateTime from);

  List<NotificationEntity> findAllByCreatedAtLessThanEquals(@NotNull LocalDateTime to);

}
