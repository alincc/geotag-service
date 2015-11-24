package no.nb.microservices.geotag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import no.nb.microservices.geotag.config.ApplicationSettings;
import no.nb.microservices.geotag.model.GeoPosition;
import no.nb.microservices.geotag.model.GeoTag;
import no.nb.microservices.geotag.repository.GeoTagRepository;
import no.nb.microservices.geotag.rest.assembler.GeoTagResourceAssembler;
import no.nb.microservices.geotag.rest.controller.GeoTagController;
import no.nb.microservices.geotag.service.GeoTagService;
import no.nb.microservices.geotag.service.NBUserService;
import no.nb.nbsecurity.NBUserDetails;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.UnknownHostException;
import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration
public class ApplicationTests {

    public static final String USER_ID = "b62eb09d-dbf2-495a-8872-7d16e6911296";
    public static final String USER_ID_2 = "edcf81fd-26ce-43a5-abca-e30a01397b39";

    @Autowired
    private GeoTagRepository geoTagRepository;

    @Autowired
    private ApplicationSettings applicationSettings;

    private GeoTagService geoTagService;

    @Mock
    private NBUserService nbUserService;

    private ObjectMapper mapper;
    private GeoTagController geoTagController;
    private MockMvc mockMvc;

    private static final MongodStarter starter = MongodStarter.getDefaultInstance();
    private static MongodExecutable _mongodExe;
    private static MongodProcess _mongod;
    private static MongoClient _mongo;
    private static boolean initDb = false;

    public ApplicationTests() throws Exception {
        if (!initDb) {
            initDb = true;

            _mongodExe = starter.prepare(new MongodConfigBuilder()
                    .version(Version.Main.PRODUCTION)
                    .net(new Net("127.0.0.1" ,12345, Network.localhostIsIPv6()))
                    .build());
            _mongod = _mongodExe.start();
            _mongo = new MongoClient("127.0.0.1", 12345);
        }
    }

    @AfterClass
    public static void finalTeardown() throws Exception {
        _mongod.stop();
        _mongodExe.cleanup();
    }

    @Before
    public void setupTest() throws Exception {
        MockitoAnnotations.initMocks(this);
        mapper = new ObjectMapper();
        geoTagService = new GeoTagService(geoTagRepository, nbUserService);
        geoTagController = new GeoTagController(nbUserService, new GeoTagResourceAssembler(applicationSettings), applicationSettings, geoTagService);
        mockMvc = MockMvcBuilders.standaloneSetup(geoTagController).build();
    }

    @After
    public void teardown() throws Exception {
        geoTagRepository.deleteAll();
    }

