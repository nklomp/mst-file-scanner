package com.sphereon.mst.filescanner;

import com.aspose.barcode.License;
import com.aspose.barcode.barcoderecognition.BarCodeReader;
import com.aspose.barcode.barcoderecognition.DecodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


@Component
public class Barcodes {

    private final static Logger logger = LoggerFactory.getLogger(Barcodes.class);
    private final ImageReader tiffReader;
    private final ConfigProperties configProperties;

    @Autowired
    public Barcodes(ConfigProperties configProperties) {
        this.configProperties = configProperties;
        Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReadersBySuffix("tiff");
        if (!readers.hasNext()) {
            throw new RuntimeException("No tiff image reader was found on the classpath");
        }
        this.tiffReader = readers.next();

        License license = new License();
        try {
            license.setLicense(configProperties.getLicenseFile());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public List<String> readFirstPage(File file) throws IOException {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new RuntimeException("Could not read barcodes for non existing file " + file == null ? "<no file>" : file.getAbsolutePath());
        }

        try (ImageInputStream is = ImageIO.createImageInputStream(file)) {
            tiffReader.reset();
            tiffReader.setInput(is, true);
            BufferedImage image = tiffReader.read(0);
            BarCodeReader reader = new BarCodeReader(image, DecodeType.QR);


            List<String> barcodes = new ArrayList<>();
            while (reader.read()) {
                barcodes.add(reader.getCodeText());
            }
            return barcodes;
        }

    }

}
