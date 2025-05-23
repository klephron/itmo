package ru.itmopai.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_permissions")
public class UserPermissionEntity {
  private @EmbeddedId UserPermissionId id;

  @Embeddable
  @EqualsAndHashCode
  @Getter
  public static class UserPermissionId {
    private @NotNull @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(
        name = "user_id", nullable = false) UserEntity user;

    private @NotNull @NotEmpty @Column(nullable = false) String privilege;
  }
}
