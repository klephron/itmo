package ru.itmo.zavar.devopslabs.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cell_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CellItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @NotEmpty
    private String itemName;

    @NotNull
    @Min(0)
    private Integer quantity;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "storage_cell_id")
    private StorageCellEntity storageCell;
}
