package com.diariodebordo.diariobordo.repository;

import com.diariodebordo.diariobordo.model.GithubCommit;
import com.diariodebordo.diariobordo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GithubCommitRepository extends JpaRepository<GithubCommit, Long> {

    List<GithubCommit> findByUserOrderByCommitDateDesc(User user);

    List<GithubCommit> findByUserAndCommitDateBetween(User user, LocalDate start, LocalDate end);

    boolean existsByUserAndSha(User user, String sha);

    Optional<GithubCommit> findFirstByUserOrderByFetchedAtDesc(User user);
}
