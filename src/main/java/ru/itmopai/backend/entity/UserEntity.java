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
  private @Id @GeneratedValue(strategy = GenerationType.AUTO) @Column(name = "user_id") Long id;

  private @NotNull @NotEmpty @Column(nullable = false) String username;

  private @NotNull @NotEmpty @Column(nullable = false) String name;
  private @NotNull @NotEmpty @Column(nullable = false) String surname;
  private String patronymic;

  private @NotNull @NotEmpty @Column(nullable = false) String password;

  private @NotNull @Column(nullable = false) Double locationLatitude;

  private @NotNull @Column(nullable = false) Double locationLongitude;

  private @NotNull @Column(nullable = false) Double locationAltitude;
}
