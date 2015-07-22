package reach.project.utils;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.SecurityUtils;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.Collections;

import reach.project.core.StaticData;

/**
 * Created by Dexter on 28-03-2015.
 */
public enum CloudStorageUtils {
    ;

    private static final String APPLICATION_NAME_PROPERTY = "Reach";
    private static final String ACCOUNT_ID_PROPERTY = "528178870551-a2qc6pb788d3djjmmult1lkloc65rgt4@developer.gserviceaccount.com";
    private static final String BUCKET_NAME_IMAGES = "able-door-616-images";
    private static final String BUCKET_NAME_MUSIC_DATA = "able-door-616-music-data";
//    private final String PROJECT_ID_PROPERTY = "able-door-616";

    public static Optional<String> uploadFile(final File file, InputStream key) {

        String fileName;
        try {
            fileName = Files.hash(file, Hashing.md5()).toString();
        } catch (IOException e) {
            e.printStackTrace();
            fileName = null;
        }

        if (TextUtils.isEmpty(fileName))
            return Optional.absent();

        final Optional<Storage> storage = getStorage(key);
        if (!storage.isPresent())
            return Optional.absent();

        final FileInputStream stream;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return Optional.absent();
        }

        Log.i("Ayush", "Uploading file name " + fileName);
        StaticData.threadPool.submit(new UploadIfNotPresent(storage.get(), fileName, stream));
        return Optional.of(fileName);
    }

    private static final class UploadIfNotPresent implements Runnable {

        final Storage storage;
        final String fileName;
        final InputStream inputStream;

        private UploadIfNotPresent(Storage storage, String fileName, InputStream inputStream) {
            this.storage = storage;
            this.fileName = fileName;
            this.inputStream = inputStream;
        }

        @Override
        public void run() {


            InputStream check = null;
            try {
                check = storage.objects().get(BUCKET_NAME_IMAGES, fileName).executeMediaAsInputStream();
                Log.i("Ayush", "File found" + fileName);
                return;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                MiscUtils.closeAndIgnore(check);
            }

            Log.i("Ayush", "File not found, Uploading " + fileName);
            final InputStreamContent content;
            content = new InputStreamContent(
                    "image/",
                    inputStream);

            //file not present
            MiscUtils.autoRetry(
                    new DoWork<Void>() {
                        @Override
                        protected Void doWork() throws IOException {
                            final Storage.Objects.Insert insert = storage.objects().insert(BUCKET_NAME_IMAGES, null, content);
                            insert.setPredefinedAcl("publicRead");
                            insert.setName(fileName);
                            insert.execute();
                            Log.i("Ayush", "Upload complete");
                            return null;
                        }
                    }, Optional.<Predicate<Void>>absent());
        }
    }

//    public byte [] downloadFile(String fileName) {
//
//        try {
//            getStorage();
//            final ByteArrayOutputStream download = new ByteArrayOutputStream();
//            BufferedInputStream bufferedInputStream = null;
//            try {
//                Log.i("Ayush", "Attempting to download " + fileName);
//                Storage.Objects.Get get = storage.objects().get(StaticData.BUCKET_NAME, fileName);
//
//                bufferedInputStream = new BufferedInputStream(get.executeMediaAsInputStream());
//                byte [] bytes = new byte[5000];
//                int read;
//                int total = 0;
//                read = bufferedInputStream.read(bytes);
//                while(read != -1) {
//
//                    total += read;
//                    byte [] toCopy = new byte[read];
//                    System.arraycopy(bytes, 0, toCopy, 0, read);
//                    download.write(toCopy);
//                    read = bufferedInputStream.read(bytes);
//                }
//            } catch (GoogleJsonResponseException |
//                     UnknownHostException |
//                     SocketTimeoutException e) {
//
//                e.printStackTrace();
//                Log.i("Ayush", "Network error man");
//                download.close();
//                if (bufferedInputStream != null) {
//                    bufferedInputStream.close();
//                }
//                return null;
//            } catch(Exception e) {
//                e.printStackTrace();
//                Log.i("Ayush", "Retrying download");
//                download.close();
//                if (bufferedInputStream != null) {
//                    bufferedInputStream.close();
//                }
//                try {
//                    Thread.sleep(5000L);
//                } catch (InterruptedException e1) {
//                    e1.printStackTrace();
//                    return downloadFile(fileName);
//                }
//                return downloadFile(fileName);
//            } finally {
//                download.close();
//                if (bufferedInputStream != null) {
//                    bufferedInputStream.close();
//                }
//            }
//            Log.i("Ayush", "downloaded " + fileName + " " + download.size());
//            return download.toByteArray();
//        } catch (Exception e) {
//            e.printStackTrace();
//            Log.i("Ayush", "Retrying download");
//            return downloadFile(fileName);
//        }
//    }

