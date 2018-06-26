package org.dhus.store.datastore.hfs;

import fr.gael.dhus.util.MultipleDigestOutputStream;
import org.apache.commons.io.FileUtils;
import org.dhus.Product;
import org.dhus.ProductConstants;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.ReadOnlyDataStoreException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
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
        if(ProductConstants.IMAGE_TYPE.equals(product.getProperty(ProductConstants.PRODUCT_TYPE_PROPERTY)))
            return putImage(product);

        return putFullProduct(product);
    }

    private String putFullProduct(Product product) throws IOException {
        // Compute the target
        File source = product.getImpl(File.class);

        super.computeAndSetChecksum(source, product);

        product.setProperty(ProductConstants.DATA_SIZE, source.length());
        return HfsDataStoreUtils.generateResource(hfs.getPath(), source.getAbsolutePath());

    }

    private String putImage(Product product) throws IOException{
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
