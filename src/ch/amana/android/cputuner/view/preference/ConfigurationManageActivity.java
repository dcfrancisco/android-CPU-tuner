package ch.amana.android.cputuner.view.preference;

import java.io.File;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import ch.amana.android.cputuner.R;
import ch.amana.android.cputuner.helper.BackupRestoreHelper;

public class ConfigurationManageActivity extends ListActivity implements OnItemClickListener {

	public static final String DIRECTORY = "configurations";
	private ListView lvConfiguration;
	private ConfigurationsAdapter configurationsAdapter;
	private int configId;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.titleManageConfigurations);
		setContentView(R.layout.configuration_manage);

		lvConfiguration = getListView();
		configurationsAdapter = new ConfigurationsAdapter(this);
		lvConfiguration.setAdapter(configurationsAdapter);
		lvConfiguration.setOnCreateContextMenuListener(this);
		lvConfiguration.setOnItemClickListener(this);

		Button buAddConfiguration = (Button) findViewById(R.id.buAddConfiguration);
		buAddConfiguration.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				saveConfig("current");
			}

		});
	}

	private void saveConfig(String name) {
		BackupRestoreHelper.backup(ConfigurationManageActivity.this, new File(DIRECTORY, name));
	}

	private void loadConfig(String name) {
		BackupRestoreHelper.restore(ConfigurationManageActivity.this, new File(DIRECTORY, name));
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		configId = position;
		lvConfiguration.showContextMenu();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (menuInfo != null) {
			configId = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
		}
		getMenuInflater().inflate(R.menu.configuration_context, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		File file = configurationsAdapter.getDirectory(configId);
		switch (item.getItemId()) {
		case R.id.itemReplace:
			replace(file);
			return true;
		case R.id.itemLoad:
			load(file);
			return true;
		case R.id.itemDelete:
			delete(file);
			return true;

		}

		return super.onContextItemSelected(item);
	}

	private void replace(final File configuration) {
		Builder alertBuilder = new AlertDialog.Builder(this);
		alertBuilder.setTitle(R.string.menuReplaceWithCurrent);
		alertBuilder.setMessage(getString(R.string.msg_replace_with_current, configuration.getName()));
		alertBuilder.setNegativeButton(R.string.no, null);
		alertBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				saveConfig(configuration.getName());
				updateListView();
			}

		});
		AlertDialog alert = alertBuilder.create();
		alert.show();
	}

	private void load(final File configuration) {
		Builder alertBuilder = new AlertDialog.Builder(this);
		alertBuilder.setTitle(R.string.menuLoad);
		alertBuilder.setMessage(getString(R.string.msg_load_configuration, configuration.getName()));
		alertBuilder.setNegativeButton(R.string.no, null);
		alertBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				loadConfig(configuration.getName());
				updateListView();
			}

		});
		AlertDialog alert = alertBuilder.create();
		alert.show();
	}

	private void delete(final File configuration) {
		Builder alertBuilder = new AlertDialog.Builder(this);
		alertBuilder.setTitle(R.string.menuItemDelete);
		alertBuilder.setMessage(getString(R.string.msg_delete_configuration, configuration.getName()));
		alertBuilder.setNegativeButton(R.string.no, null);
		alertBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				File[] files = configuration.listFiles();
				for (int i = 0; i < files.length; i++) {
					files[i].delete();
				}
				configuration.delete();
				updateListView();
			}

		});
		AlertDialog alert = alertBuilder.create();
		alert.show();
	}

	private void updateListView() {
		configurationsAdapter.notifyDataSetChanged();
	}
}
