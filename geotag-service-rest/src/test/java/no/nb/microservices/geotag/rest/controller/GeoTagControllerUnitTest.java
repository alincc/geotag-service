package no.nb.microservices.geotag.rest.controller;

import no.nb.microservices.geotag.config.ApplicationSettings;
import no.nb.microservices.geotag.model.GeoPosition;
import no.nb.microservices.geotag.model.GeoQuery;
import no.nb.microservices.geotag.model.GeoTag;
import no.nb.microservices.geotag.repository.GeoTagRepository;
import no.nb.microservices.geotag.rest.assembler.GeoTagResourceAssembler;
import no.nb.microservices.geotag.service.GeotagService;
import no.nb.microservices.geotag.service.NBUserService;
import no.nb.nbsecurity.NBUserDetails;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
@RunWith(MockitoJUnitRunner.class)
public class GeoTagControllerUnitTest  {

    public static final String USER_ID = "b62eb09d-dbf2-495a-8872-7d16e6911296";
    public static final String USER_ID_2 = "edcf81fd-26ce-43a5-abca-e30a01397b39";

    @Mock
    private GeoTagRepository geoTagRepository;

    @Mock
    private GeotagService geotagService;

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

        geoTagController = new GeoTagController(nbUserService, new GeoTagResourceAssembler(applicationSettings), applicationSettings, geotagService);

        mockMvc = MockMvcBuilders.standaloneSetup(geoTagController).build();

        //GeoTag(String id, String userID, String urn, String sesamid, String longitude, String latitude, Date date, String title)
        GeoTag tag1 = new GeoTag("13aa8f23e4b0666a514604fa", "URN:NBN:no-nb_foto_NF.W_50121",
                "32e68b89a170214633119b1717b45d56", "Wilse, Anders Bee", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 9.052734375, 66.04758417711061, new Date()));

        GeoTag tag2 = new GeoTag("539fedvf3786e7e7fa64f47c", "URN:NBN:no-nb_digifoto_20140228_00094_NB_WF_EDK_129136", "1d5712eb0cf8a6e71a928e15a6c1f9f8",
                "Widerøe Flyveselskaps flyfoto fra Engerdal kommune : Østli, Engerdal.", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 2.052734375, 61.04758417711061, new Date()));

        GeoTag tag3 = new GeoTag("408fedvf3786e7e7fa64f47c", "URN:NBN:no-nb_digifoto_20140228_00094_NB_WF_EDK_129138", "1d5712eb0cf8a6e71a928e15a6c1f9f7",
                "Widerøe Flyveselskaps flyfoto fra Engerdal kommune : Østli, Rana.", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 13.052734375, 62.04758417711061, new Date()));

        geoTagList.addAll(Arrays.asList(tag1, tag2, tag3));
    }

    @Test
    public void getAllTags() throws Exception {
        when(geotagService.query(any(GeoQuery.class), anyInt(), anyInt(), any(String[].class))).thenReturn(new PageImpl<GeoTag>(geoTagList));

        mockMvc.perform(get("/geotags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[1].id").value(geoTagList.get(1).getGeoId()));

        verify(geotagService, times(1)).query(any(GeoQuery.class), anyInt(), anyInt(), any(String[].class));
        Mockito.verifyNoMoreInteractions(geotagService);
    }

    @Test
    public void getTagByUrn_Found() throws Exception {
        GeoTag tag1 = geoTagList.get(1);

        when(geotagService.query(any(GeoQuery.class), anyInt(), anyInt(), any(String[].class))).thenReturn(new PageImpl<GeoTag>(Arrays.asList(tag1)));

        mockMvc.perform(get("/geotags")
                .param("urn", tag1.getUrn()))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(status().isOk());
    }

    @Test
    public void getTagByUrn_NotFound() throws Exception {
        GeoTag tag1 = geoTagList.get(0);

        GeoQuery query = new GeoQuery();
        query.setUrn("URN:NBN:no-nb_foto_NF.W_50121");

        when(geotagService.query(eq(query), anyInt(), anyInt(), any(String[].class))).thenReturn(new PageImpl<GeoTag>(Arrays.asList(tag1)));

        mockMvc.perform(get("/geotags")
                .param("urn", "URN:NBN:no-nb_film123tull"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    public void getTagByID_Found() throws Exception {
        GeoTag tag1 = geoTagList.get(0);

        when(geotagService.findOne(eq(tag1.getGeoId()), any(String[].class))).thenReturn(tag1);

        mockMvc.perform(get("/geotags/{id}", tag1.getGeoId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tag1.getGeoId()));
    }

    @Test
    public void getTagByID_NotFound() throws Exception {
        GeoTag tag1 = geoTagList.get(0);

        when(geotagService.findOne(anyString(), any(String[].class))).thenThrow(new NoSuchElementException("Geotag not found"));

        mockMvc.perform(get("/geotags/{id}", "dummyID"))
                .andExpect(status().isNotFound());
    }
}
