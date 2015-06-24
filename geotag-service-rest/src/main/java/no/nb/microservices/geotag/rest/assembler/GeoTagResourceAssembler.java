package no.nb.microservices.geotag.rest.assembler;

import no.nb.microservices.geotag.config.ApplicationSettings;
import no.nb.microservices.geotag.model.GeoTag;
import no.nb.microservices.geotag.rest.controller.GeoTagController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * Created by Andreas Bjørnådal (andreasb) on 26.08.14.
 */
@Component
public class GeoTagResourceAssembler implements ResourceAssembler<GeoTagPage, PagedResources<GeoTag>> {

    private final ApplicationSettings applicationSettings;

    @Autowired
    public GeoTagResourceAssembler(ApplicationSettings applicationSettings) {
        this.applicationSettings = applicationSettings;
    }

    @Override
    public PagedResources<GeoTag> toResource(GeoTagPage geoTagPage) {
        Page page = geoTagPage.getPage();
        Collection<GeoTag> resources = new ArrayList<GeoTag>();
        List<Link> links = new ArrayList<Link>();

        if (page == null) {
            PagedResources<GeoTag> pagedResources = new PagedResources<GeoTag>(resources, new PagedResources.PageMetadata(0, 0, 0, 0), links);
            return pagedResources;
        }

        for(Object o : page.getContent()) {
            GeoTag geoTag = (GeoTag)o;
            if (!geoTagPage.getGeoQuery().isMinify()) {
                geoTag.add(linkTo(GeoTagController.class).slash(geoTag.getGeoId()).withSelfRel());
            }
            
            resources.add(geoTag);
        }	

        if (geoTagPage.getGeoQuery().getSecondLat() != null && geoTagPage.getGeoQuery().getSecondLon() != null) {
            links.add(linkTo(methodOn(GeoTagController.class).getTagsWithin(geoTagPage.getGeoQuery(), page.getNumber(), page.getSize())).withSelfRel());
            if (page.hasPrevious()) {
                links.add(linkTo(methodOn(GeoTagController.class).getTagsWithin(geoTagPage.getGeoQuery(), page.previousPageable().getPageNumber(), page.getSize())).withRel("prev"));
            }
            if (page.hasNext()) {
                links.add(linkTo(methodOn(GeoTagController.class).getTagsWithin(geoTagPage.getGeoQuery(), page.nextPageable().getPageNumber(), page.getSize())).withRel("next"));
            }
            if (!page.isFirst()) {
                links.add(linkTo(methodOn(GeoTagController.class).getTagsWithin(geoTagPage.getGeoQuery(), 0, page.getSize())).withRel("first"));
            }
            if (!page.isLast()) {
                links.add(linkTo(methodOn(GeoTagController.class).getTagsWithin(geoTagPage.getGeoQuery(), page.getTotalPages() - 1, page.getNumberOfElements())).withRel("last"));
            }
        }
        else if (geoTagPage.getGeoQuery().getLat() != null && geoTagPage.getGeoQuery().getLon() != null && geoTagPage.getGeoQuery().getMaxDistance() != null) {
            links.add(linkTo(methodOn(GeoTagController.class).getNearbyTags(geoTagPage.getGeoQuery(), page.getNumber(), page.getSize())).withSelfRel());
            if (page.hasPrevious()) {
                links.add(linkTo(methodOn(GeoTagController.class).getNearbyTags(geoTagPage.getGeoQuery(), page.previousPageable().getPageNumber(), page.getSize())).withRel("prev"));
            }
            if (page.hasNext()) {
                links.add(linkTo(methodOn(GeoTagController.class).getNearbyTags(geoTagPage.getGeoQuery(), page.nextPageable().getPageNumber(), page.getSize())).withRel("next"));
            }
            if (!page.isFirst()) {
                links.add(linkTo(methodOn(GeoTagController.class).getNearbyTags(geoTagPage.getGeoQuery(), 0, page.getSize())).withRel("first"));
            }
            if (!page.isLast()) {
                links.add(linkTo(methodOn(GeoTagController.class).getNearbyTags(geoTagPage.getGeoQuery(), page.getTotalPages()-1, page.getNumberOfElements())).withRel("last"));
            }
        }
        else {
            links.add(linkTo(methodOn(GeoTagController.class).getTags(geoTagPage.getGeoQuery(), page.getNumber(), page.getSize())).withSelfRel());

            if (page.hasPrevious()) {
                links.add(linkTo(methodOn(GeoTagController.class).getTags(geoTagPage.getGeoQuery(), page.previousPageable().getPageNumber(), page.getSize())).withRel("prev"));
            }
            if (page.hasNext()) {
                links.add(linkTo(methodOn(GeoTagController.class).getTags(geoTagPage.getGeoQuery(), page.nextPageable().getPageNumber(), page.getSize())).withRel("next"));
            }
            if (!page.isFirst()) {
                links.add(linkTo(methodOn(GeoTagController.class).getTags(geoTagPage.getGeoQuery(), 0, page.getSize())).withRel("first"));
            }
            if (!page.isLast()) {
                links.add(linkTo(methodOn(GeoTagController.class).getTags(geoTagPage.getGeoQuery(), page.getTotalPages()-1, page.getNumberOfElements())).withRel("last"));
            }
        }

        PagedResources.PageMetadata metadata = new PagedResources.PageMetadata(page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages());
        PagedResources<GeoTag> pagedResources = new PagedResources<GeoTag>(resources, metadata, links);

        return pagedResources;
    }
}