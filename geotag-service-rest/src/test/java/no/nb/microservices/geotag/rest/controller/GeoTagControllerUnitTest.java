package no.nb.microservices.geotag.rest.controller;

import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.BooleanExpression;
import no.nb.microservices.geotag.config.ApplicationSettings;
import no.nb.microservices.geotag.config.TestContext;
import no.nb.microservices.geotag.model.GeoTag;
import no.nb.microservices.geotag.model.QGeoTag;
import no.nb.microservices.geotag.repository.GeoTagRepository;
import no.nb.microservices.geotag.rest.assembler.GeoTagResourceAssembler;
import no.nb.microservices.geotag.service.NBUserService;
import no.nb.nbsecurity.NBUserDetails;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by Andreas Bjørnådal (andreasb) on 25.08.14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestContext.class})
@WebAppConfiguration
public class GeoTagControllerUnitTest  {

    public static final String USER_ID = "b62eb09d-dbf2-495a-8872-7d16e6911296";
    public static final String USER_ID_2 = "edcf81fd-26ce-43a5-abca-e30a01397b39";

    @Mock
    private GeoTagRepository geoTagRepository;

    @Mock
    private NBUserService nbUserService;

    @Autowired
    private ApplicationSettings applicationSettings;

    private GeoTagController geoTagController;

    private MockMvc mockMvc;

    private List<GeoTag> geoTagList = new ArrayList<GeoTag>();

    @Before
    public void setupTest() throws Exception {
        MockitoAnnotations.initMocks(this);

        List<GrantedAuthority> permissions = new ArrayList<GrantedAuthority>();
        permissions.add(new SimpleGrantedAuthority("ROLE_USER"));
        NBUserDetails nbUserDetails = new NBUserDetails("sessionID1234", UUID.fromString(USER_ID), "myusername", "mypassword", true, true, true, true, true, permissions);
        when(nbUserService.getNBUser()).thenReturn(nbUserDetails);

        geoTagController = new GeoTagController(nbUserService, geoTagRepository, new GeoTagResourceAssembler(applicationSettings), applicationSettings);

        mockMvc = MockMvcBuilders.standaloneSetup(geoTagController).build();

        //GeoTag(String id, String userID, String urn, String sesamid, String longitude, String latitude, Date date, String title)
        GeoTag tag1 = new GeoTag("13aa8f23e4b0666a514604fa", "a62eb09d-dbf2-495a-8872-7d16e6911296", "URN:NBN:no-nb_foto_NF.W_50121",
                "32e68b89a170214633119b1717b45d56",9.052734375, 66.04758417711061, new Date(),
                "Wilse, Anders Bee");

        GeoTag tag2 = new GeoTag("539fedvf3786e7e7fa64f47c", "a62eb09d-dbf2-495a-8872-7d16e6911296", "URN:NBN:no-nb_digifoto_20140228_00094_NB_WF_EDK_129136",
                "1d5712eb0cf8a6e71a928e15a6c1f9f8",12.052734375, 61.04758417711061, new Date(),
                "Widerøe Flyveselskaps flyfoto fra Engerdal kommune : Østli, Engerdal.");

        GeoTag tag3 = new GeoTag("408fedvf3786e7e7fa64f47c", "a62eb09d-dbf2-495a-8872-7d16e6911296", "URN:NBN:no-nb_digifoto_20140228_00094_NB_WF_EDK_129138",
                "1d5712eb0cf8a6e71a928e15a6c1f9f7",13.052734375, 62.04758417711061, new Date(),
                "Widerøe Flyveselskaps flyfoto fra Engerdal kommune : Østli, Rana.");

        geoTagList.addAll(Arrays.asList(tag1, tag2, tag3));
    }

    @Test
    public void getAllTags() throws Exception {
        when(geoTagRepository.findAll(any(Predicate.class), any(Pageable.class))).thenReturn(new PageImpl<GeoTag>(geoTagList));

        mockMvc.perform(get("/geotags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[1].id").value(geoTagList.get(1).getGeoId()));

        verify(geoTagRepository, times(1)).findAll(any(Predicate.class), any(Pageable.class));
        Mockito.verifyNoMoreInteractions(geoTagRepository);
    }

    @Test
    public void getTagByUrn_Found() throws Exception {
        GeoTag tag1 = geoTagList.get(1);

        QGeoTag t = QGeoTag.geoTag;
        BooleanExpression expression = t.urn.isNotNull().and(t.urn.eq(tag1.getUrn()));

        when(geoTagRepository.findAll(eq(expression), any(Pageable.class))).thenReturn(new PageImpl<GeoTag>(Arrays.asList(tag1)));

        mockMvc.perform(get("/geotags")
                .param("urn", tag1.getUrn()))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(status().isOk());
    }

    @Test
    public void getTagByUrn_NotFound() throws Exception {
        GeoTag tag1 = geoTagList.get(0);

        when(geoTagRepository.findByUrn(eq("URN:NBN:no-nb_foto_NF.W_50121"), any(Pageable.class))).thenReturn(new PageImpl<GeoTag>(Arrays.asList(tag1)));

        mockMvc.perform(get("/geotags")
                .param("urn", "URN:NBN:no-nb_film123tull"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    public void getTagByID_Found() throws Exception {
        GeoTag tag1 = geoTagList.get(0);

        when(geoTagRepository.findOne(tag1.getGeoId())).thenReturn(tag1);

        mockMvc.perform(get("/geotags/{id}", tag1.getGeoId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tag1.getGeoId()));
    }

    @Test
    public void getTagByID_NotFound() throws Exception {
        GeoTag tag1 = geoTagList.get(0);

        when(geoTagRepository.findOne(tag1.getGeoId())).thenReturn(tag1);

        mockMvc.perform(get("/geotags/{id}", "dummyID"))
                .andExpect(status().isNotFound());
    }
}
