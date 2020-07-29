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
import org.hibernate.validator.constraints.Length;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;

@Configuration
@ConfigurationProperties(prefix = "scanner")
@Validated
public class ConfigProperties {

    private Csv csv = new Csv();
    private Output output = new Output();
    private String licenseFile = "Aspose.Total.Java.lic";

    public Csv getCsv() {
        return csv;
    }

    public void setCsv(Csv csv) {
        this.csv = csv;
    }

    public Output getOutput() {
        return output;
    }

    public void setOutput(Output output) {
        this.output = output;
    }

    public String getLicenseFile() {
        return licenseFile;
    }

    public void setLicenseFile(String licenseFile) {
        this.licenseFile = licenseFile;
    }

    @Validated
    public class Output {
        private String targetDirectory = "output";

        public String getTargetDirectory() {
            return targetDirectory;
        }

        public void setTargetDirectory(String targetDirectory) {
            this.targetDirectory = targetDirectory;
        }
    }

    @Validated
    public class Csv {
        private boolean headerLinePresent = true;
        private String format = "excel";

        @Length(min = 1, max = 1)
        private String delimiter = ";";

        private String charset = StandardCharsets.UTF_8.name();

        public boolean isHeaderLinePresent() {
            return headerLinePresent;
        }

        public void setHeaderLinePresent(boolean headerLinePresent) {
            this.headerLinePresent = headerLinePresent;
        }

        public String getFormat() {
            return format;
        }

        public CSVFormat getFormatEnum() {
            if (StringUtils.isEmpty(format)) {
                return CSVFormat.DEFAULT;
            } else if (format.equalsIgnoreCase("excel")) {
                return CSVFormat.EXCEL;
            }
            return CSVFormat.valueOf(format.toUpperCase());
        }

        public void setFormat(String format) {
            this.format = format;
        }


        public String getDelimiter() {
            return delimiter;
        }

        public void setDelimiter(String delimiter) {
            this.delimiter = delimiter;
        }

        public String getCharset() {
            return charset;
        }

        public void setCharset(String charset) {
            this.charset = charset;
        }
    }

}
