# MST Filescanner

This quick and dirty program reads a scan path provided as 2nd argument recursively for Tiff files. It then determines the hash of the file;
reads barcodes on the first page and copies the file to a target directory provided in the application properties, in a subdirectory that is the hash.
The info gets written into a CSV file for later processing.

IIt omits already processed files, so the program is idempotent. It also deduplicates files with existing hashes and stores the original locations in the CSV.

## Running the program
To run the program execute the following command from the directory where you extracted the program:

 ```
 filescanner.bat <output-csv-file> <scan-path>
 ```
 
 So for instance
 
 ```
 filescanner.bat /Users/nklomp/Example-Output.csv /Random/Scans
 ```
 
 If you want to run it yourself without the cmd script because you are using another Operating System then Windows, execute:
 
 ```
 java -jar filescanner*.jar <output-csv-file> <scan-path>
 ```
 