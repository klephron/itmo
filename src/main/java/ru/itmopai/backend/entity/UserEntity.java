package ru.itmopai.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(name = "users",
    indexes = { @Index(columnList = "locationLatitude,locationLongitude,locationAltitude") })
public class UserEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "user_id")
  private Long id;

  @NotNull
  @NotEmpty
  @Column(nullable = false)
  private String username;

  @NotNull
  @NotEmpty
  @Column(nullable = false)
  private String name;

  @NotNull
  @NotEmpty
  @Column(nullable = false)
  private String surname;

  private String patronymic;

  @NotNull
  @NotEmpty
  @Column(nullable = false)
  private String password;

  @NotNull
  @Column(nullable = false)
  private Double locationLatitude;

  @NotNull
  @Column(nullable = false)
  private Double locationLongitude;

  @NotNull
  @Column(nullable = false)
  private Double locationAltitude;
}
