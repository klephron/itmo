package ru.itmopai.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cases")
public class CaseEntity {
  private @Id @GeneratedValue(strategy = GenerationType.AUTO) @Column(name = "case_id") Long id;

  private @NotNull @NotEmpty String title;

  private String description;

  private @NotNull @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(
      name = "creator_user_id") UserEntity creator;

  private @NotNull LocalDateTime createdAt;

  private @NotNull @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(
      name = "updator_user_id") UserEntity updator;

  private @NotNull LocalDateTime updatedAt;

  private @NotNull @NotEmpty String status;

  private @NotNull @OneToOne(fetch = FetchType.LAZY) @JoinColumn(
      name = "incident_id") IncidentEntity incident;
}
