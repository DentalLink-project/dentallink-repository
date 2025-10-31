package com.dentallink.domain.search.service;

import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.search.dto.response.SearchHospitalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {



    @Override
    public PageResponse<SearchHospitalResponse> getSearchHospital(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size);


        return null;
    }
}
