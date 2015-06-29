package no.nb.microservices.geotag.service;

import no.nb.microservices.geotag.model.GeoPosition;
import no.nb.microservices.geotag.model.GeoQuery;
import no.nb.microservices.geotag.model.GeoTag;
import org.springframework.data.domain.Page;

/**
 * Created by andreasb on 25.06.15.
 */
public interface IGeoTagService {
    Page<GeoTag> query(GeoQuery geoQuery, int page, int size, String[] expand);

    GeoTag findOne(String id, String[] expand);

    GeoPosition findOnePosition(String id, String posId);

    void delete(String id);

    void deletePosition(String id, String positionId);

    GeoTag save(GeoTag geoTag);

    GeoPosition savePosition(String id, GeoPosition geoPosition);

    GeoTag update(String id, GeoTag geoTag);

    Page<GeoTag> nearby(GeoQuery geoQuery, int page, int size);

    Page<GeoTag> within(GeoQuery geoQuery, int page, int size);
}
