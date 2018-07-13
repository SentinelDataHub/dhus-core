package org.dhus.store.datastore.hfs;

import fr.gael.dhus.system.init.WorkingDirectory;
import fr.gael.dhus.util.MultipleDigestInputStream;
import fr.gael.dhus.util.MultipleDigestOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.dhus.Product;
import org.dhus.ProductConstants;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.ReadOnlyDataStoreException;

import java.io.*;
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

    private String put(Product product) throws IOException
    {
        // Computes the source
        if (product.hasImpl(File.class))
        {
            File source = product.getImpl(File.class);
            File dest = null;

            if (!isInTmpFolder(source.toPath())) //is reading from inbox folder
            {
                dest = source;
                computeAndSetChecksum(dest, product);
            }
            else
            {
                dest = new File(hfs.getNewIncomingPath(), product.getName());
                copyAndProcessFile(product, source, dest);
            }

            product.setProperty(ProductConstants.DATA_SIZE, dest.length());
            return HfsDataStoreUtils.generateResource(hfs.getPath(), dest.getAbsolutePath());
        }

        // Case of source file not supported
        if (product.hasImpl(InputStream.class))
        {
            File dest = new File(hfs.getNewIncomingPath(), product.getName());
            extractAndProcessStream(product, dest);

            return HfsDataStoreUtils.generateResource(hfs.getPath(), dest.getAbsolutePath());
        }
        // Case of data is not a SPI or it does not support both File and InputStream accesses (unlikely)
        throw new IOException("Input product \"" + product.getName() +
                "\" has no defined implementation for access.");
    }

    private boolean isInTmpFolder(Path path) {
        return WorkingDirectory.contains(path);
    }

}