//    public static void deleteFile(String fileName) {
//
//        getStorage();
//        try {
//            storage.objects().delete(BUCKET_NAME, fileName).execute();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    public static void deleteAllFiles() {
//
//        getStorage();
//        List<String> stringList = listBucket(BUCKET_NAME);
//        for (String a : stringList) deleteFile(a);
//    }

//    public void createBucket(String bucketName) {
//
//        getStorage();
//        Bucket bucket = new Bucket();
//        bucket.setName(bucketName);
//        try {
//            storage.buckets().insert(PROJECT_ID_PROPERTY, bucket).execute();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    public void deleteBucket(String bucketName) {
//
//        getStorage();
//        try {
//            storage.buckets().delete(bucketName).execute();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    public static List<String> listBucket(String bucketName) {
//
//        final Storage storage = getStorage();
//        List<String> list = new ArrayList<>();
//        List<StorageObject> objects = null;
//        try {
//            objects = storage.objects().list(bucketName).execute().getItems();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        if (objects != null) {
//            for (StorageObject o : objects) {
//                list.add(o.getName());
//            }
//        }
//        return list;
//    }

//    public List<String> listBuckets() {
//
//        getStorage();
//        List<String> list = new ArrayList<>();
//        List<Bucket> buckets = null;
//        try {
//            buckets = storage.buckets().list(PROJECT_ID_PROPERTY).execute().getItems() ;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        if(buckets != null) {
//            for(Bucket b : buckets) {
//                list.add(b.getName());
//            }
//        }
//        return list;
//    }

    /**
     * Uploads the music data to google cloud storage
     *
     * @param musicData bytes of music data
     * @param fileName  the name of file (@MiscUtils.getMusicStorageKey())
     * @param key       the cloud storage key as input stream
     */
    public static boolean uploadMusicData(byte[] musicData, final String fileName, InputStream key) {

        //prepare storage object
        final Storage storage = getStorage(key).orNull();
        MiscUtils.closeAndIgnore(key);
        if (storage == null)
            return true;

        //compute hash of current music data
        final String currentHash = Base64.encodeToString(Hashing.md5().newHasher()
                .putBytes(musicData)
                .hash().asBytes(), Base64.DEFAULT).trim();
        Log.i("Ayush", "Current hash = " + currentHash);

        //getMd5Hash of music data on storage
        String hash = "";
        try {
            hash = storage.objects().get(BUCKET_NAME_MUSIC_DATA, fileName).execute().getMd5Hash().trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i("Ayush", "Server hash = " + hash);

        //compare hash
        if (currentHash.equals(hash))
            return false; //hash was same

        Log.i("Ayush", currentHash.compareTo(hash) + " ");
        //file not present OR old
        Log.i("Ayush", "File not found, Uploading " + fileName);
        final ByteArrayInputStream stream = new ByteArrayInputStream(musicData);
        final InputStreamContent content;
        content = new InputStreamContent("application/octet-stream", stream);

        MiscUtils.autoRetry(
                new DoWork<Void>() {
                    @Override
                    protected Void doWork() throws IOException {

                        final String uploadedHash = storage.objects().insert(BUCKET_NAME_MUSIC_DATA, null, content)
                                .setName(fileName)
                                .execute().getMd5Hash();

                        MiscUtils.closeAndIgnore(stream);
                        Log.i("Ayush", "Upload complete " + uploadedHash);
                        return null;
                    }
                }, Optional.<Predicate<Void>>absent());

        return true;
    }

    private static Optional<Storage> getStorage(InputStream stream) {

        final HttpTransport transport = new NetHttpTransport();
        final JsonFactory factory = new JacksonFactory();
        final HttpRequestInitializer initializer = new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {
            }
        };

        final PrivateKey key;

        try {
            key = SecurityUtils.loadPrivateKeyFromKeyStore(
                    SecurityUtils.getPkcs12KeyStore(),
                    stream,
                    "notasecret", "privatekey", "notasecret");
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            try {
                transport.shutdown();
            } catch (IOException ignored) {
            }
            MiscUtils.closeAndIgnore(stream);
            return Optional.absent();
        }

        final GoogleCredential googleCredential = new GoogleCredential.Builder()
                .setTransport(transport)
                .setJsonFactory(factory)
                .setServiceAccountId(ACCOUNT_ID_PROPERTY)
                .setServiceAccountPrivateKey(key)
                .setServiceAccountScopes(Collections.singletonList(StorageScopes.DEVSTORAGE_FULL_CONTROL))
                .setRequestInitializer(initializer).build();

        return Optional.of(new Storage.Builder(transport, factory, googleCredential)
                .setApplicationName(APPLICATION_NAME_PROPERTY)
                .build());
    }
}