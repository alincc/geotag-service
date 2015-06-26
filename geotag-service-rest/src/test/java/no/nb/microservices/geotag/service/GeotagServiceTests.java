package no.nb.microservices.geotag.service;

import com.mysema.query.types.expr.BooleanExpression;
import no.nb.microservices.geotag.config.Constants;
import no.nb.microservices.geotag.model.GeoPosition;
import no.nb.microservices.geotag.model.GeoQuery;
import no.nb.microservices.geotag.model.GeoTag;
import no.nb.microservices.geotag.model.QGeoTag;
import no.nb.microservices.geotag.repository.GeoTagRepository;
import no.nb.nbsecurity.NBUserDetails;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by andreasb on 25.06.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class GeotagServiceTests {

    private GeotagService geotagService;

    @Mock
    private GeoTagRepository geoTagRepository;

    @Mock
    private NBUserService nbUserService;

    private static final double DELTA = 1e-15;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        geotagService = new GeotagService(geoTagRepository, nbUserService);
    }

    public void loginAsUser(String userId, String role) {
        List<GrantedAuthority> permissions = new ArrayList<GrantedAuthority>();
        permissions.add(new SimpleGrantedAuthority(role));
        NBUserDetails nbUserDetails = new NBUserDetails("sessionID1234", UUID.fromString(userId), "myusername", "mypassword", true, true, true, true, true, permissions);
        when(nbUserService.getNBUser()).thenReturn(nbUserDetails);
    }

    @Test
    public void queryTest() {
        // Test data
        GeoQuery query = new GeoQuery();
        int page = 0;
        int size = 10;

        // Mock request data
        QGeoTag t = QGeoTag.geoTag;
        BooleanExpression expression = t.urn.isNotNull();
        PageRequest pageRequest = new PageRequest(page, size, new Sort(Sort.Direction.DESC, "date"));

        // Mock response data
        List geotags = new ArrayList<>();
        GeoTag tag1 = new GeoTag("13aa8f23e4b0666a514604fa", "URN:NBN:no-nb_foto_NF.W_50121", "32e68b89a170214633119b1717b45d56", "Wilse, Anders Bee", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 9.052734375, 66.04758417711061, new Date()));
        GeoTag tag2 = new GeoTag("539fedvf3786e7e7fa64f47c", "URN:NBN:no-nb_digifoto_20140228_00094_NB_WF_EDK_129136", "1d5712eb0cf8a6e71a928e15a6c1f9f8", "Widerøe Flyveselskaps flyfoto fra Engerdal kommune : Østli, Engerdal.", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 2.052734375, 61.04758417711061, new Date()));
        GeoTag tag3 = new GeoTag("408fedvf3786e7e7fa64f47c", "URN:NBN:no-nb_digifoto_20140228_00094_NB_WF_EDK_129138", "1d5712eb0cf8a6e71a928e15a6c1f9f7", "Widerøe Flyveselskaps flyfoto fra Engerdal kommune : Østli, Rana.", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 13.052734375, 62.04758417711061, new Date()));
        geotags.add(tag1);
        geotags.add(tag2);
        geotags.add(tag3);
        Page<GeoTag> mockPages = new PageImpl<GeoTag>(geotags);

        // Mock response
        when(geoTagRepository.findAll(expression, pageRequest)).thenReturn(mockPages);

        // Tests
        Page<GeoTag> pages = geotagService.query(query, page, size, null);

        // Asserts
        assertEquals(3, pages.getTotalElements());
        assertEquals(tag1.getGeoId(), pages.getContent().get(0).getGeoId());
        assertEquals(tag2.getGeoId(), pages.getContent().get(1).getGeoId());
        assertEquals(tag3.getGeoId(), pages.getContent().get(2).getGeoId());
    }

    @Test(expected = NoSuchElementException.class)
    public void findOneTest() {
        // Mock response data
        List geotags = new ArrayList<>();
        GeoTag mockTag1 = new GeoTag("13aa8f23e4b0666a514604fa", "URN:NBN:no-nb_foto_NF.W_50121", "32e68b89a170214633119b1717b45d56", "Wilse, Anders Bee", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", "example@example.com", 9.052734375, 66.04758417711061, new Date()));

        GeoTag mockTag2 = new GeoTag("539fedvf3786e7e7fa64f47c", "URN:NBN:no-nb_digifoto_20140228_00094_NB_WF_EDK_129136", "1d5712eb0cf8a6e71a928e15a6c1f9f8", "Widerøe Flyveselskaps flyfoto fra Engerdal kommune : Østli, Engerdal.", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", "example@example.com", 2.052734375, 61.04758417711061, new Date()));
        List<GeoPosition> geoPositions = new ArrayList<>();
        geoPositions.add(new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 13.052734375, 62.04758417711061, new Date()));
        mockTag2.setPositionHistory(geoPositions);

        GeoTag mockTag3 = new GeoTag("408fedvf3786e7e7fa64f47c", "URN:NBN:no-nb_digifoto_20140228_00094_NB_WF_EDK_129138", "1d5712eb0cf8a6e71a928e15a6c1f9f7", "Widerøe Flyveselskaps flyfoto fra Engerdal kommune : Østli, Rana.", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", "example@example.com", 13.052734375, 62.04758417711061, new Date()));

        // Mock response
        when(geoTagRepository.findOne(mockTag1.getGeoId())).thenReturn(mockTag1);
        when(geoTagRepository.findOne(mockTag2.getGeoId())).thenReturn(mockTag2);
        when(geoTagRepository.findOne(mockTag3.getGeoId())).thenReturn(mockTag3);

        // Tests
        loginAsUser("a62eb09d-dbf2-495a-8872-7d16e6911296", Constants.USER_ROLE);
        GeoTag tag1 = geotagService.findOne(mockTag1.getGeoId(), null);
        GeoTag tag2 = geotagService.findOne(mockTag2.getGeoId(), new String[]{"positionHistory"});

        loginAsUser("a62eb09d-dbf2-495a-8872-7d16e6911296", Constants.ADMIN_ROLE);
        GeoTag tag3 = geotagService.findOne(mockTag3.getGeoId(), null);
        GeoTag tag4 = geotagService.findOne("333fedvf3796e7e7fa64f50v", null);

        // Asserts
        assertTrue(tag1 != null);
        assertTrue(tag1.getCurrentPosition().getUserEmail() == null);
        assertTrue(tag1.getPositionHistory() == null);
        assertTrue(tag2.getPositionHistory() != null);
        assertTrue(tag3 != null);
        assertTrue(tag3.getCurrentPosition().getUserEmail() != null);
        assertTrue(tag4 == null);
    }

    @Test
    public void deleteTest() {
        // Mock response data
        GeoTag mockTag1 = new GeoTag("539fedvf3786e7e7fa64f47c", "URN:NBN:no-nb_digifoto_20140228_00094_NB_WF_EDK_129136", "1d5712eb0cf8a6e71a928e15a6c1f9f8", "Widerøe Flyveselskaps flyfoto fra Engerdal kommune : Østli, Engerdal.", new GeoPosition("posid1", "a62eb09d-dbf2-495a-8872-7d16e6911296", "example@example.com", 2.052734375, 61.04758417711061, new Date()));
        List<GeoPosition> geoPositions = new ArrayList<>();
        geoPositions.add(new GeoPosition("posid1", "a62eb09d-dbf2-495a-8872-7d16e6911296", "example@example.com", 13.052734375, 62.04758417711061, new Date()));
        mockTag1.setPositionHistory(geoPositions);
        // Mock response
        when(geoTagRepository.findOne(mockTag1.getGeoId())).thenReturn(mockTag1);

        // Tests
        geotagService.delete(mockTag1.getGeoId(), mockTag1.getCurrentPosition().getPosId());

        verify(geoTagRepository, times(1)).findOne(mockTag1.getGeoId());
        verify(geoTagRepository, times(1)).save(any(GeoTag.class));
    }

    @Test
    public void saveNewTest() {
        // Test data
        GeoTag tag1 = new GeoTag("13aa8f23e4b0666a514604fa", "URN:NBN:no-nb_foto_NF.W_50121", "32e68b89a170214633119b1717b45d56", "Wilse, Anders Bee", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 9.052734375, 66.04758417711061, new Date()));

        // Mock response data
        Page<GeoTag> mockPages = new PageImpl<GeoTag>(new ArrayList<>());

        // Mock response
        when(geoTagRepository.findByUrn(anyString(), any(Pageable.class))).thenReturn(mockPages);
        loginAsUser("a62eb09d-dbf2-495a-8872-7d16e6911296", Constants.USER_ROLE);

        // Tests
        GeoTag savedTag = geotagService.save(tag1);

        // Asserts
        assertEquals("a62eb09d-dbf2-495a-8872-7d16e6911296", savedTag.getCurrentPosition().getUserId());
        assertTrue(savedTag.getUrn().equals(tag1.getUrn()));
        assertTrue(savedTag.getCurrentPosition().getPosId() != null);
        assertTrue(savedTag.isDirty());
        assertFalse(savedTag.isSticky());
        verify(nbUserService, times(1)).getNBUser();
        verify(geoTagRepository, times(1)).findByUrn(tag1.getUrn(), new PageRequest(0, 1));
    }

    @Test
    public void saveOldTest() {
        // Test data
        GeoTag tag1 = new GeoTag("13aa8f23e4b0666a514604fa", "URN:NBN:no-nb_foto_NF.W_50121", "32e68b89a170214633119b1717b45d56", "Wilse, Anders Bee", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 9.052734375, 66.04758417711061, new Date()));
        GeoTag tag2 = new GeoTag("13aa8f23e4b0666a514604fa", "URN:NBN:no-nb_foto_NF.W_50121", "32e68b89a170214633119b1717b45d56", "Wilse, Anders Bee", new GeoPosition("b62eb09d-dbf2-495a-8872-7d16e6911296", 10.13245458, 67.54454568811200, new Date()));

        // Mock response data
        List geotags = new ArrayList<>();
        geotags.add(tag2);
        Page<GeoTag> mockPages = new PageImpl<GeoTag>(geotags);

        // Mock response
        when(geoTagRepository.findByUrn(anyString(), any(Pageable.class))).thenReturn(mockPages);
        loginAsUser("a62eb09d-dbf2-495a-8872-7d16e6911296", Constants.USER_ROLE);

        // Tests
        GeoTag savedTag = geotagService.save(tag1);

        // Asserts
        assertEquals("a62eb09d-dbf2-495a-8872-7d16e6911296", savedTag.getCurrentPosition().getUserId());
        assertEquals(tag1.getCurrentPosition().getLatitude(), savedTag.getCurrentPosition().getLatitude(), DELTA);
        assertEquals(tag1.getCurrentPosition().getLongitude(), savedTag.getCurrentPosition().getLongitude(), DELTA);
        assertEquals(tag1.getCurrentPosition().getUserId(), savedTag.getCurrentPosition().getUserId());
        assertEquals(10.13245458, savedTag.getPositionHistory().get(0).getLongitude(), DELTA);
        assertEquals(67.54454568811200, savedTag.getPositionHistory().get(0).getLatitude(), DELTA);
        assertEquals("b62eb09d-dbf2-495a-8872-7d16e6911296", savedTag.getPositionHistory().get(0).getUserId());
        assertTrue(savedTag.getUrn().equals(tag1.getUrn()));
        assertTrue(savedTag.getCurrentPosition().getPosId() != null);
        assertTrue(savedTag.isDirty());
        verify(nbUserService, times(1)).getNBUser();
        verify(geoTagRepository, times(1)).findByUrn(tag1.getUrn(), new PageRequest(0, 1));
    }

    @Test
    public void updateTest() {
        // Test data
        GeoTag tag1 = new GeoTag("13aa8f23e4b0666a514604fa", "URN:NBN:no-nb_foto_NF.W_50121", "32e68b89a170214633119b1717b45d56", "Wilse, Anders Bee", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 9.052734375, 66.04758417711061, new Date()));

        // Tests
        GeoTag savedTag = geotagService.update(tag1.getGeoId(), tag1);

        // Asserts
        verify(geoTagRepository, times(1)).save(any(GeoTag.class));
    }
}
