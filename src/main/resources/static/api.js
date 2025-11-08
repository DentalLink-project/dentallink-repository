// API Configuration
const API_BASE_URL = 'http://43.203.215.197:8080/api';

// Storage for token
let authToken = localStorage.getItem('authToken');

// API Error Types and Helper
class APIError extends Error {
    constructor(message, status = null, details = null) {
        super(message);
        this.name = 'APIError';
        this.status = status;
        this.details = details;
    }
}

// API Helper Functions with Enhanced Error Handling
const apiCall = async (method, endpoint, data = null, options = {}) => {
    const fetchOptions = {
        method,
        headers: {
            'Content-Type': 'application/json',
        }
    };

    if (authToken) {
        fetchOptions.headers['Authorization'] = `Bearer ${authToken}`;
    }

    if (data) {
        fetchOptions.body = JSON.stringify(data);
    }

    // Default timeout: 30 seconds
    const timeout = options.timeout || 30000;
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeout);

    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            ...fetchOptions,
            signal: controller.signal
        });

        clearTimeout(timeoutId);

        // Handle 401 Unauthorized - Token expired
        if (response.status === 401) {
            localStorage.removeItem('authToken');
            authToken = null;

            // Only navigate if not already on login page
            const currentPage = document.querySelector('.page.active');
            if (currentPage && currentPage.id !== 'login') {
                // Small delay to allow current request to complete before navigation
                setTimeout(() => {
                    navigateTo('login');
                }, 100);
            }

            throw new APIError('세션이 만료되었습니다. 다시 로그인해주세요.', 401);
        }

        // Handle 403 Forbidden - Permission denied
        if (response.status === 403) {
            throw new APIError('접근 권한이 없습니다.', 403);
        }

        // Parse response
        let result;
        try {
            result = await response.json();
        } catch (parseError) {
            // Handle empty response or invalid JSON
            if (response.ok) {
                return null; // Success with no data
            }
            throw new APIError('응답 데이터 형식이 올바르지 않습니다.', response.status);
        }

        // Check if response is successful
        if (!response.ok) {
            const errorMessage = result?.message || result?.error || `API Error (${response.status})`;
            throw new APIError(errorMessage, response.status, result);
        }

        return result.data !== undefined ? result.data : result;
    } catch (error) {
        clearTimeout(timeoutId);

        // Handle network errors and timeouts
        if (error.name === 'AbortError') {
            console.error('API Timeout:', endpoint);
            throw new APIError('요청 시간이 초과되었습니다. 네트워크 연결을 확인해주세요.', null);
        }

        if (error instanceof TypeError && error.message.includes('Failed to fetch')) {
            console.error('Network Error:', endpoint, error);
            throw new APIError('네트워크 연결 오류가 발생했습니다. 인터넷 연결을 확인해주세요.', null);
        }

        // If it's already an APIError, re-throw it
        if (error instanceof APIError) {
            throw error;
        }

        // Fallback for unexpected errors
        console.error('API Error:', endpoint, error);
        throw new APIError(error.message || 'API 요청 중 오류가 발생했습니다.', null, error);
    }
};

