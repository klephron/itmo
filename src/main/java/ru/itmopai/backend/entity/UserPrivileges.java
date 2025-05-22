package ru.itmopai.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_privileges")
public class UserPrivileges {
  @Id
  @NotNull
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @NotNull
  @OneToOne(fetch = FetchType.EAGER)
  private UserEntity userId;

  @NotNull
  @NotEmpty
  private String privilege;
}
