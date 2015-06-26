package no.nb.microservices.geotag.service;

import com.mysema.query.types.expr.BooleanExpression;
import no.nb.microservices.geotag.config.Constants;
import no.nb.microservices.geotag.model.GeoPosition;
import no.nb.microservices.geotag.model.GeoQuery;
import no.nb.microservices.geotag.model.GeoTag;
import no.nb.microservices.geotag.model.QGeoTag;
import no.nb.microservices.geotag.repository.GeoTagRepository;
import no.nb.nbsecurity.NBUserDetails;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Created by andreasb on 25.06.15.
 */
@Service
public class GeotagService implements IGeotagService {

    private static final Logger LOG = LoggerFactory.getLogger(GeotagService.class);

    private final GeoTagRepository geoTagRepository;
    private final NBUserService nbUserService;

    @Autowired
    public GeotagService(GeoTagRepository geoTagRepository, NBUserService nbUserService) {
        this.geoTagRepository = geoTagRepository;
        this.nbUserService = nbUserService;
    }

    @Override
    public Page<GeoTag> query(GeoQuery query, int page, int size, String[] expand) {
        //Expand
        boolean removeHistory = true;
        if (expand != null) {
            for (String item : expand) {
                //Expand games
                if (item.equals("positionHistory")) {
                    removeHistory = false;
                }
            }
        }

        QGeoTag t = QGeoTag.geoTag;
        BooleanExpression expression = t.urn.isNotNull();

        if (StringUtils.isNotBlank(query.getUrn())) {
            expression = expression.and(t.urn.eq(query.getUrn()));
        }
        if (StringUtils.isNotBlank(query.getEmail())) {
            expression = expression.and(t.currentPosition.userEmail.eq(query.getEmail())).or(t.positionHistory.any().userEmail.eq(query.getEmail()));
        }
        if (StringUtils.isNotBlank(query.getUser())) {
            expression = expression.and(t.currentPosition.userId.eq(query.getUser()));
        }
        if (query.getUpdatedSince() != null) {
            expression = expression.and(t.currentPosition.date.after(query.getUpdatedSince()));
        }
        if (query.getDirty() != null) {
            expression = expression.and(t.dirty.eq(query.getDirty()));
        }
        if (query.getSticky() != null) {
            expression = expression.and(t.sticky.eq(query.getSticky()));
        }

        PageRequest pageRequest = new PageRequest(page, size, new Sort(Sort.Direction.DESC, "date"));
        Page<GeoTag> pages = geoTagRepository.findAll(expression, pageRequest);

        // If not admin then remove some fields.
        boolean maskGeotag = (pages != null && (nbUserService.getNBUser() == null || !nbUserService.getNBUser().getAuthorities().contains(new SimpleGrantedAuthority(Constants.ADMIN_ROLE))));
        for (GeoTag geoTag : pages.getContent()) {
            if (removeHistory) {
                geoTag.setPositionHistory(null);
            }
            if (maskGeotag) {
                geoTag.mask();
            }
        }

        return pages;
    }

    @Override
    public GeoTag findOne(String id, String[] expand) {
        //Expand
        boolean removeHistory = true;
        if (expand != null) {
            for (String item : expand) {
                //Expand games
                if (item.equals("positionHistory")) {
                    removeHistory = false;
                }
            }
        }

        GeoTag geoTag = geoTagRepository.findOne(id);

        if (geoTag == null) {
            throw new NoSuchElementException("Geotag not found");
        }

        // If not admin then remove some fields
        if (nbUserService.getNBUser() == null || !nbUserService.getNBUser().getAuthorities().contains(new SimpleGrantedAuthority(Constants.ADMIN_ROLE))) {
            geoTag.mask();
        }
        if (removeHistory) {
            geoTag.setPositionHistory(null);
        }

        return geoTag;
    }

