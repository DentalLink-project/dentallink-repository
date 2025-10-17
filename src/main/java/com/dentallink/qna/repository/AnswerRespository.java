package com.dentallink.qna.repository;

import com.dentallink.qna.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRespository extends JpaRepository<Answer,Long> {
}
