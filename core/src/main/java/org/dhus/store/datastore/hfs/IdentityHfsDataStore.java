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

    private String put(Product product) throws IOException {
        // Compute the target
        File source = product.getImpl(File.class);

        super.computeAndSetChecksum(source, product);

        product.setProperty(ProductConstants.DATA_SIZE, source.length());
        return HfsDataStoreUtils.generateResource(hfs.getPath(), source.getAbsolutePath());

    }


}
