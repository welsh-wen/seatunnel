/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.e2e.connector.file.fstp;

import org.apache.seatunnel.e2e.common.TestResource;
import org.apache.seatunnel.e2e.common.TestSuiteBase;
import org.apache.seatunnel.e2e.common.container.TestContainer;
import org.apache.seatunnel.e2e.common.container.TestContainerId;
import org.apache.seatunnel.e2e.common.container.TestHelper;
import org.apache.seatunnel.e2e.common.junit.DisabledOnContainer;
import org.apache.seatunnel.e2e.common.util.ContainerUtil;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startables;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Stream;

@DisabledOnContainer(
        value = {TestContainerId.SPARK_2_4},
        disabledReason = "The apache-compress version is not compatible with apache-poi")
@Slf4j
public class SftpFileIT extends TestSuiteBase implements TestResource {

    private static final String SFTP_IMAGE = "atmoz/sftp:latest";

    private static final String SFTP_CONTAINER_HOST = "sftp";

    private static final int SFTP_PORT = 22;

    private static final int SFTP_BIND_PORT = 2222;

    private static final String USERNAME = "seatunnel";

    private static final String PASSWORD = "pass";

    private GenericContainer<?> sftpContainer;

    @BeforeAll
    @Override
    public void startUp() throws Exception {
        sftpContainer =
                new GenericContainer<>(SFTP_IMAGE)
                        .withEnv("SFTP_USERS", USERNAME + ":" + PASSWORD)
                        .withCommand(USERNAME + ":" + PASSWORD + ":::tmp")
                        .withNetwork(NETWORK)
                        .withNetworkAliases(SFTP_CONTAINER_HOST)
                        .withExposedPorts(SFTP_PORT);

        sftpContainer.setPortBindings(Collections.singletonList(SFTP_BIND_PORT + ":" + SFTP_PORT));
        sftpContainer.start();
        Startables.deepStart(Stream.of(sftpContainer)).join();
        log.info("Sftp container started");

        ContainerUtil.copyFileIntoContainers(
                "/json/e2e.json",
                "/home/seatunnel/tmp/seatunnel/read/json/name=tyrantlucifer/hobby=coding/e2e.json",
                sftpContainer);

        ContainerUtil.copyFileIntoContainers(
                "/text/e2e.txt",
                "/home/seatunnel/tmp/seatunnel/read/text/name=tyrantlucifer/hobby=coding/e2e.txt",
                sftpContainer);

        ContainerUtil.copyFileIntoContainers(
                "/text/e2e-text.zip",
                "/home/seatunnel/tmp/seatunnel/read/zip/text/e2e-text.zip",
                sftpContainer);

        ContainerUtil.copyFileIntoContainers(
                "/excel/e2e.xlsx",
                "/home/seatunnel/tmp/seatunnel/read/excel/name=tyrantlucifer/hobby=coding/e2e.xlsx",
                sftpContainer);

        ContainerUtil.copyFileIntoContainers(
                "/excel/e2e.xlsx",
                "/home/seatunnel/tmp/seatunnel/read/excel_filter/name=tyrantlucifer/hobby=coding/e2e_filter.xlsx",
                sftpContainer);

        ContainerUtil.copyFileIntoContainers(
                "/xml/e2e.xml",
                "/home/seatunnel/tmp/seatunnel/read/xml/name=tyrantlucifer/hobby=coding/e2e.xml",
                sftpContainer);

        sftpContainer.execInContainer("sh", "-c", "chown -R seatunnel /home/seatunnel/tmp/");
    }

    @TestTemplate
    public void testSftpFileReadAndWrite(TestContainer container)
            throws IOException, InterruptedException {
        TestHelper helper = new TestHelper(container);
        // test write sftp excel file
        helper.execute("/excel/fakesource_to_sftp_excel.conf");
        // test read sftp excel file
        helper.execute("/excel/sftp_excel_to_assert.conf");
        // test read sftp excel file with projection
        helper.execute("/excel/sftp_excel_projection_to_assert.conf");
        // test read sftp excel file with filter pattern
        helper.execute("/excel/sftp_filter_excel_to_assert.conf");
        // test write sftp text file
        helper.execute("/text/fake_to_sftp_file_text.conf");
        // test read skip header
        helper.execute("/text/sftp_file_text_skip_headers.conf");
        // test read sftp text file
        helper.execute("/text/sftp_file_text_to_assert.conf");
        // test read sftp text file with projection
        helper.execute("/text/sftp_file_text_projection_to_assert.conf");
        // test read sftp zip text file
        helper.execute("/text/sftp_file_zip_text_to_assert.conf");
        // test write sftp json file
        helper.execute("/json/fake_to_sftp_file_json.conf");
        // test read sftp json file
        helper.execute("/json/sftp_file_json_to_assert.conf");
        // test write sftp xml file
        helper.execute("/xml/fake_to_sftp_file_xml.conf");
        // test read sftp xml file
        helper.execute("/xml/sftp_file_xml_to_assert.conf");
    }

    @AfterAll
    @Override
    public void tearDown() {
        if (sftpContainer != null) {
            sftpContainer.close();
        }
    }
}
