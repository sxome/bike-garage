package es.tuempresa.bikegarage.controller;

import es.tuempresa.bikegarage.model.dto.HealthResponseDTO;
import es.tuempresa.bikegarage.service.api.GarageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static es.tuempresa.bikegarage.util.ApiConstantes.HEALTH;

@RestController
@RequiredArgsConstructor
public class GarageController {

    private final GarageService garageService;

    @GetMapping(HEALTH)
    public ResponseEntity<HealthResponseDTO> health() {
        return ResponseEntity.ok(garageService.getHealth());
    }
}
