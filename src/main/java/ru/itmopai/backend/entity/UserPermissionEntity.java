package ru.itmopai.backend.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
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
@Table(name = "user_permissions",
    indexes = {@Index(columnList = "user_id")})
public class UserPermissionEntity {

  private @EmbeddedId UserPermissionId id;

  @Embeddable
  @EqualsAndHashCode
  public static class UserPermissionId {
    // TODO: do we actually need eager?
    private @NotNull @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(
        name = "user_id") UserEntity user;

    private @NotNull @NotEmpty String privilege;
  }
}
