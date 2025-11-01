package com.dentallink.domain.search.service;

import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import com.dentallink.domain.hospital.repository.HospitalScheduleRepository;
import com.dentallink.domain.hospital.service.HospitalExternalService;
import com.dentallink.domain.search.dto.response.SearchHospitalResponse;
import com.dentallink.domain.user.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final HospitalExternalService hospitalExternalService;
    private final HospitalScheduleRepository hospitalScheduleRepository;

    @Override
    public PageResponse<SearchHospitalResponse> getSearchHospital(int page, int size, String keyword) {

        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size);
        Page<Hospital> hospitals = hospitalExternalService.getHospitalsByKeyword(pageable, keyword);

        Page<SearchHospitalResponse> response = hospitals.map(
                hospital -> SearchHospitalResponse.from(hospital, hospital.getHospitalSchedule())
        );

        return PageResponse.fromPage(response);
    }
}
