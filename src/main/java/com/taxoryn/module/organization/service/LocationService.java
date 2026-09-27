package com.taxoryn.module.organization.service;

import com.taxoryn.module.organization.dto.CreateLocationRequest;
import com.taxoryn.module.organization.dto.LocationDto;
import com.taxoryn.module.organization.dto.UpdateLocationRequest;

import java.util.List;
import java.util.UUID;

public interface LocationService {

    LocationDto createLocation(UUID organizationId, CreateLocationRequest request);

    LocationDto initializeDefaultHeadOffice(UUID organizationId, String organizationName, String addressLine1, String city, String state, String pincode);

    List<LocationDto> getLocations(UUID organizationId);

    LocationDto getLocationById(UUID organizationId, UUID locationId);

    LocationDto updateLocation(UUID organizationId, UUID locationId, UpdateLocationRequest request);

    void deleteLocation(UUID organizationId, UUID locationId);

    LocationDto assignEmployees(UUID organizationId, UUID locationId, List<UUID> employeeIds);

    List<UUID> getLocationsForEmployee(UUID organizationId, UUID employeeId);
}
