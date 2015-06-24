package no.nb.microservices.geotag.rest.assembler;

import no.nb.microservices.geotag.model.GeoQuery;
import no.nb.microservices.geotag.model.GeoTag;
import org.springframework.data.domain.Page;

/**
 * Created by Andreas Bjørnådal (andreasb) on 27.08.14.
 */
public class GeoTagPage {

    private Page<GeoTag> page;
    private GeoQuery geoQuery;

    public GeoTagPage(Page<GeoTag> page, GeoQuery geoQuery) {
        this.page = page;
        this.geoQuery = geoQuery;
    }

    public Page<GeoTag> getPage() {
        return page;
    }

    public void setPage(Page<GeoTag> page) {
        this.page = page;
    }

    public GeoQuery getGeoQuery() {
        return geoQuery;
    }

    public void setGeoQuery(GeoQuery geoQuery) {
        this.geoQuery = geoQuery;
    }
}
