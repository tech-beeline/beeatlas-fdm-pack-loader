/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmpackloader.annotation.ApiErrorCodes;
import ru.beeline.fdmpackloader.dto.PackageAndPackagePartDTO;
import ru.beeline.fdmpackloader.dto.PackageDTO;
import ru.beeline.fdmpackloader.dto.PackageRegistrationRequestDTO;
import ru.beeline.fdmpackloader.dto.PackageRegistrationResponseDTO;
import ru.beeline.fdmpackloader.service.PackageService;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Пакеты v1", description = "Регистрация и получение информации о пакетах (API v1)")
public class PackageController {

    @Autowired
    private PackageService packageService;

    @ApiErrorCodes({400, 404, 500})
    @PostMapping("/package")
    @Operation(summary = "Регистрация пакета",
            description = "Регистрирует новый пакет с указанием источника, статуса и идентификатора источника")
    public ResponseEntity<PackageRegistrationResponseDTO> registration(
            @RequestBody PackageRegistrationRequestDTO packageRegistrationRequestDTO,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "source_id", required = false) Integer sourceId) {
        return ResponseEntity.ok(packageService.registration(packageRegistrationRequestDTO, source, status, sourceId));
    }

    @ApiErrorCodes({400, 500})
    @GetMapping("/packages-list")
    @Operation(summary = "Получение списка пакетов",
            description = "Возвращает постраничный список пакетов с фильтрацией по статусу")
    public Page<PackageDTO> getKidsById(@RequestParam(value = "status", required = false, defaultValue = "none") String status,
                                        @RequestParam(value = "limit", required = false, defaultValue = "15") Integer limit,
                                        @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset) {
        return packageService.getPackageList(status, limit, offset);
    }

    @ApiErrorCodes({400, 500})
    @GetMapping("/package/{id}")
    @Operation(summary = "Информация о пакете",
            description = "Возвращает детальную информацию о пакете и его частях по идентификатору")
    public PackageAndPackagePartDTO getPackageParts(@PathVariable Integer id,
                                                    @RequestParam(value = "limit", required = false, defaultValue = "" + Integer.MAX_VALUE) Integer limit,
                                                    @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset) {
        return packageService.getPackageParts(id, limit, offset);
    }

}
