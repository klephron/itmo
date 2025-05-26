package ru.itmopai.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import ru.itmopai.backend.dto.NotificationDto;
import ru.itmopai.backend.entity.NotificationEntity;
import ru.itmopai.backend.entity.UserEntity;
import ru.itmopai.backend.mapper.NotificationMapper;
import ru.itmopai.backend.repository.NotificationRepository;
import ru.itmopai.backend.repository.UserRepository;

@Singleton
@AllArgsConstructor
public class NotificationService {
  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final NotificationMapper notificationMapper;

  public NotificationEntity addNotification(UserEntity userEntity, String title,
      String description) {
    NotificationEntity notificationEntity = new NotificationEntity();
    notificationEntity.setTitle(title);
    notificationEntity.setDescription(description);
    notificationEntity.setUser(userEntity);
    notificationEntity.setCreatedAt(LocalDateTime.now());
    return notificationRepository.save(notificationEntity);
  }

  public NotificationEntity addNotification(Long userId, String title, String description)
      throws NoSuchElementException {
    UserEntity userEntity =
        userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found"));
    return addNotification(userEntity, title, description);
  }

  @Transactional
  public NotificationEntity updateNotification(Long id, String title, String description,
      LocalDateTime readAt) throws NoSuchElementException {
    NotificationEntity notificationEntity =
        notificationRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Notification not found"));
    NotificationDto notificationDto =
        new NotificationDto(id, notificationEntity.getUser().getId(), title, description,
            notificationEntity.getCreatedAt(), readAt);
    NotificationEntity partialUpdate =
        notificationMapper.partialUpdate(notificationDto, notificationEntity);
    return notificationRepository.update(partialUpdate);
  }

  public void deleteNotification(Long notificationId) {
    notificationRepository.deleteById(notificationId);
  }

  public List<NotificationEntity> getReadNotifications(LocalDateTime from, LocalDateTime to) {
    return notificationRepository.findAllByReadAtIsNotNullAndCreatedAtBetween(from, to);
  }

  public List<NotificationEntity> getUnreadNotifications(LocalDateTime from, LocalDateTime to) {
    return notificationRepository.findAllByReadAtIsNullAndCreatedAtBetween(from, to);
  }

  public List<NotificationEntity> getAllNotifications(LocalDateTime from, LocalDateTime to) {
    return notificationRepository.findAllByCreatedAtBetween(from, to);
  }

  public NotificationEntity getNotification(Long notificationId) throws NoSuchElementException {
    return notificationRepository.findById(notificationId)
        .orElseThrow(() -> new NoSuchElementException("Notification not found"));
  }

}
