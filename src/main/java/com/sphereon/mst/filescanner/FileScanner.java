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

import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class FileScanner {

    public static final String HASH = "hash";
    public static final String ORIGINAL_PATH = "original-path";
    private static Logger logger = LoggerFactory.getLogger(FileScanner.class);

    private final ConfigProperties configProperties;
    private final DatasetReader datasetReader;
    private final DatasetWriter datasetWriter;
    private final Barcodes barcodes;
    private static final MessageDigest MD;

    static {
        try {
            MD = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> filesByHash = new HashMap<>();
    private Map<String, String> hashesByPath = new HashMap<>();
    private CSVPrinter printer;

    public FileScanner(ConfigProperties configProperties, DatasetReader datasetReader, DatasetWriter datasetWriter, Barcodes barcodes) {
        this.configProperties = configProperties;
        this.datasetReader = datasetReader;
        this.datasetWriter = datasetWriter;
        this.barcodes = barcodes;
    }


    private static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
        Map<Object, Boolean> map = new ConcurrentHashMap<>();
        return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    public void execute(String outputCsv, String scanPath) throws IOException {
        File csvFile = new File(outputCsv);
        initPrinter(csvFile);
        createCsvFileWithHeaderWhenNeeded(csvFile);
        initProcessedFilesFromCsv(csvFile);

        Files.walk(Paths.get(scanPath), FileVisitOption.FOLLOW_LINKS).
                forEach(filePath ->

                        {
                            File imageFile = filePath.toFile();
                            try {
                                if (!imageFile.exists() || !imageFile.isFile()) {
                                    return;
                                } else if (imageFile.length() == 0) {
                                    logger.warn("File length is 0 : {}", imageFile.getAbsolutePath());
                                    return;
                                } else if (hashesByPath.containsKey(imageFile.getAbsolutePath())) {
                                    logger.info("Already processed file {}", imageFile.getAbsolutePath());
                                    return;
                                } else if (!imageFile.getName().toLowerCase().endsWith("tif")) {
                                    logger.info("Not a tiff file {}", imageFile.getAbsolutePath());
                                    return;
                                }


                                MD.reset();
                                byte buffer[] = new byte[8 * 1024];
                                try (DigestInputStream dis = new DigestInputStream(new FileInputStream(imageFile), MD)) {
                                    while (dis.read(buffer, 0, buffer.length) > 0)
                                        ; //empty loop since we just need to read it all for the MD
                                }
                                StringBuilder sb = new StringBuilder();
                                for (byte b : MD.digest()) {
                                    sb.append(String.format("%02x", b));
                                }
                                String hexHash = sb.toString();

                                File targetDir = new File(configProperties.getOutput().getTargetDirectory() + "/" + hexHash);
                                String destinationPath = targetDir.toPath() + "\\" + imageFile.getName();

                                String existingPath = filesByHash.get(hexHash);
                                if (existingPath == null) {
                                    // Only store the first file with a certain hash.
                                    filesByHash.put(hexHash, imageFile.getAbsolutePath());

                                    if (!targetDir.mkdirs() && !targetDir.exists()) {
                                        logger.warn("Could not create directory {}", targetDir.getAbsolutePath());
                                    }
                                    Path targetPath = targetDir.toPath().resolve(imageFile.getName());
                                    try {
                                        Files.copy(filePath, targetPath, StandardCopyOption.COPY_ATTRIBUTES);
                                        logger.info("Copied file {} with hash {} to target {}", imageFile.getName(), hexHash, destinationPath);
                                    } catch (IOException ioe) {
                                        if (targetPath.toFile().exists() && targetPath.toFile().length() > 0) {
                                            logger.info("Target file already exists: {}", targetPath);
                                        }
                                        else throw ioe;
                                    }

                                } else {
                                    if (imageFile.getAbsoluteFile().equals(existingPath)) {
                                        // Already processed this file before (should have been detected above
                                        return;
                                    } else if (hashesByPath.containsKey(imageFile.getAbsolutePath())) {
                                        logger.info("Already processed duplicate file {} orig {}", imageFile.getAbsolutePath(), filesByHash.get(hexHash));
                                        // Processed it, but was already a duplicate
                                        return;
                                    }
                                    logger.info("Found a duplicate file {} of {}", imageFile.getAbsolutePath(), filesByHash.get(hexHash));
                                }
                                hashesByPath.put(imageFile.getAbsolutePath(), hexHash);

//                                List<BarCodeReader.PossibleBarCode> possibleBarCodes = barcodes.readFirstPage(imageFile);
                                String barcodesString = String.join(" ", barcodes.readFirstPage(imageFile)/*possibleBarCodes.stream().map(BarCodeReader.PossibleBarCode::getCodetext).collect(Collectors.toList())*/);

                               printer.printRecord(imageFile.getAbsoluteFile().getPath(), filePath.getFileName().toString(), imageFile.length(), imageFile.lastModified(), sb, barcodesString, destinationPath, existingPath == null ? "": filesByHash.get(hexHash));
//                                printer.println();
                                printer.flush();


                            } catch (Exception e) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                );
    }

    private void initPrinter(File csvFile) {
        this.printer = datasetWriter.printer(csvFile.toPath(), Optional.empty());
    }

    private void initProcessedFilesFromCsv(File csvFile) {
        this.filesByHash = StreamSupport.stream(datasetReader.parse(csvFile).spliterator(), false)
                .filter(distinctByKey(record -> record.get(HASH)))
                .collect(Collectors.toMap(record -> record.get(HASH), record -> record.get(ORIGINAL_PATH)));

        this.hashesByPath = /*filesByHash.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));*/
          StreamSupport.stream(datasetReader.parse(csvFile).spliterator(), false)
                .collect(Collectors.toMap(record -> record.get(ORIGINAL_PATH), record -> record.get(HASH)));
    }

    private void createCsvFileWithHeaderWhenNeeded(File csvFile) throws IOException {

        if (!csvFile.exists() || csvFile.length() == 0) {
            printer.printRecord(ORIGINAL_PATH, "filename", "size", "modified", HASH, "barcode", "destination-path", "duplicate-of");
            printer.flush();
        }
    }
}
