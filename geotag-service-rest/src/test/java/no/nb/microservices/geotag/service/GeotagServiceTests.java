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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

    private GeoTagService geoTagService;

    @Mock
    private GeoTagRepository geoTagRepository;

    @Mock
    private NBUserService nbUserService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final double DELTA = 1e-15;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        geoTagService = new GeoTagService(geoTagRepository, nbUserService);
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
        GeoTag tag1 = new GeoTag("13aa8f23e4b0666a514604fa", "URN:NBN:no-nb_foto_NF.W_50121",  new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 9.052734375, 66.04758417711061, new Date()));
        GeoTag tag2 = new GeoTag("539fedvf3786e7e7fa64f47c", "URN:NBN:no-nb_digifoto_20140228_00094_NB_WF_EDK_129136", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 2.052734375, 61.04758417711061, new Date()));
        GeoTag tag3 = new GeoTag("408fedvf3786e7e7fa64f47c", "URN:NBN:no-nb_digifoto_20140228_00094_NB_WF_EDK_129138", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 13.052734375, 62.04758417711061, new Date()));
        geotags.add(tag1);
        geotags.add(tag2);
        geotags.add(tag3);
        Page<GeoTag> mockPages = new PageImpl<GeoTag>(geotags);

        // Mock response
        when(geoTagRepository.findAll(expression, pageRequest)).thenReturn(mockPages);

        // Tests
        Page<GeoTag> pages = geoTagService.query(query, page, size, null);

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
        GeoTag mockTag1 = new GeoTag("13aa8f23e4b0666a514604fa", "URN:NBN:no-nb_foto_NF.W_50121", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", "example@example.com", 9.052734375, 66.04758417711061, new Date()));

        GeoTag mockTag2 = new GeoTag("539fedvf3786e7e7fa64f47c", "URN:NBN:no-nb_digifoto_20140228_00094_NB_WF_EDK_129136", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", "example@example.com", 2.052734375, 61.04758417711061, new Date()));
        List<GeoPosition> geoPositions = new ArrayList<>();
        geoPositions.add(new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 13.052734375, 62.04758417711061, new Date()));
        mockTag2.setUserPositions(geoPositions);

        GeoTag mockTag3 = new GeoTag("408fedvf3786e7e7fa64f47c", "URN:NBN:no-nb_digifoto_20140228_00094_NB_WF_EDK_129138", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", "example@example.com", 13.052734375, 62.04758417711061, new Date()));

        // Mock response
        when(geoTagRepository.findOne(mockTag1.getGeoId())).thenReturn(mockTag1);
        when(geoTagRepository.findOne(mockTag2.getGeoId())).thenReturn(mockTag2);
        when(geoTagRepository.findOne(mockTag3.getGeoId())).thenReturn(mockTag3);

        // Tests
        loginAsUser("a62eb09d-dbf2-495a-8872-7d16e6911296", Constants.USER_ROLE);
        GeoTag tag1 = geoTagService.findOne(mockTag1.getGeoId(), null);
        GeoTag tag2 = geoTagService.findOne(mockTag2.getGeoId(), new String[]{"userPositions"});

        loginAsUser("a62eb09d-dbf2-495a-8872-7d16e6911296", Constants.ADMIN_ROLE);
        GeoTag tag3 = geoTagService.findOne(mockTag3.getGeoId(), null);
        GeoTag tag4 = geoTagService.findOne("333fedvf3796e7e7fa64f50v", null);

        // Asserts
        assertNotNull(tag1);
        assertNull(tag1.getCurrentPosition().getUserEmail());
        assertNull(tag1.getUserPositions());
        assertNotNull(tag2.getUserPositions());
        assertNotNull(tag3);
        assertNotNull(tag3.getCurrentPosition().getUserEmail());
        assertNull(tag4);
    }

    @Test(expected = NoSuchElementException.class)
    public void findOnePositionTest() {
        // Mock response data
        List geotags = new ArrayList<>();
        GeoTag mockTag1 = new GeoTag("13aa8f23e4b0666a514604fa", "URN:NBN:no-nb_foto_NF.W_50121", new GeoPosition("pos10", "a62eb09d-dbf2-495a-8872-7d16e6911296", "example@example.com", 9.052734375, 66.04758417711061, new Date()));

        GeoTag mockTag2 = new GeoTag("539fedvf3786e7e7fa64f47c", "URN:NBN:no-nb_digifoto_20140228_00094_NB_WF_EDK_129136", new GeoPosition("pos20", "a62eb09d-dbf2-495a-8872-7d16e6911296", "example@example.com", 2.052734375, 61.04758417711061, new Date()));
        List<GeoPosition> geoPositions = new ArrayList<>();
        geoPositions.add(new GeoPosition("pos21", "a62eb09d-dbf2-495a-8872-7d16e6911296", "example@example.com", 13.052734375, 62.04758417711061, new Date()));
        mockTag2.setUserPositions(geoPositions);

        GeoTag mockTag3 = new GeoTag("408fedvf3786e7e7fa64f47c", "URN:NBN:no-nb_digifoto_20140228_00094_NB_WF_EDK_129138", new GeoPosition("pos30", "a62eb09d-dbf2-495a-8872-7d16e6911296", "example@example.com", 13.052734375, 62.04758417711061, new Date()));

        // Mock response
        when(geoTagRepository.findOne(mockTag1.getGeoId())).thenReturn(mockTag1);
        when(geoTagRepository.findOne(mockTag2.getGeoId())).thenReturn(mockTag2);
        when(geoTagRepository.findOne(mockTag3.getGeoId())).thenReturn(mockTag3);

        // Tests
        loginAsUser("a62eb09d-dbf2-495a-8872-7d16e6911296", Constants.USER_ROLE);
        GeoPosition pos1 = geoTagService.findOnePosition(mockTag1.getGeoId(), "pos10");
        GeoPosition pos2 = geoTagService.findOnePosition(mockTag2.getGeoId(), "pos21");

        loginAsUser("a62eb09d-dbf2-495a-8872-7d16e6911296", Constants.ADMIN_ROLE);
        GeoPosition pos3 = geoTagService.findOnePosition(mockTag3.getGeoId(), "pos30");
        GeoPosition pos4 = geoTagService.findOnePosition("333fedvf3796e7e7fa64f50v", "pos30");
        GeoPosition pos5 = geoTagService.findOnePosition(mockTag3.getGeoId(), "dummypos");

        assertEquals("pos10", pos1.getPosId());
        assertEquals(mockTag1.getCurrentPosition().getLatitude(), pos1.getLatitude());
        assertEquals(mockTag1.getCurrentPosition().getLongitude(), pos1.getLongitude());
        assertEquals("pos21", pos2.getPosId());
        assertEquals("pos30", pos3.getPosId());
        assertNull(pos4);
        thrown.expect(NoSuchElementException.class);
        assertNull(pos5);
    }


    @Test
    public void deleteTagTest() {
        // Mock response data
        GeoTag mockTag1 = new GeoTag("539fedvf3786e7e7fa64f47c", "URN:NBN:no-nb_digifoto_20140228_00094_NB_WF_EDK_129136", new GeoPosition("posid1", "a62eb09d-dbf2-495a-8872-7d16e6911296", "example@example.com", 2.052734375, 61.04758417711061, new Date()));
        List<GeoPosition> geoPositions = new ArrayList<>();
        geoPositions.add(new GeoPosition("posid1", "a62eb09d-dbf2-495a-8872-7d16e6911296", "example@example.com", 13.052734375, 62.04758417711061, new Date()));
        mockTag1.setUserPositions(geoPositions);

        // Tests
        geoTagService.delete(mockTag1.getGeoId());

        verify(geoTagRepository, times(1)).delete(anyString());
    }

    @Test
    public void deletePositionTest() {
        // Mock response data
        GeoTag mockTag1 = new GeoTag("539fedvf3786e7e7fa64f47c", "URN:NBN:no-nb_digifoto_20140228_00094_NB_WF_EDK_129136", new GeoPosition("posid1", "a62eb09d-dbf2-495a-8872-7d16e6911296", "example@example.com", 2.052734375, 61.04758417711061, new Date()));
        List<GeoPosition> geoPositions = new ArrayList<>();
        geoPositions.add(new GeoPosition("posid1", "a62eb09d-dbf2-495a-8872-7d16e6911296", "example@example.com", 13.052734375, 62.04758417711061, new Date()));
        mockTag1.setUserPositions(geoPositions);
        // Mock response
        when(geoTagRepository.findOne(mockTag1.getGeoId())).thenReturn(mockTag1);

        // Tests
        geoTagService.deletePosition(mockTag1.getGeoId(), mockTag1.getCurrentPosition().getPosId());

        verify(geoTagRepository, times(1)).findOne(mockTag1.getGeoId());
        verify(geoTagRepository, times(1)).save(any(GeoTag.class));
    }

    @Test
    public void saveNewTest() {
        // Test data
        GeoTag tag1 = new GeoTag("13aa8f23e4b0666a514604fa", "URN:NBN:no-nb_foto_NF.W_50121", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 9.052734375, 66.04758417711061, new Date()));

        // Mock response data
        Page<GeoTag> mockPages = new PageImpl<GeoTag>(new ArrayList<>());

        // Mock response
        when(geoTagRepository.findByUrn(anyString(), any(Pageable.class))).thenReturn(mockPages);
        loginAsUser("a62eb09d-dbf2-495a-8872-7d16e6911296", Constants.USER_ROLE);

        // Tests
        GeoTag savedTag = geoTagService.save(tag1);

        // Asserts
        assertEquals("a62eb09d-dbf2-495a-8872-7d16e6911296", savedTag.getCurrentPosition().getUserId());
        assertTrue(savedTag.getUrn().equals(tag1.getUrn()));
        assertNotNull(savedTag.getCurrentPosition().getPosId());
        assertTrue(savedTag.isDirty());
        assertFalse(savedTag.isSticky());
        verify(nbUserService, times(1)).getNBUser();
        verify(geoTagRepository, times(1)).findByUrn(tag1.getUrn(), new PageRequest(0, 1));
    }

    @Test
    public void saveOldTest() {
        // Test data
        GeoTag tag1 = new GeoTag("13aa8f23e4b0666a514604fa", "URN:NBN:no-nb_foto_NF.W_50121", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 9.052734375, 66.04758417711061, new Date()));
        GeoTag tag2 = new GeoTag("13aa8f23e4b0666a514604fa", "URN:NBN:no-nb_foto_NF.W_50121", new GeoPosition("b62eb09d-dbf2-495a-8872-7d16e6911296", 10.13245458, 67.54454568811200, new Date()));

        // Mock response data
        List geotags = new ArrayList<>();
        geotags.add(tag2);
        Page<GeoTag> mockPages = new PageImpl<GeoTag>(geotags);

        // Mock response
        when(geoTagRepository.findByUrn(anyString(), any(Pageable.class))).thenReturn(mockPages);
        loginAsUser("a62eb09d-dbf2-495a-8872-7d16e6911296", Constants.USER_ROLE);

        // Tests
        GeoTag savedTag = geoTagService.save(tag1);

        // Asserts
        assertEquals("a62eb09d-dbf2-495a-8872-7d16e6911296", savedTag.getCurrentPosition().getUserId());
        assertEquals(tag1.getCurrentPosition().getLatitude(), savedTag.getCurrentPosition().getLatitude(), DELTA);
        assertEquals(tag1.getCurrentPosition().getLongitude(), savedTag.getCurrentPosition().getLongitude(), DELTA);
        assertEquals(tag1.getCurrentPosition().getUserId(), savedTag.getCurrentPosition().getUserId());
        assertEquals(10.13245458, savedTag.getUserPositions().get(0).getLongitude(), DELTA);
        assertEquals(67.54454568811200, savedTag.getUserPositions().get(0).getLatitude(), DELTA);
        assertEquals("b62eb09d-dbf2-495a-8872-7d16e6911296", savedTag.getUserPositions().get(0).getUserId());
        assertTrue(savedTag.getUrn().equals(tag1.getUrn()));
        assertTrue(savedTag.getCurrentPosition().getPosId() != null);
        assertTrue(savedTag.isDirty());
        verify(nbUserService, times(1)).getNBUser();
        verify(geoTagRepository, times(1)).findByUrn(tag1.getUrn(), new PageRequest(0, 1));
    }

    @Test(expected = NoSuchElementException.class)
    public void savePositionTest() {
        GeoTag tag1 = new GeoTag("13aa8f23e4b0666a514604fa", "URN:NBN:no-nb_foto_NF.W_50121", new GeoPosition("b62eb09d-dbf2-495a-8872-7d16e6911296", "example@example.com", 10.13245458, 67.54454568811200, new Date()));
        GeoPosition pos1 = new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 9.052734375, 66.04758417711061, new Date());

        // Mock data
        GeoTag tag1Saved = new GeoTag("13aa8f23e4b0666a514604fa", "URN:NBN:no-nb_foto_NF.W_50121", new GeoPosition("b62eb09d-dbf2-495a-8872-7d16e6911296", "example@example.com", 10.13245458, 67.54454568811200, new Date()));
        GeoPosition pos1Saved = new GeoPosition("pos1", "a62eb09d-dbf2-495a-8872-7d16e6911296", "exmaple@example.com", 9.052734375, 66.04758417711061, new Date());
        tag1Saved.getUserPositions().add(pos1Saved);

        when(geoTagRepository.findOne(tag1.getGeoId())).thenReturn(tag1);
        when(geoTagRepository.save(any(GeoTag.class))).thenReturn(tag1Saved);
        GeoPosition savedPos1 = geoTagService.savePosition(tag1.getGeoId(), pos1);

        assertNotNull(savedPos1.getPosId());
        assertEquals(pos1.getLongitude(), savedPos1.getLongitude(), DELTA);

        when(geoTagRepository.findOne(tag1.getGeoId())).thenReturn(null);
        GeoPosition savedPos2 = geoTagService.savePosition(tag1.getGeoId(), pos1);
        assertNull(savedPos2);
        thrown.expect(NoSuchElementException.class);
    }

    @Test
    public void updateTest() {
        // Test data
        GeoTag tag1 = new GeoTag("13aa8f23e4b0666a514604fa", "URN:NBN:no-nb_foto_NF.W_50121", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 9.052734375, 66.04758417711061, new Date()));

        // Tests
        GeoTag savedTag = geoTagService.update(tag1.getGeoId(), tag1);

        // Asserts
        verify(geoTagRepository, times(1)).save(any(GeoTag.class));
    }
}
