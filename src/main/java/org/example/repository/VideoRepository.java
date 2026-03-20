package org.example.repository;

import org.example.model.ES.VideoES;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface VideoRepository extends ElasticsearchRepository<VideoES, Long> {
}