// Auth API
const authAPI = {
    login: async (email, password) => {
        try {
            const response = await fetch(`${API_BASE_URL}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });

            // 백엔드는 토큰을 헤더로 반환함
            const authHeader = response.headers.get('Authorization');
            const refreshHeader = response.headers.get('Refresh-Token');

            const result = await response.json().catch(() => ({ message: 'No body' }));

            if (!response.ok) {
                throw new Error(result?.message || 'Login failed');
            }

            if (!authHeader) {
                throw new Error('Missing Authorization header');
            }

            // 'Bearer ' 접두사 제거 후 저장
            const token = authHeader.replace(/^Bearer\s+/i, '');
            authToken = token;
            localStorage.setItem('authToken', token);
            if (refreshHeader) {
                localStorage.setItem('refreshToken', refreshHeader);
            }
            return { token };
        } catch (error) {
            console.error('Login error:', error);
            throw error;
        }
    },

    signup: async (email, username, password, userRole) => {
        try {
            const response = await fetch(`${API_BASE_URL}/users/signup`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, username, password, userRole })
            });

            const result = await response.json();

            if (!response.ok) {
                throw new Error(result.message || 'Signup failed');
            }

            return result.data;
        } catch (error) {
            console.error('Signup error:', error);
            throw error;
        }
    },

    logout: async () => {
        try {
            if (authToken) {
                await apiCall('POST', '/auth/logout');
            }
        } catch (error) {
            console.error('Logout error:', error);
        } finally {
            localStorage.removeItem('authToken');
            authToken = null;
        }
    },

    getProfile: async () => {
        return await apiCall('GET', '/users/me');
    }
};

// Hospitals API
const hospitalsAPI = {
    getAll: async (page = 0, size = 10) => {
        // 백엔드는 page=1부터 받음
        const apiPage = (page ?? 0) + 1;
        return await apiCall('GET', `/hospitals?page=${apiPage}&size=${size}`);
    },

    getById: async (id) => {
        return await apiCall('GET', `/hospitals/${id}`);
    },

    search: async (query, page = 0, size = 10) => {
        const apiPage = (page ?? 0) + 1;
        return await apiCall('GET', `/search?keyword=${encodeURIComponent(query)}&page=${apiPage}&size=${size}`);
    },

    create: async (hospitalData) => {
        return await apiCall('POST', '/hospitals', hospitalData);
    },

    update: async (id, hospitalData) => {
        return await apiCall('PATCH', `/hospitals/${id}`, hospitalData);
    },

    delete: async (id) => {
        return await apiCall('DELETE', `/hospitals/${id}`);
    },

    getSchedule: async (id) => {
        return await apiCall('GET', `/hospitals/${id}/schedule`);
    },

    createSchedule: async (id, scheduleData) => {
        return await apiCall('POST', `/hospitals/${id}/schedule`, scheduleData);
    },

    getAvailableSlots: async (hospitalId, date) => {
        return await apiCall('GET', `/reservations/available-slots?hospitalId=${hospitalId}&date=${date}`);
    }
};

// Reservations API
const reservationsAPI = {
    create: async (reservationData) => {
        return await apiCall('POST', '/reservations', reservationData);
    },

    getById: async (id) => {
        return await apiCall('GET', `/reservations/${id}`);
    },

    getMyReservations: async (page = 0, size = 10) => {
        return await apiCall('GET', `/reservations/my?page=${page}&size=${size}`);
    },

    getHospitalReservations: async (hospitalId, page = 0, size = 10) => {
        return await apiCall('GET', `/reservations?hospitalId=${hospitalId}&page=${page}&size=${size}`);
    },

    updateStatus: async (id, status) => {
        return await apiCall('PATCH', `/reservations/${id}/status`, { status });
    },

    cancel: async (id) => {
        return await apiCall('DELETE', `/reservations/${id}`);
    },

    /**
     * 병원의 특정 날짜에 예약 가능한 시간 조회
     * @param {number} hospitalId - 병원 ID
     * @param {string} date - 조회 날짜 (YYYY-MM-DD 형식)
     * @returns {Array} 예약 가능한 시간대 배열
     */
    getAvailableSlots: async (hospitalId, date) => {
        return await apiCall('GET', `/reservations/available-slots?hospitalId=${hospitalId}&date=${encodeURIComponent(date)}`);
    }
};

// Reviews API
const reviewsAPI = {
    create: async (hospitalId, reviewData) => {
        return await apiCall('POST', `/hospitals/${hospitalId}/reviews`, reviewData);
    },

    getByHospital: async (hospitalId, page = 0, size = 10) => {
        return await apiCall('GET', `/hospitals/${hospitalId}/reviews?page=${page}&size=${size}`);
    },

    getById: async (id) => {
        return await apiCall('GET', `/reviews/${id}`);
    },

    update: async (id, reviewData) => {
        return await apiCall('PATCH', `/reviews/${id}`, reviewData);
    },

    delete: async (id) => {
        return await apiCall('DELETE', `/reviews/${id}`);
    },

    updateStatus: async (id, status) => {
        return await apiCall('PATCH', `/reviews/${id}/status`, { status });
    }
};

// Points API
const pointsAPI = {
    getBalance: async () => {
        return await apiCall('GET', '/points/account');
    },

    getTransactionHistory: async (page = 0, size = 10) => {
        return await apiCall('GET', `/point-log/me?page=${page}&size=${size}`);
    },

    deposit: async (accountId, amount) => {
        return await apiCall('POST', '/points/deposit', { accountId, amount });
    },

    withdraw: async (amount) => {
        return await apiCall('POST', '/points/withdraw', { amount });
    },

    // spend/refund는 별도 결제/예약 흐름에서 처리. 프론트에서는 사용하지 않음
};

// Favorites API
const favoritesAPI = {
    getAll: async () => {
        return await apiCall('GET', '/favorites');
    },

    add: async (hospitalId) => {
        return await apiCall('POST', '/favorites', { hospitalId });
    },

    remove: async (hospitalId) => {
        return await apiCall('DELETE', `/favorites/${hospitalId}`);
    }
};

// Q&A API
const qnaAPI = {
    createQuestion: async (hospitalId, questionData) => {
        return await apiCall('POST', `/hospitals/${hospitalId}/questions`, questionData);
    },

    getHospitalQuestions: async (hospitalId, page = 0, size = 10) => {
        return await apiCall('GET', `/hospitals/${hospitalId}/questions?page=${page}&size=${size}`);
    },

    updateQuestion: async (id, questionData) => {
        return await apiCall('PATCH', `/questions/${id}`, questionData);
    },

    deleteQuestion: async (id) => {
        return await apiCall('DELETE', `/questions/${id}`);
    },

    answerQuestion: async (id, answerData) => {
        return await apiCall('POST', `/questions/${id}/answers`, answerData);
    },

    updateAnswer: async (id, answerData) => {
        return await apiCall('PATCH', `/answers/${id}`, answerData);
    },

    deleteAnswer: async (id) => {
        return await apiCall('DELETE', `/answers/${id}`);
    }
};

console.log('API module loaded');
