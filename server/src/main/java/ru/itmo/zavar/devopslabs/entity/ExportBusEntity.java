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
@Table(name = "export_buses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExportBusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @NotEmpty
    private String busName;

    @NotNull
    @Min(0)
    private int channelsUsed;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "network_id")
    private NetworkEntity network;
}
