package ru.itmopai.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_cases",
    indexes = {
        // select count(*) from user_cases where user_id=1 and status <> "WORKING"
        @Index(columnList = "user_id,status")
    })
public class UserCaseEntity {
  private @EmbeddedId UserPermissionId id;

  private @NotNull @NotEmpty @Column(nullable = false) String position;

  private @NotNull @NotEmpty @Column(nullable = false) @Enumerated(EnumType.STRING) Status status;

  @EqualsAndHashCode
  @Embeddable
  @Getter
  public static class UserPermissionId {
    private @NotNull @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(
        name = "user_id") UserEntity user;

    private @NotNull @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(
        name = "case_id") CaseEntity relatedCase;
  }

  public static enum Status {
    WORKING, VACATION, TRANSFERED // переданы полномочия
  }
}
