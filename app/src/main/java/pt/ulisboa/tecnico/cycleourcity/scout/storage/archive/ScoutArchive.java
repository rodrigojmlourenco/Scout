package pt.ulisboa.tecnico.cycleourcity.scout.storage.archive;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.IOException;

import edu.mit.media.funf.storage.BackedUpArchive;
import edu.mit.media.funf.storage.CompositeFileArchive;
import edu.mit.media.funf.storage.DirectoryCleaner;
import edu.mit.media.funf.storage.FileArchive;
import edu.mit.media.funf.storage.FileCopier;
import edu.mit.media.funf.storage.FileDirectoryArchive;
import edu.mit.media.funf.util.NameGenerator;
import pt.ulisboa.tecnico.cycleourcity.scout.logging.ScoutLogger;

/**
 *
 */
public class ScoutArchive implements FileArchive {

    private final static String LOG_TAG = "ScoutArchive";

    private Context context;

    private String name = "scout";
    private String archiveTag = "ScoutArchive";
    private final String suffix = ".db";

    private final ScoutLogger logger = ScoutLogger.getInstance();

    public ScoutArchive(Context context, String name){
        this.context = context;
        this.name = name;
    }

    private FileDirectoryArchive getDbFileArchive(File archiveDir, Context context) {
        NameGenerator nameGenerator =
                new NameGenerator.CompositeNameGenerator(
                        new NameGenerator.TimestampNameGenerator(),
                        new NameGenerator.ConstantNameGenerator(archiveTag, suffix)
                );

        logger.log(ScoutLogger.VERBOSE, LOG_TAG, "getDBFileArchive[a]"+nameGenerator.generateName(""));
        logger.log(ScoutLogger.VERBOSE, LOG_TAG, "getDBFileArchive[b]"+archiveDir.getAbsolutePath());

        FileCopier copier = new FileCopier.SimpleFileCopier();
        return new FileDirectoryArchive(archiveDir, nameGenerator, copier, new DirectoryCleaner.KeepAll());
    }

    private FileArchive delegateArchive; //Cache
    private FileArchive getDelegateArchive(){
        if(delegateArchive==null)
            synchronized (this){
                if(delegateArchive==null){

                    String archiveDirPath = getScoutDefaultArchive().getAbsolutePath();
                    FileDirectoryArchive mainFileDir = getDbFileArchive(new File(archiveDirPath + "/Archive"), context);

                    FileArchive backupArchive =
                            FileDirectoryArchive.getRollingFileArchive(new File(archiveDirPath+"/Backup"));
                    FileArchive mainArchive =
                            new CompositeFileArchive(mainFileDir);

                    delegateArchive = new BackedUpArchive(mainArchive, backupArchive);
                    return delegateArchive;
                }
            }

        return null;
    }

    private File getScoutDefaultArchive(){

        File dir = new File(Environment.getExternalStorageDirectory(), name);

        //Debugging
        if (!dir.mkdirs()) {
            logger.log(ScoutLogger.INFO, LOG_TAG, "Directory '"+dir.getAbsolutePath()+"'not created");
        }else
            logger.log(ScoutLogger.INFO, LOG_TAG, "Directory '"+dir.getAbsolutePath()+"' created");

        return dir;
    }

    @Override
    public boolean add(File file) {
        return getDelegateArchive().add(file);
    }

    public boolean add(File file, String tag){
        setArchiveTag(tag);
        return add(file);
    }

    @Override
    public boolean remove(File file) {
        return getDelegateArchive().remove(file);
    }

    @Override
    public boolean contains(File file) {
        return getDelegateArchive().contains(file);
    }

    @Override
    public File[] getAll() {
        return getDelegateArchive().getAll();
    }

    public String getArchiveTag() {
        return archiveTag;
    }

    public void setArchiveTag(String archiveTag) {
        this.archiveTag = archiveTag;
    }

    public String getPathOnSDCard() throws IOException {
        return getScoutDefaultArchive().getCanonicalPath();
    }
}
