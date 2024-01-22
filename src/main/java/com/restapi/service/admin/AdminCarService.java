package com.restapi.service.admin;

import com.restapi.dto.admin.AdminCarDto;
import com.restapi.model.AppUser;
import com.restapi.model.CarDetail;
import com.restapi.model.MaintenanceSchedule;
import com.restapi.model.Role;
import com.restapi.repository.CarDetailRepository;
import com.restapi.repository.UserRepository;
import com.restapi.request.admin.AdminCarFilterRequest;
import com.restapi.request.admin.AdminCarRequest;
import com.restapi.response.admin.AdminCarResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
public class AdminCarService {
    @Autowired
    private AdminCarDto adminCarDto;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CarDetailRepository carDetailRepository;

    @Transactional
    public List<AdminCarResponse> addCar(AdminCarRequest carRequest) {
        CarDetail carDetail = adminCarDto.mapToCarDetail(carRequest);
        carDetail.setPhoto(carRequest.getPhoto());
        Optional<AppUser> appUser = userRepository.findById(Integer.valueOf(carRequest.getMaintenance_staff_id()));
        carDetail.setMaintenanceStaff(appUser.get());
        List<CarDetail> carDetailList = new ArrayList<>();
        carDetailList.add(carDetailRepository.save(carDetail));
        return adminCarDto.mapToAdminCarResponse(carDetailList);
    }

    public List<AdminCarResponse> findAllCars() {
        List<CarDetail> carDetailsList = carDetailRepository.findAll();
        List<AdminCarResponse> adminCarResponse = adminCarDto.mapToAdminCarResponse(carDetailsList);
        return adminCarResponse;
    }

    public List<AdminCarResponse> deleteById(Integer id) {
        carDetailRepository.deleteById(id);
        return findAllCars();
    }

    @Transactional
    public CarDetail updateCar(AdminCarRequest adminCarRequest) {
        CarDetail carDetail = adminCarDto.mapToCarDetails(adminCarRequest);

        CarDetail existingCarDetail = carDetailRepository.findById(adminCarRequest.getId())
                .orElseThrow(() -> new EntityNotFoundException("CarDetail not found"));

        carDetail.setMaintenanceSchedules(new ArrayList<>()); // Clear existing schedules

        carDetail.getMaintenanceSchedules().addAll(existingCarDetail.getMaintenanceSchedules());
        AppUser maintenanceStaff = userRepository.findById(Integer.valueOf(adminCarRequest.getMaintenance_staff_id()))
                .orElseThrow(() -> new RuntimeException("Maintenance staff not found"));
        carDetail.setMaintenanceStaff(maintenanceStaff);
        if (adminCarRequest.getPhoto() == null) {
            CarDetail carDetailImage = carDetailRepository.findCarById(adminCarRequest.getId());
            carDetail.setPhoto(carDetailImage.getPhoto());
        } else {
            carDetail.setPhoto(adminCarRequest.getPhoto());
        }
        carDetail = carDetailRepository.save(carDetail);
        return carDetail;

    }

    @Transactional
    public List<AdminCarResponse> filterByManufacturerAndSeats(AdminCarFilterRequest adminCarFilterRequest) {
        List<CarDetail> carDetail;
        if (adminCarFilterRequest.getManufacturer() != null && !adminCarFilterRequest.getManufacturer().isEmpty()
                && adminCarFilterRequest.getSeat() != null && !adminCarFilterRequest.getSeat().isEmpty()) {
            // Both manufacturer and seats are provided
            carDetail = carDetailRepository.findByManufacturerInAndSeatsIn(
                    adminCarFilterRequest.getManufacturer(),
                    adminCarFilterRequest.getSeat()
            );
        } else if (adminCarFilterRequest.getManufacturer() != null && !adminCarFilterRequest.getManufacturer().isEmpty()) {
            // Only manufacturer is provided
            carDetail = carDetailRepository.findByManufacturerIn(adminCarFilterRequest.getManufacturer());
        } else if (adminCarFilterRequest.getSeat() != null && !adminCarFilterRequest.getSeat().isEmpty()) {
            // Only seats are provided
            carDetail = carDetailRepository.findBySeatsIn(adminCarFilterRequest.getSeat());
        } else {
            // No filters provided, return all cars
            carDetail = carDetailRepository.findAll();
        }

        return adminCarDto.mapToAdminCarResponse(carDetail);
    }
}


