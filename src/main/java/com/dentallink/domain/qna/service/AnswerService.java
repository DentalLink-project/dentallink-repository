package com.dentallink.domain.qna.service;

import com.dentallink.domain.qna.repository.AnswerRepository;
import com.dentallink.domain.qna.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    // 비즈니스 로직 작성 create


    // 비즈니스 로직 작성 read


    // 비즈니스 로직 작성 update


    // 비즈니스 로직 작성 delete

}
