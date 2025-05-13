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
@Table(name = "patterns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatternEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @NotEmpty
    private String patternName;

    @Lob
    @NotNull
    @NotEmpty
    private String inputItems;  // "[{\"itemName\":\"Iron Ingot\",\"count\":3}, ...]"

    @NotNull
    @NotEmpty
    private String outputItem;

    @NotNull
    @Min(0)
    private Integer outputQuantity;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "interface_id")
    private MeInterfaceEntity meInterface;
}
