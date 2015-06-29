package no.nb.microservices.geotag.rest.controller;

import no.nb.microservices.geotag.config.ApplicationSettings;
import no.nb.microservices.geotag.model.GeoPosition;
import no.nb.microservices.geotag.model.GeoTag;
import no.nb.microservices.geotag.repository.GeoTagRepository;
import no.nb.microservices.geotag.rest.assembler.GeoTagResourceAssembler;
import no.nb.microservices.geotag.service.GeoTagService;
import no.nb.microservices.geotag.service.NBUserService;
import no.nb.nbsecurity.NBUserDetails;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by andreasb on 29.06.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class PositionControllerTest {

    public static final String USER_ID = "b62eb09d-dbf2-495a-8872-7d16e6911296";
    public static final String USER_ID_2 = "edcf81fd-26ce-43a5-abca-e30a01397b39";

    @Mock
    private GeoTagRepository geoTagRepository;

    @Mock
    private GeoTagService geoTagService;

    @Mock
    private NBUserService nbUserService;

    @Autowired
    private ApplicationSettings applicationSettings;

    private PositionController positionController;

    private MockMvc mockMvc;

    private List<GeoTag> geoTagList = new ArrayList<GeoTag>();

    @Before
    public void setupTest() throws Exception {
        MockitoAnnotations.initMocks(this);

        List<GrantedAuthority> permissions = new ArrayList<GrantedAuthority>();
        permissions.add(new SimpleGrantedAuthority("ROLE_USER"));
        NBUserDetails nbUserDetails = new NBUserDetails("sessionID1234", UUID.fromString(USER_ID), "myusername", "mypassword", true, true, true, true, true, permissions);
        when(nbUserService.getNBUser()).thenReturn(nbUserDetails);

        positionController = new PositionController(new GeoTagResourceAssembler(applicationSettings), geoTagService);

        mockMvc = MockMvcBuilders.standaloneSetup(positionController).build();

        GeoTag tag1 = new GeoTag("13aa8f23e4b0666a514604fa", "URN:NBN:no-nb_foto_NF.W_50121", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 9.052734375, 66.04758417711061, new Date()));
        GeoTag tag2 = new GeoTag("539fedvf3786e7e7fa64f47c", "URN:NBN:no-nb_digifoto_20140228_00094_NB_WF_EDK_129136", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 2.052734375, 61.04758417711061, new Date()));
        GeoTag tag3 = new GeoTag("408fedvf3786e7e7fa64f47c", "URN:NBN:no-nb_digifoto_20140228_00094_NB_WF_EDK_129138", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 13.052734375, 62.04758417711061, new Date()));

        geoTagList.addAll(Arrays.asList(tag1, tag2, tag3));
    }

    @Test
    public void getPositionsTest() throws Exception {
        GeoTag tag1 = geoTagList.get(0);
        when(geoTagService.findOne(tag1.getGeoId(), new String[] {"positions"})).thenReturn(tag1);

        mockMvc.perform(get("/geotags/{id}/positions", tag1.getGeoId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}