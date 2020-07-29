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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Optional;

@Component
public class DatasetReader {

    private final ConfigProperties configProperties;

    public DatasetReader(ConfigProperties configProperties) {

        this.configProperties = configProperties;
    }

    public Iterable<CSVRecord> parse(File file) {
        return parse(file, Optional.of(configProperties.getCsv().getFormatEnum()));
    }

    public Iterable<CSVRecord> parse(File inputFile, Optional<CSVFormat> inputFormat) {
        try {
            Reader in = new FileReader(inputFile);
            if (in == null) {
                throw new FileScannerException(String.format("Could not find input file %s", inputFile.getAbsolutePath()));
            }

            CSVFormat format = inputFormat.orElse(configProperties.getCsv().getFormatEnum()).withFirstRecordAsHeader().withDelimiter(configProperties.getCsv().getDelimiter().charAt(0));
            return format.parse(in);
        } catch (IOException ioe) {
            throw new FileScannerException(ioe);
        }
    }

}
