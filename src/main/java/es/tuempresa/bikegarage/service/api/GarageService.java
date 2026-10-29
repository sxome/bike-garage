package es.tuempresa.bikegarage.service.api;

import es.tuempresa.bikegarage.model.dto.HealthResponseDTO;

public interface GarageService {

    HealthResponseDTO getHealth();
}
