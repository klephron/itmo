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
    indexes = {@Index(columnList = "locationLatitude,locationLongitude,locationAltitude")})
public class UserEntity {
  private @Id @GeneratedValue(strategy = GenerationType.AUTO) @Column(name = "user_id") Long id;

  private @NotNull @NotEmpty String username;

  private @NotNull @NotEmpty String password;

  private @NotNull Double locationLatitude;

  private @NotNull Double locationLongitude;

  private @NotNull Double locationAltitude;
}
