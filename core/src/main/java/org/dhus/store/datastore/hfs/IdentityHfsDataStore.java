package org.dhus.store.datastore.hfs;

import fr.gael.dhus.system.init.WorkingDirectory;
import fr.gael.dhus.util.MultipleDigestOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.dhus.Product;
import org.dhus.ProductConstants;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.ReadOnlyDataStoreException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

public class IdentityHfsDataStore extends HfsDataStore {

    public IdentityHfsDataStore(String name, HfsManager hfs, boolean readOnly) {
        super(name, hfs, readOnly);
    }

    @Override
    public void set(String id, Product product) throws DataStoreException {
        if (isReadOnly()) {
            throw new ReadOnlyDataStoreException(getName() + " datastore is read only");
        }

        try {
            String path = put(product);
            putResource(id, path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void move(String id, Product product) throws DataStoreException
    {
        throw new DataStoreException(getName() + " datastore does not support move");

    }

    private String put(Product product) throws IOException {
        if (product.hasImpl(File.class)){
            File source = product.getImpl(File.class);
            if(isInTmpFolder(source.toPath()))
                return putInInbox(product);
            return keepInInbox(product, source);
        }

        return putInputStream(product);
    }

    private boolean isInTmpFolder(Path path) {
        return WorkingDirectory.contains(path);
    }

    private String putInputStream(Product product) throws IOException
    {
        // Compute the target
        File dest = new File(hfs.getNewIncomingPath(), product.getName());

        // Case of source file not supported
        if (product.hasImpl(InputStream.class))
        {
            String[] algorithms = SUPPORTED_ALGORITHMS.split(",");
            try (InputStream source = product.getImpl(InputStream.class))
            {
                try (MultipleDigestOutputStream bos =
                             new MultipleDigestOutputStream(new FileOutputStream(dest), algorithms))
                {
                    IOUtils.copy(source, bos);
                    extractAndSetChecksum(bos, algorithms, product);
                }
                catch (NoSuchAlgorithmException e)
                {
                    // Should be never happen
                    throw new IOException("Invalid supported algorithms !", e);
                }
            }

            return HfsDataStoreUtils.generateResource(hfs.getPath(), dest.getAbsolutePath());
        }
        // Case of data is not a SPI or it does not support both File and InputStream accesses (unlikely)
        throw new IOException("Input product \"" + product.getName() +
                "\" has no defined implementation for access.");
    }

    private String keepInInbox(Product product, File source) throws IOException {
        super.computeAndSetChecksum(source, product);

        product.setProperty(ProductConstants.DATA_SIZE, source.length());
        return HfsDataStoreUtils.generateResource(hfs.getPath(), source.getAbsolutePath());

    }

    private String putInInbox(Product product) throws IOException{
        // Compute the target
        File dest = new File(hfs.getNewIncomingPath(), product.getName());

        // Computes the source
        if (product.hasImpl(File.class)) {
            File source = product.getImpl(File.class);
            String[] algorithms = SUPPORTED_ALGORITHMS.split(",");
            try (MultipleDigestOutputStream outputStream =
                         new MultipleDigestOutputStream(new FileOutputStream(dest), algorithms)) {
                // store and compute checksum
                FileUtils.copyFile(source, outputStream);
                extractAndSetChecksum(outputStream, algorithms, product);
            } catch (NoSuchAlgorithmException e) {
                // Should be never happen
                throw new IOException("Invalid supported algorithms !", e);
            }

        }

        product.setProperty(ProductConstants.DATA_SIZE, dest.length());
        return HfsDataStoreUtils.generateResource(hfs.getPath(), dest.getAbsolutePath());
    }


}