    @Override
    public void delete(String id, String positionId) {
        GeoTag geoTag = geoTagRepository.findOne(id);

        if (geoTag == null) {
            throw new NoSuchElementException("Geotag not found");
        }

        for (Iterator<GeoPosition> iterator = geoTag.getPositionHistory().iterator(); iterator.hasNext(); ) {
            GeoPosition geoPosition = iterator.next();
            if (geoPosition.getPosId() != null && geoPosition.getPosId().equals(positionId)) {
                iterator.remove();
            }
        }

        if (geoTag.getCurrentPosition().getPosId().equals(positionId) && geoTag.getPositionHistory().size() - 1 >= 0) {
            geoTag.setCurrentPosition(geoTag.getPositionHistory().get(geoTag.getPositionHistory().size() - 1));
            geoTag.getPositionHistory().remove(geoTag.getPositionHistory().size() - 1);
        }

        geoTagRepository.save(geoTag);
    }

    @Override
    public GeoTag save(GeoTag geoTag) {
        Page<GeoTag> geoTags = geoTagRepository.findByUrn(geoTag.getUrn(), new PageRequest(0, 1));
        GeoTag oldGeoTag = (geoTags.hasContent()) ? geoTags.getContent().get(0) : null;

        NBUserDetails user = nbUserService.getNBUser();
        String currentUser = user.getUserId().toString();
        geoTag.getCurrentPosition().setPosId(UUID.randomUUID().toString());
        geoTag.getCurrentPosition().setUserId(currentUser);
        geoTag.getCurrentPosition().setUserDisplayName(user.getDisplayName());
        geoTag.getCurrentPosition().setUserEmail(user.getEmail());
        geoTag.getCurrentPosition().setDate(Calendar.getInstance().getTime());

        if (oldGeoTag != null) {
            if (oldGeoTag.isSticky() != null && oldGeoTag.isSticky()) {
                throw new AccessDeniedException("User do not have access to update this object");
            }

            oldGeoTag.setDirty(true);
            oldGeoTag.addPositionHistory(oldGeoTag.getCurrentPosition());
            oldGeoTag.setCurrentPosition(geoTag.getCurrentPosition());

            // Sletter tidligere tagger brukeren har på denne taggen slik at det blir kun 1 tag per bruker og urn.
            for (Iterator<GeoPosition> iterator = oldGeoTag.getPositionHistory().iterator(); iterator.hasNext(); ) {
                GeoPosition geoPosition = iterator.next();
                if (geoPosition.getUserId().equals(currentUser)) {
                    iterator.remove();
                }
            }

            geoTagRepository.save(oldGeoTag);

            return oldGeoTag;
        }
        else {
            geoTag.setSticky(false);
            geoTag.setDirty(true);
            geoTag.setPositionHistory(null);
            geoTag.getLinks().clear();
            geoTag.setGeoId(UUID.randomUUID().toString());
            geoTagRepository.save(geoTag);

            return geoTag;
        }
    }

    @Override
    public GeoTag update(String id, GeoTag geoTag) {
        geoTag.getLinks().clear();
        geoTag.setGeoId(id);
        geoTagRepository.save(geoTag);
        return geoTag;
    }

    @Override
    public Page<GeoTag> nearby(GeoQuery query, int page, int size) {
        Point position = new Point(query.getLon(), query.getLat());
        Distance distance = new Distance(query.getMaxDistance(), Metrics.KILOMETERS);
        Page<GeoTag> pages = geoTagRepository.findByCurrentPositionPositionNear(position, distance, new PageRequest(page, size));

        // If not admin then remove some fields
        if (nbUserService.getNBUser() == null || !nbUserService.getNBUser().getAuthorities().contains(new SimpleGrantedAuthority(Constants.ADMIN_ROLE))) {
            for (GeoTag geoTag : pages.getContent()) {
                geoTag.mask();
            }
        }

        return pages;
    }

    @Override
    public Page<GeoTag> within(GeoQuery query, int page, int size) {
        Box box = new Box(new Point(query.getLon(), query.getLat()), new Point(query.getSecondLon(), query.getSecondLat()));
        Page<GeoTag> pages = geoTagRepository.findByCurrentPositionPositionWithin(box, new PageRequest(page, size));

        // If not admin then remove some fields
        if (nbUserService.getNBUser() == null || !nbUserService.getNBUser().getAuthorities().contains(new SimpleGrantedAuthority(Constants.ADMIN_ROLE))) {
            for (GeoTag geoTag : pages.getContent()) {
                geoTag.mask();
            }
        }

        return pages;
    }
}
