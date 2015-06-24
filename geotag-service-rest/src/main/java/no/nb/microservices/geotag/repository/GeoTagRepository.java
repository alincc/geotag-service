package no.nb.microservices.geotag.repository;

import no.nb.microservices.geotag.model.GeoTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

import java.util.Date;

/**
 * Created by Andreas Bjørnådal (andreasb) on 26.08.14.
 */
public interface GeoTagRepository extends MongoRepository<GeoTag, String>, QueryDslPredicateExecutor<GeoTag> {
    Page<GeoTag> findByUrn(String urn, Pageable pageable);
    Page<GeoTag> findByCurrentPositionUserId(String userId, Pageable pageable);
    Page<GeoTag> findByCurrentPositionPositionNear(Point location, Distance distance, Pageable pageable);
    Page<GeoTag> findByCurrentPositionPositionWithin(Box box, Pageable pageable);
    Page<GeoTag> findByCurrentPositionNotNull(Pageable pageable);
    
    Page<GeoTag> findByCurrentPositionDateGreaterThan(Date date, Pageable pageable);
    
    @Query(value = "{'currentPosition.date': {'$gt': ?0}}", fields = "{'sesamId' : 1, 'urn' : 1, 'title' : 1, 'currentPosition.position' : 1}")
    Page<GeoTag> findByCurrentPositionDateGreaterThanMinified(Date date, Pageable pageable);
}
