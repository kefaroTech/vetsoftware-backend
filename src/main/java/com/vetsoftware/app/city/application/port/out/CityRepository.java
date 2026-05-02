package com.vetsoftware.app.city.application.port.out;

import com.vetsoftware.app.city.domain.City;
import java.util.List;
import java.util.Optional;

public interface CityRepository {
    City save(City city);
    Optional<City> findById(Long id);
    List<City> findAll();
    void delete(Long id);
}
