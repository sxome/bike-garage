package es.tuempresa.bikegarage.service.impl;

import es.tuempresa.bikegarage.model.dto.HealthResponseDTO;
import es.tuempresa.bikegarage.service.api.GarageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GarageServiceImpl implements GarageService {

    private static final Logger log = LoggerFactory.getLogger(GarageServiceImpl.class);

    @Override
    public HealthResponseDTO getHealth() {
        log.debug("Consultando estado de la aplicación");
        return new HealthResponseDTO("UP");
    }
}
