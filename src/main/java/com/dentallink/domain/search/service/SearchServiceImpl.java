package com.dentallink.domain.search.service;

import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.service.HospitalExternalService;
import com.dentallink.domain.search.dto.response.SearchHospitalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final HospitalExternalService hospitalExternalService;

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
