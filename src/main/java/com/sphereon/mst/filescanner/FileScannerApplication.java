/**
 * Copyright 2019 Systems of Trust BV
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sphereon.mst.filescanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@SpringBootApplication
@EnableConfigurationProperties(ConfigProperties.class)
public class FileScannerApplication implements CommandLineRunner {

    private static Logger logger = LoggerFactory.getLogger(FileScannerApplication.class);
    @Autowired
    FileScanner fileScanner;

    public static void main(String[] args) {

        SpringApplication.run(FileScannerApplication.class, args);

    }

    @Override
    public void run(String... args) {


        for (int i = 0; i < args.length; ++i) {
            logger.info("args[{}]: {}", i, args[i]);
        }


        try {

            if (args.length <= 0) {
                throw new FileScannerException("Please provide an output path for the CSV file, eg : input.csv");
            } else if (args.length == 1) {
                throw new FileScannerException("Please provide a scan base path, eg : ./files");
            } else if (args.length != 2) {
                throw new FileScannerException("Only output csv file and scan base path arguments are allowed");
            }
            logger.info("--------- START CUT HERE --------------");
            fileScanner.execute(args[0], args[1]);
            logger.info("--------- END CUT HERE --------------");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }
}
