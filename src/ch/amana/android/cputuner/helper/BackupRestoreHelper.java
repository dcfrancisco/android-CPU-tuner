package ch.amana.android.cputuner.helper;

import java.io.File;
import java.io.InputStream;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.ContentResolver;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import ch.almana.android.importexportdb.BackupRestoreCallback;
import ch.almana.android.importexportdb.ExportDataTask;
import ch.almana.android.importexportdb.constants.JsonConstants;
import ch.almana.android.importexportdb.importer.DataJsonImporter;
import ch.almana.android.importexportdb.importer.JSONBundle;
import ch.amana.android.cputuner.hw.PowerProfiles;
import ch.amana.android.cputuner.model.ConfigurationAutoloadModel;
import ch.amana.android.cputuner.model.ModelAccess;
import ch.amana.android.cputuner.model.ProfileModel;
import ch.amana.android.cputuner.model.TriggerModel;
import ch.amana.android.cputuner.model.VirtualGovernorModel;
import ch.amana.android.cputuner.provider.CpuTunerProvider;
import ch.amana.android.cputuner.provider.db.DB;
import ch.amana.android.cputuner.provider.db.DB.OpenHelper;

public class BackupRestoreHelper {

	public static final String DIRECTORY_CONFIGURATIONS = "configurations";

	public static final Object MUTEX = new Object();

	private final BackupRestoreCallback cb;

	private final ContentResolver contentResolver;

	public BackupRestoreHelper(BackupRestoreCallback cb) {
		super();
		this.cb = cb;
		this.contentResolver = cb.getContext().getContentResolver();
	}

	private void backup(File storagePath) {
		if (!storagePath.isDirectory()) {
			storagePath.mkdir();
		}
		// ModelAccess.getInstace(cb.getContext()).applyDelayedTriggerUpdates();
		SQLiteDatabase db = new OpenHelper(cb.getContext()).getWritableDatabase();
		ExportDataTask exportDataTask = new ExportDataTask(cb, db, storagePath, ExportDataTask.ExportType.JSON);
		exportDataTask.execute(new String[] { DB.DATABASE_NAME });
	}

	public static File getStoragePath(Context ctx, String directory) {
		return new File(Environment.getExternalStorageDirectory(), ctx.getPackageName() + "/" + directory);
	}

	private void restore(DataJsonImporter dje, boolean inclAutoloadConfig) throws JSONException {
		Context ctx = cb.getContext();
		CpuTunerProvider.deleteAllTables(ctx, inclAutoloadConfig);
		synchronized (ModelAccess.virtgovCacheMutex) {
			synchronized (ModelAccess.profileCacheMutex) {
				synchronized (ModelAccess.triggerCacheMutex) {
					loadVirtualGovernors(dje);
					loadCpuProfiles(dje);
					loadTriggers(dje);
					if (inclAutoloadConfig) {
						loadAutoloadConfig(dje);
					}
				}
			}
		}
		ModelAccess.getInstace(cb.getContext()).clearCache();
	}

	private void loadVirtualGovernors(DataJsonImporter dje) throws JSONException {
		JSONArray table = dje.getTables(DB.VirtualGovernor.TABLE_NAME);
		for (int i = 0; i < table.length(); i++) {
			VirtualGovernorModel vgm = new VirtualGovernorModel();
			vgm.readFromJson(new JSONBundle(table.getJSONObject(i)));
			contentResolver.insert(DB.VirtualGovernor.CONTENT_URI, vgm.getValues());
		}
	}

	private void loadCpuProfiles(DataJsonImporter dje) throws JSONException {
		JSONArray table = dje.getTables(DB.CpuProfile.TABLE_NAME);
		for (int i = 0; i < table.length(); i++) {
			ProfileModel pm = new ProfileModel();
			pm.readFromJson(new JSONBundle(table.getJSONObject(i)));
			contentResolver.insert(DB.CpuProfile.CONTENT_URI, pm.getValues());
		}
	}

	private void loadTriggers(DataJsonImporter dje) throws JSONException {
		JSONArray table = dje.getTables(DB.Trigger.TABLE_NAME);
		for (int i = 0; i < table.length(); i++) {
			TriggerModel tr = new TriggerModel();
			tr.readFromJson(new JSONBundle(table.getJSONObject(i)));
			contentResolver.insert(DB.Trigger.CONTENT_URI, tr.getValues());
		}
	}

	private void loadAutoloadConfig(DataJsonImporter dje) throws JSONException {
		JSONArray table = dje.getTables(DB.ConfigurationAutoload.TABLE_NAME);
		for (int i = 0; i < table.length(); i++) {
			ConfigurationAutoloadModel cam = new ConfigurationAutoloadModel();
			cam.readFromJson(new JSONBundle(table.getJSONObject(i)));
			contentResolver.insert(DB.ConfigurationAutoload.CONTENT_URI, cam.getValues());
		}
	}

	public void backupConfiguration(String name) {
		synchronized (MUTEX) {
			if (name == null) {
				return;
			}
			File storagePath = new File(getStoragePath(cb.getContext(), DIRECTORY_CONFIGURATIONS), name);
			if (!storagePath.isDirectory()) {
				storagePath.mkdirs();
			}
			backup(storagePath);
		}
	}

	public void restoreConfiguration(String name, boolean isUserConfig, boolean restoreAutoload) throws Exception {
		synchronized (MUTEX) {
			if (name == null) {
				return;
			}
			try {
				if (isUserConfig) {
					File file = new File(getStoragePath(cb.getContext(), DIRECTORY_CONFIGURATIONS), name);
					DataJsonImporter dje = new DataJsonImporter(DB.DATABASE_NAME, file);
					restore(dje, restoreAutoload);
				} else {
					String fileName = DIRECTORY_CONFIGURATIONS + "/" + name + "/" + DB.DATABASE_NAME + JsonConstants.FILE_NAME;
					InputStream is = cb.getContext().getAssets().open(fileName);
					//	getconfig 
					DataJsonImporter dje = new DataJsonImporter(DB.DATABASE_NAME, is);
					restore(dje, restoreAutoload);
					//	FIXME		fix frequencies
					//	fix governors
					fixGovernors();
				}
				PowerProfiles.getInstance().reapplyProfile(true);
				cb.hasFinished(true);
			} catch (Exception e) {
				Logger.e("Cannot restore configuration", e);
				cb.hasFinished(false);
				throw new Exception("Cannot restore configuration", e);
			}
		}
	}

	private void fixGovernors() {
		// FIXME
		//		Cursor c = null;
		//		String[] availCpuGov = CpuHandler.getInstance().getAvailCpuGov();
		//		try {
		//			c = contentResolver.query(DB.VirtualGovernor.CONTENT_URI, DB.VirtualGovernor.PROJECTION_DEFAULT, null, null, null);
		//			while (c.moveToNext()) {
		//				String govs = c.getString(DB.VirtualGovernor.INDEX_REAL_GOVERNOR);
		//				String[] govitems = govs.split("|");
		//				for (String gov : govitems) {
		//					if (Arrays.binarySearch(availCpuGov, 0, availCpuGov.length, gov) > -1) {
		//						Logger.i("Using " + gov);
		//					}
		//				}
		//
		//			}
		//		} finally {
		//			if (c != null) {
		//				c.close();
		//				c = null;
		//			}
		//		}

	}

}
