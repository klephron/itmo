package ru.itmo.zavar.account.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.account.dto.ApartmentDto;
import ru.itmo.zavar.account.dto.RadiatorToApartmentDto;
import ru.itmo.zavar.account.exception.ObjectNotFoundException;
import ru.itmo.zavar.account.security.CustomUserDetails;
import ru.itmo.zavar.account.service.ApartmentService;

import java.util.List;

@RestController
@RequestMapping("/apartments")
@Tag(name = "Apartments", description = "Управление квартирами и датчиками")
public class ApartmentController {

    private final ApartmentService apartmentService;

    public ApartmentController(ApartmentService apartmentService) {
        this.apartmentService = apartmentService;
    }


    @Operation(summary = "Создать квартиру", description = "Создаёт новую квартиру для указанного пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Квартира успешно создана", content = @Content(schema = @Schema(implementation = ApartmentDto.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка создания", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.account.security.Roles).TSJ.toString())")
    @PostMapping
    public ResponseEntity<?> createApartment(
            @Parameter(description = "ID пользователя") @RequestParam String userId,
            @Parameter(description = "Название квартиры") @RequestParam String name) {
        try {
            ApartmentDto apartment = this.apartmentService.createNewApartment(userId, name);
            return ResponseEntity.status(HttpStatus.CREATED).body(apartment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Получить квартиру по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Квартира найдена", content = @Content(schema = @Schema(implementation = ApartmentDto.class))),
            @ApiResponse(responseCode = "400", description = "Квартира не найдена", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.account.security.Roles).TSJ.toString())")
    @GetMapping("/{id}")
    public ResponseEntity<?> getApartmentById(
            @Parameter(description = "ID квартиры") @PathVariable String id) {
        try {
            ApartmentDto apartment = this.apartmentService.getApartmentById(id);
            return ResponseEntity.ok(apartment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Получить все квартиры")
    @ApiResponse(responseCode = "200", description = "Список квартир получен", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApartmentDto.class))))
    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.account.security.Roles).TSJ.toString())")
    @GetMapping
    public ResponseEntity<List<ApartmentDto>> getAllApartments() {
        return ResponseEntity.ok(this.apartmentService.getAllApartments());
    }

    @Operation(summary = "Получить квартиры по ID пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Квартиры найдены", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApartmentDto.class)))),
            @ApiResponse(responseCode = "400", description = "Ошибка получения", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.account.security.Roles).TSJ.toString())")
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<?> getApartmentByUserId(
            @Parameter(description = "ID пользователя") @PathVariable String userId) {
        try {
            List<ApartmentDto> apartmentsByUserId = this.apartmentService.getApartmentsByUserId(userId);
            return ResponseEntity.ok(apartmentsByUserId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Получить мои квартиры", description = "Получить список квартир, привязанных к текущему пользователю")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Квартиры найдены", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApartmentDto.class)))),
            @ApiResponse(responseCode = "400", description = "Ошибка получения", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/my")
    public ResponseEntity<?> getMyApartments(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails user) {
        try {
            List<ApartmentDto> apartmentsByUserId = this.apartmentService.getApartmentsByUserId(user.getId());
            return ResponseEntity.ok(apartmentsByUserId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Удалить квартиру по ID")
    @ApiResponse(responseCode = "204", description = "Квартира удалена")
    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.account.security.Roles).TSJ.toString())")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApartment(
            @Parameter(description = "ID квартиры") @PathVariable String id) {
        this.apartmentService.deleteApartmentById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Привязать радиатор к квартире")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Радиатор успешно привязан", content = @Content(schema = @Schema(implementation = RadiatorToApartmentDto.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка привязки", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.account.security.Roles).TSJ.toString())")
    @PostMapping("/{apartmentId}/radiators/{deviceId}")
    public ResponseEntity<?> attachRadiatorToApartment(
            @Parameter(description = "ID квартиры") @PathVariable String apartmentId,
            @Parameter(description = "ID радиатора") @PathVariable Long deviceId) {
        try {
            RadiatorToApartmentDto dto = this.apartmentService.attachRadiatorToApartment(apartmentId, deviceId);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Получить все радиаторы, привязанные к квартире")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список радиаторов получен", content = @Content(array = @ArraySchema(schema = @Schema(implementation = RadiatorToApartmentDto.class)))),
            @ApiResponse(responseCode = "400", description = "Квартира не найдена", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.account.security.Roles).TSJ.toString())")
    @GetMapping("/{apartmentId}/radiators")
    public ResponseEntity<?> listAllRadiators(
            @Parameter(description = "ID квартиры") @PathVariable String apartmentId) {
        try {
            List<RadiatorToApartmentDto> list = this.apartmentService.listAllRadiators(apartmentId);
            return ResponseEntity.ok(list);
        } catch (ObjectNotFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
