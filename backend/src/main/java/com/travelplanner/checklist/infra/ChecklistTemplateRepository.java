package com.travelplanner.checklist.infra;

import com.travelplanner.checklist.domain.ChecklistTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChecklistTemplateRepository extends JpaRepository<ChecklistTemplate, Long> {

  /** Do sistema primeiro, depois os do usuário, cada grupo na ordem de criação. */
  @EntityGraph(attributePaths = "items")
  @Query(
      "select distinct t from ChecklistTemplate t where t.ownerUserId is null or t.ownerUserId ="
          + " :userId order by case when t.ownerUserId is null then 0 else 1 end, t.id")
  List<ChecklistTemplate> findVisibleTo(Long userId);
}
