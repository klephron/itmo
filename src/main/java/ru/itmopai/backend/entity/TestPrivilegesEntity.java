package ru.itmopai.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "test_privileges")
public class TestPrivilegesEntity {
  private @Id @NotNull @GeneratedValue(strategy = GenerationType.AUTO) Long id;

  private @NotNull @OneToOne(fetch = FetchType.EAGER) TestEntity testId;

  private @NotNull @NotEmpty String privilege;
}
