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
  private @Id @NotNull @GeneratedValue(strategy = GenerationType.AUTO) Long id;

  private @NotNull @OneToOne(fetch = FetchType.EAGER) UserEntity userId;

  private @NotNull @NotEmpty String privilege;
}
