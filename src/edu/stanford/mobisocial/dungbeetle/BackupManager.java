package edu.stanford.mobisocial.dungbeetle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import android.os.Environment;

/**
 * A class to handle backups of personal data. Backups can be sent to
 * the local device, a nearby device, or a cloud service.
 *
 */
public class BackupManager {

	static class LocalBackupService implements BackupService {
		@Override
		public void load() {
			
		}

		@Override
		public void store() {
			
		}
	}
	
	interface BackupService {
		public void load();
		public void store();
	}
}
