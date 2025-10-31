package com.dentallink.domain.search.service;

import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.search.dto.response.SearchHospitalResponse;

public interface SearchService {
    PageResponse<SearchHospitalResponse> getSearchHospital(int page, int size, String keyword);
}
