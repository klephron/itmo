package ru.itmo.zavar.devopslabs.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "storage_cells")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StorageCellEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Min(0)
    private Integer capacity;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "drive_id")
    private DriveEntity drive;
}