    @Test
    public void integrationTest1() throws Exception {
        List<GrantedAuthority> permissions = new ArrayList<GrantedAuthority>();
        permissions.add(new SimpleGrantedAuthority("ROLE_TagsAdmin"));
        NBUserDetails nbUserDetails = new NBUserDetails("sessionID1234", UUID.fromString(USER_ID), "myusername", "mypassword", true, true, true, true, true, permissions);
        when(nbUserService.getNBUser()).thenReturn(nbUserDetails);

        //GeoTag(String id, String userID, String urn, String sesamid, String longitude, String latitude, Date date, String title)
        GeoTag tag1 = new GeoTag("", "URN:NBN:no-nb_foto_NF.W_50121", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 9.052734375, 66.04758417711061, new Date()));
        GeoTag tag2 = new GeoTag("", "URN:NBN:nb_digifoto_20140228_00094_NB_WF_EDK_129136", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 12.052734375, 61.04758417711061, new Date()));

        for (GeoTag tag : Arrays.asList(tag1, tag2)) {
            MvcResult result = mockMvc.perform(post("/v1/geotags")
                    .content(mapper.convertValue(tag, JsonNode.class).toString())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andReturn();

            String location = result.getResponse().getHeader("location");
            String id = location.split("/")[2];
            tag.setGeoId(id);
        }

        mockMvc.perform(get("/v1/geotags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        mockMvc.perform(get("/v1/geotags")
                .param("urn", tag1.getUrn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].urn", is(tag1.getUrn())))
                .andExpect(jsonPath("$.content[0].id", notNullValue()))
                .andExpect(jsonPath("$.content[0].currentPosition.userId", is(USER_ID)))
                .andExpect(jsonPath("$.content[0].currentPosition.date", notNullValue()));

        mockMvc.perform(get("/v1/geotags")
                .param("user", USER_ID))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/nearby")
                .param("lon", "9.1487565")
                .param("lat", "65.9954774")
                .param("maxDistance", "10"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/nearby")
                .param("lon", "9.1487565")
                .param("lat", "68.9954774")
                .param("maxDistance", "1"))
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/within")
                .param("lon", "12.000")
                .param("lat", "61.000")
                .param("secondLon", "16.000")
                .param("secondLat", "62.000"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/within")
                .param("lon", "9.000")
                .param("lat", "61.500")
                .param("secondLon", "16.000")
                .param("secondLat", "62.000"))
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/within")
                .param("lon", "9.000")
                .param("lat", "61.000")
                .param("secondLon", "13.000")
                .param("secondLat", "67.000"))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(status().isOk());

        GeoTag geotag1 = new GeoTag("", "URN:NBN:no-nb_digifoto_20131218_00056_NB_WF_LUR_041262", new GeoPosition(USER_ID, 12.835818529129028, 66.40039201638066, new Date()));

        mockMvc.perform(put("/v1/geotags/{tagid}", tag1.getGeoId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.convertValue(geotag1, JsonNode.class).toString()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/geotags/{tagid}", tag1.getGeoId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urn", is(geotag1.getUrn())))
                .andExpect(jsonPath("$.id", is(tag1.getGeoId())))
                .andExpect(jsonPath("$.currentPosition.userId", is(USER_ID)))
                .andExpect(jsonPath("$.currentPosition.date", notNullValue()))
                .andExpect(jsonPath("$.currentPosition.position[0]", is(12.835818529129028)))
                .andExpect(jsonPath("$.currentPosition.position[1]", is(66.40039201638066)));
    }

    @Test
    public void integrationTest2() throws Exception {
        List<GrantedAuthority> permissions = new ArrayList<GrantedAuthority>();
        permissions.add(new SimpleGrantedAuthority("ROLE_USER"));
        NBUserDetails nbUserDetails1 = new NBUserDetails("sessionID1234", UUID.fromString(USER_ID), "myusername", "mypassword", true, true, true, true, true, permissions);
        when(nbUserService.getNBUser()).thenReturn(nbUserDetails1);

        //GeoTag(String id, String userID, String urn, String sesamid, String longitude, String latitude, Date date, String title)
        GeoTag tag1 = new GeoTag("", "URN:NBN:no-nb_foto_NF.W_50121", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 9.052734375, 66.04758417711061, new Date()));
        GeoTag tag2 = new GeoTag("", "URN:NBN:nb_digifoto_20140228_00094_NB_WF_EDK_129136", new GeoPosition("a62eb09d-dbf2-495a-8872-7d16e6911296", 12.052734375, 61.04758417711061, new Date()));

        for (GeoTag tag : Arrays.asList(tag1, tag2)) {
            MvcResult result = mockMvc.perform(post("/v1/geotags")
                    .content(mapper.convertValue(tag, JsonNode.class).toString())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andReturn();

            String location = result.getResponse().getHeader("location");
            String id = location.split("/")[2];
            tag.setGeoId(id);
        }

        mockMvc.perform(get("/v1/geotags")
                .param("urn", "dummyurn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(get("/v1/geotags")
                .param("user", "dummyuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        // Change user
        NBUserDetails nbUserDetails2 = new NBUserDetails("sessionID1234", UUID.fromString(USER_ID_2), "myusername", "mypassword", true, true, true, true, true, permissions);
        when(nbUserService.getNBUser()).thenReturn(nbUserDetails2);

        mockMvc.perform(get("/v1/geotags")
                .param("urn", tag1.getUrn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].urn", is(tag1.getUrn())))
                .andExpect(jsonPath("$.content[0].id", notNullValue()))
                .andExpect(jsonPath("$.content[0].currentPosition.userId", is(nbUserDetails1.getUserId().toString())))
                .andExpect(jsonPath("$.content[0].currentPosition.date").exists());

        GeoTag tag3 = new GeoTag("", "URN:NBN:no-nb_digifoto_20131218_00056_NB_WF_LUR_041262", new GeoPosition(USER_ID, 12.835818529129028, 66.40039201638066, new Date()));

        mockMvc.perform(put("/v1/geotags/{tagid}", tag1.getGeoId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.convertValue(tag3, JsonNode.class).toString()))
                .andExpect(status().isOk());

//        mockMvc.perform(delete("/geotags/{tagid}", tag1.getGeoId()))
//                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/v1/geotags/{tagid}/{posId}", "dummyid", "dummyPos"))
                .andExpect(status().isNotFound());
    }

    @Configuration
    @EnableMongoRepositories
    @ComponentScan(basePackageClasses = { GeoTagRepository.class, GeoTagService.class })
    static class MongoConfiguration extends AbstractMongoConfiguration {

        @Override
        protected String getDatabaseName() {
            return "tagdb";
        }

        @Override
        public Mongo mongo() throws UnknownHostException {
            return new MongoClient("127.0.0.1", 12345);
        }

        @Override
        protected String getMappingBasePackage() {
            return "no.nb.tag.repository";
        }
    }

    @Configuration
    static class Config {
        @Bean
        public ApplicationSettings applicationSettings() {
            ApplicationSettings settings = new ApplicationSettings();
            settings.setNbsokContentUrl("http://www.nb.no/nbsok/nb/{sesamid}");
            settings.setFotoContentUrl("http://www.nb.no/foto/nb/{sesamid}");
            return settings;
        }
    }

}
